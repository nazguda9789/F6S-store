package com.web.doss;


import com.web.utils.TypeSystem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    // ============ cấu hình (tùy chỉnh) ============
    private static final long WINDOW_MS = 60_000L;              // cửa sổ tính (60s)
    private static final int LIMIT = 100;                      // max request trong cửa sổ
    private static final long BLOCK_MS = 1 * 60_000L;         // block tạm (1 phút)
    private static final long CLEANUP_INTERVAL_MS = 5 * 60_000L; // quét cleanup mỗi 5 phút
    private static final long ENTRY_TTL_MS = 15 * 60_000L;     // xóa entry nếu inactive lâu hơn 15 phút

    // các prefix / path tĩnh cần bỏ qua (không tính vào limit)
    private static final List<String> EXCLUDE_PREFIXES = Arrays.asList(
            "/css/", "/js/", "/images/", "/img/", "/lib/", "/static/", "/webjars/", "/favicon.ico", "/logo", "/image/"
    );

    // ============ trạng thái in-memory ============
    private final Map<String, RequestInfo> ipMap = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(0L);

    // ============ doFilterInternal ============
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
//        System.out.println("uri: === "+uri);
        String accept = request.getHeader("Accept");

        // Nếu URI nằm trong danh sách loại trừ (tài nguyên tĩnh) -> bỏ qua rate limiting
        if (isExcluded(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Nếu là trang admin thì bỏ qua rate limit
        if (uri.startsWith(TypeSystem.ADMIN_URL)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Xác định đây có phải "page load" (HTML) hay "api request" mà ta muốn tính hay không
        boolean isApiRequest = uri.startsWith("/api");
        boolean isHtmlRequest = accept != null && accept.contains("text/html") && !uri.contains(".");

        // Nếu không phải loại cần tính (ví dụ tài nguyên tĩnh khác), chuyển tiếp
        if (!isApiRequest && !isHtmlRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        // Lấy IP client — ưu tiên header nếu deploy sau reverse-proxy
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        // cleanup map thỉnh thoảng (tránh làm mỗi request tốn quá nhiều)
        long last = lastCleanup.get();
        if (last + CLEANUP_INTERVAL_MS < now) {
            if (lastCleanup.compareAndSet(last, now)) {
                cleanupOldEntries(now);
            }
        }

        // lấy hoặc tạo RequestInfo cho IP này
        RequestInfo info = ipMap.computeIfAbsent(ip, k -> new RequestInfo(now));

        synchronized (info) {
            info.lastSeen = now;

            // nếu IP đang bị block tạm thời -> trả 429
            if (info.blockedUntil > now) {
                long retryAfterSeconds = (info.blockedUntil - now + 999) / 1000;
                sendTooMany(response, retryAfterSeconds);
                return;
            }

            // nếu cửa sổ đã hết, reset
            if (now - info.windowStart > WINDOW_MS) {
                info.windowStart = now;
                info.count = 0;
            }

            // tăng count (1 lần cho mỗi request loại page/API)
            info.count++;

            // nếu vượt limit -> block tạm thời và trả 429
            if (info.count > LIMIT) {
                info.blockedUntil = now + BLOCK_MS;
                long retryAfterSeconds = (info.blockedUntil - now + 999) / 1000;
                sendTooMany(response, retryAfterSeconds);
                return;
            }
        }

        // nếu chưa bị block -> tiếp tục chuỗi filter
        filterChain.doFilter(request, response);
    }

    // ============ helper methods ============

    private boolean isExcluded(String uri) {
        if (uri == null) return true;
        for (String p : EXCLUDE_PREFIXES) {
            if (uri.startsWith(p) || uri.contains(p)) return true;
        }
        // loại trừ các file có extension rõ rệt (ví dụ .css .js .png .jpg .ico)
        int lastDot = uri.lastIndexOf('.');
        if (lastDot > 0 && lastDot > uri.lastIndexOf('/')) {
            String ext = uri.substring(lastDot + 1).toLowerCase();
            List<String> extExclude = Arrays.asList("css", "js", "png", "jpg", "jpeg", "gif", "ico", "svg", "map", "woff", "woff2", "ttf");
            if (extExclude.contains(ext)) return true;
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            // X-Forwarded-For có thể chứa danh sách IP, IP đầu tiên là client gốc
            return xfHeader.split(",")[0].trim();
        }
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) {
            return xr.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooMany(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"error\":\"Bạn đã gửi quá nhiều yêu cầu trong 1 phút\",\"retry_after\":%s}", (String.valueOf((retryAfterSeconds / 1000)) + " phút"));
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    // xóa các entry quá cũ (inactive lâu) để tránh memory leak in-memory
    private void cleanupOldEntries(long now) {
        for (Map.Entry<String, RequestInfo> e : ipMap.entrySet()) {
            RequestInfo v = e.getValue();
            if (v == null) continue;
            // xóa khi đã inactive lâu hơn ENTRY_TTL_MS và không đang bị block
            if (now - v.lastSeen > ENTRY_TTL_MS && v.blockedUntil < now) {
                ipMap.remove(e.getKey(), v);
            }
        }
    }

    // ============ inner class lưu trạng thái per IP ============
    private static class RequestInfo {
        int count;
        long windowStart;
        long blockedUntil;
        long lastSeen;

        RequestInfo(long now) {
            this.count = 0;
            this.windowStart = now;
            this.blockedUntil = 0L;
            this.lastSeen = now;
        }
    }
}


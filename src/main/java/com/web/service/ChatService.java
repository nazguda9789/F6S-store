package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.web.entity.*;
import com.web.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final VoucherRepository voucherRepository;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final ObjectMapper         objectMapper = new ObjectMapper();

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    @Value("${groq.api.key}")
    private String groqApiKey;
    /*
    *
    *   llama-3.3-70b-versatile	Mạnh nhất, khuyên dùng
        llama-3.1-8b-instant	Nhanh, nhẹ, tốn ít token
        mixtral-8x7b-32768	Context 32K, tốt cho hội thoại dài
        gemma2-9b-it	Model của Google nhưng chạy trên Groq
        deepseek-r1-distill-llama-70b	Tốt cho lý luận logic
    * */
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";
    @Autowired
    public ChatService(
            ProductRepository productRepository,
            ProductDetailRepository productDetailRepository,
            InvoiceRepository invoiceRepository,
            InvoiceDetailRepository invoiceDetailRepository,
            CategoryRepository categoryRepository,
            ProductCategoryRepository productCategoryRepository,
            ColorRepository colorRepository,
            SizeRepository sizeRepository,
            VoucherRepository voucherRepository) {
        this.productRepository = productRepository;
        this.productDetailRepository = productDetailRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceDetailRepository = invoiceDetailRepository;
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
        this.voucherRepository = voucherRepository;
    }

    /**
     * Chat với Gemini cho website bán giày thể thao
     */
    public String chatWithGemini(String userMessage, jakarta.servlet.http.HttpSession session) {
        manageChatHistory(session, userMessage);

        try {
            // Trích xuất thông tin từ tin nhắn
            Map<String, Object> extractedInfo = extractInfoFromMessage(userMessage);

            // Lấy dữ liệu tối ưu dựa trên câu hỏi
            String contextData = getOptimizedContextData(extractedInfo);

            // Xây dựng prompt tối ưu
            String prompt = buildOptimizedPrompt(session, contextData, userMessage, extractedInfo);

            // Gửi request Gemini
            String response = sendToGroq(prompt);

            // Lưu phản hồi
            saveResponseToHistory(session, response);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Xin lỗi, hệ thống đang gặp sự cố. Vui lòng thử lại sau!";
        }
    }

    /**
     * Quản lý lịch sử chat - chỉ lưu 5 tin nhắn gần nhất
     */
    private void manageChatHistory(jakarta.servlet.http.HttpSession session, String userMessage) {
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("chat-history");

        if (history == null) {
            history = new ArrayList<>();
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "u");
        userMsg.put("c", userMessage.length() > 100 ? userMessage.substring(0, 100) : userMessage);
        history.add(userMsg);

        // Chỉ giữ 10 tin nhắn gần nhất
        if (history.size() > 10) {
            history = history.subList(history.size() - 10, history.size());
        }

        session.setAttribute("chat-history", history);
    }

    private void saveResponseToHistory(HttpSession session, String response) {
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("chat-history");
        if (history != null) {
            Map<String, String> botMsg = new HashMap<>();
            botMsg.put("role", "a");
            botMsg.put("c", response.length() > 200 ? response.substring(0, 200) : response);
            history.add(botMsg);
            session.setAttribute("chat-history", history);
        }
    }

    /**
     * Trích xuất thông tin từ tin nhắn
     */
    private Map<String, Object> extractInfoFromMessage(String message) {
        Map<String, Object> info = new HashMap<>();
        String msg = message.toLowerCase();

        // Khoảng giá
        Pattern pricePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(triệu|tr|k|ngàn)");
        Matcher priceMatcher = pricePattern.matcher(msg);
        if (priceMatcher.find()) {
            double value = Double.parseDouble(priceMatcher.group(1));
            String unit = priceMatcher.group(2);
            if (unit.equals("triệu") || unit.equals("tr")) {
                info.put("price", (long) (value * 1000000));
            } else if (unit.equals("k") || unit.equals("ngàn")) {
                info.put("price", (long) (value * 1000));
            }
        }

        // Màu sắc
        String[] colors = {"đen", "trắng", "đỏ", "xanh", "vàng", "hồng", "tím", "xám", "nâu"};
        for (String color : colors) {
            if (msg.contains(color)) {
                info.put("color", color);
                break;
            }
        }

        // Size
        Pattern sizePattern = Pattern.compile("size\\s*(3[5-9]|4[0-5]|\\d{2})");
        Matcher sizeMatcher = sizePattern.matcher(msg);
        if (sizeMatcher.find()) {
            info.put("size", sizeMatcher.group(1));
        }

        // Thương hiệu (Category)
        List<Category> brands = categoryRepository.findByIsParentTrue();
        for (Category brand : brands) {
            if (msg.contains(brand.getName().toLowerCase())) {
                info.put("brand", brand.getName());
                info.put("brandId", brand.getId());
                break;
            }
        }

        // Danh mục con (loại giày)
        List<Category> subCategories = categoryRepository.findByIsParentFalse();
        for (Category cat : subCategories) {
            if (msg.contains(cat.getName().toLowerCase())) {
                info.put("category", cat.getName());
                info.put("categoryId", cat.getId());
                break;
            }
        }

        // Loại câu hỏi
        if (msg.contains("bán chạy") || msg.contains("top") || msg.contains("best") || msg.contains("hot")) {
            info.put("type", "hot");
        } else if (msg.contains("còn hàng") || msg.contains("stock") || msg.contains("tồn")) {
            info.put("type", "stock");
        } else if (msg.contains("đặt") || msg.contains("mua") || msg.contains("order")) {
            info.put("type", "order");
        } else if (msg.contains("giá") || msg.contains("price")) {
            info.put("type", "price");
        } else if (msg.contains("size") || msg.contains("cỡ")) {
            info.put("type", "size");
        } else if (msg.contains("voucher") || msg.contains("giảm giá") || msg.contains("khuyến mãi")) {
            info.put("type", "voucher");
        } else {
            info.put("type", "general");
        }

        return info;
    }

    /**
     * Lấy dữ liệu ngữ cảnh tối ưu (chỉ lấy thông tin cần thiết)
     */
    private String getOptimizedContextData(Map<String, Object> info) {
        String type = (String) info.getOrDefault("type", "general");

        switch (type) {
            case "hot":
                return getHotProducts();
            case "stock":
                return getStockInfo(info);
            case "price":
                return getPriceInfo(info);
            case "size":
                return getSizeGuide();
            case "voucher":
                return getVoucherInfo();
            default:
                return getGeneralProducts(info);
        }
    }

    /**
     * Lấy sản phẩm bán chạy - chỉ 5 sản phẩm
     */
    private String getHotProducts() {
        List<Product> topProducts = productRepository.findTop5ByOrderBySoldDesc();
        if (topProducts.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("🔥TOP5:\n");
        for (Product p : topProducts) {
            // Lấy brand của sản phẩm
            String brand = getProductBrand(p.getId());
            sb.append(String.format("%s %s|%,.0fk|đã bán:%d\n",
                    brand.isEmpty() ? "" : brand,
                    p.getName(),
                    p.getPrice() / 1000,
                    p.getSold()));
        }
        return sb.toString();
    }

    /**
     * Lấy thông tin tồn kho - chỉ 5 sản phẩm sắp hết
     */
    private String getStockInfo(Map<String, Object> info) {
        List<ProductDetail> lowStock = productDetailRepository.findTop5ByStockLessThanOrderByStockAsc(20);
        if (lowStock.isEmpty()) return "Đủ hàng";

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️HẾT HÀNG:\n");
        for (ProductDetail pd : lowStock) {
            productRepository.findById(pd.getProductId()).ifPresent(p -> {
                String brand = getProductBrand(p.getId());
                sb.append(String.format("%s%s %s-%s:còn%d\n",
                        brand.isEmpty() ? "" : brand,
                        p.getName(),
                        pd.getColor() != null ? pd.getColor().getName() : "",
                        pd.getSize() != null ? pd.getSize().getName() : "",
                        pd.getStock()));
            });
            if (sb.length() > 300) break;
        }
        return sb.toString();
    }

    /**
     * Lấy thông tin giá - top 10 sản phẩm
     */
    private String getPriceInfo(Map<String, Object> info) {
        List<Product> products = new ArrayList<>();

        // Lọc theo brand
        if (info.containsKey("brandId")) {
            Long brandId = (Long) info.get("brandId");
            List<Long> productIds = productCategoryRepository.findProductIdsByCategoryId(brandId);
            if (!productIds.isEmpty()) {
                products = productRepository.findByIdIn(productIds);
                products.sort((a, b) -> b.getSold().compareTo(a.getSold()));
                if (products.size() > 10) products = products.subList(0, 10);
            }
        }
        // Lọc theo giá
        else if (info.containsKey("price")) {
            Long maxPrice = (Long) info.get("price");
            products = productRepository.findTop10ByPriceLessThanEqualOrderByPriceDesc(Double.valueOf(maxPrice));
        }
        // Mặc định lấy top bán chạy
        else {
            products = productRepository.findTop10ByOrderBySoldDesc();
        }

        if (products.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("💰GIÁ:\n");
        for (Product p : products) {
            String brand = getProductBrand(p.getId());
            sb.append(String.format("%s%s|%,.0fk\n",
                    brand.isEmpty() ? "" : brand,
                    p.getName(),
                    p.getPrice() / 1000));
        }
        return sb.toString();
    }

    /**
     * Sản phẩm chung - chỉ 10 sản phẩm
     */
    private String getGeneralProducts(Map<String, Object> info) {
        List<Product> products = new ArrayList<>();

        // Lọc theo category
        if (info.containsKey("categoryId")) {
            Long categoryId = (Long) info.get("categoryId");
            List<Long> productIds = productCategoryRepository.findProductIdsByCategoryId(categoryId);
            if (!productIds.isEmpty()) {
                products = productRepository.findByIdIn(productIds);
                products.sort((a, b) -> b.getSold().compareTo(a.getSold()));
                if (products.size() > 10) products = products.subList(0, 10);
            }
        }
        // Lọc theo brand
        else if (info.containsKey("brandId")) {
            Long brandId = (Long) info.get("brandId");
            List<Long> productIds = productCategoryRepository.findProductIdsByCategoryId(brandId);
            if (!productIds.isEmpty()) {
                products = productRepository.findByIdIn(productIds);
                products.sort((a, b) -> b.getSold().compareTo(a.getSold()));
                if (products.size() > 10) products = products.subList(0, 10);
            }
        }
        // Mặc định lấy top bán chạy
        else {
            products = productRepository.findTop10ByOrderBySoldDesc();
        }

        if (products.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("👟SẢN PHẨM:\n");
        for (Product p : products) {
            String brand = getProductBrand(p.getId());
            sb.append(String.format("%s%s|%,.0fk\n",
                    brand.isEmpty() ? "" : brand,
                    p.getName(),
                    p.getPrice() / 1000));
        }
        return sb.toString();
    }

    /**
     * Lấy brand của sản phẩm từ category cha
     */
    private String getProductBrand(Long productId) {
        List<ProductCategory> productCategories = productCategoryRepository.findByProductId(productId);
        for (ProductCategory pc : productCategories) {
            Category cat = pc.getCategory();
            // Nếu category là con, lấy category cha
            if (cat.getCategory() != null) {
                Category parent = cat.getCategory();
                if (parent.getIsParent()) {
                    return parent.getName();
                }
            }
            // Nếu category là cha
            else if (cat.getIsParent()) {
                return cat.getName();
            }
        }
        return "";
    }

    /**
     * Hướng dẫn chọn size
     */
    private String getSizeGuide() {
        return "📏SIZE: đo cm chân: 22cm(size35), 23cm(36), 24cm(37), 25cm(38), 26cm(39-40), 27cm(41-42), 28cm(43-44)";
    }

    /**
     * Thông tin voucher
     */
    private String getVoucherInfo() {
        List<Voucher> vouchers = voucherRepository.findTop5ByBlockFalseOrderByCreatedDateDesc();
        if (vouchers.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("🎫VOUCHER:\n");
        for (Voucher v : vouchers) {
            sb.append(String.format("%s:giảm%d%%|tối thiểu%,.0fk\n",
                    v.getCode(),
                    v.getDiscount(),
                    v.getMinAmount() / 1000.0));
        }
        return sb.toString();
    }

    /**
     * Xây dựng prompt tối ưu
     */
    private String buildOptimizedPrompt(HttpSession session, String contextData,
                                        String userMessage, Map<String, Object> info) {
        List<Map<String, String>> history = (List<Map<String, String>>) session.getAttribute("chat-history");

        String historyText = "";
        if (history != null && !history.isEmpty()) {
            historyText = "📜HISTORY:\n" + history.stream()
                    .map(m -> (m.get("role").equals("u") ? "👤" : "🤖") + m.get("c"))
                    .collect(Collectors.joining("\n")) + "\n";
        }

        String type = (String) info.getOrDefault("type", "general");
        String extraInfo = "";
        if (info.containsKey("color")) extraInfo += " màu:" + info.get("color");
        if (info.containsKey("size")) extraInfo += " size:" + info.get("size");
        if (info.containsKey("brand")) extraInfo += " hãng:" + info.get("brand");
        if (info.containsKey("category")) extraInfo += " loại:" + info.get("category");
        if (info.containsKey("price")) extraInfo += " giá<" + info.get("price");

        String systemPrompt = "Bạn là AI bán giày thể thao. Trả lời TIẾNG VIỆT, NGẮN GỌN, THÂN THIỆN. Chính sách: đổi trả 7 ngày, freeship >500k.";

        String instruction = "";
        switch (type) {
            case "hot": instruction = "Gợi ý sản phẩm hot nhất"; break;
            case "stock": instruction = "Kiểm tra tồn kho"; break;
            case "order": instruction = "Hướng dẫn đặt hàng, hỏi size,màu,số lượng"; break;
            case "size": instruction = "Tư vấn size giày"; break;
            case "voucher": instruction = "Giới thiệu voucher"; break;
            default: instruction = "Tư vấn sản phẩm phù hợp";
        }

        return String.format("""
            %s
            %s
            %s
            📌%s%s
            💬%s
            🎯%s
            """,
                systemPrompt,
                historyText,
                contextData,
                instruction,
                extraInfo,
                userMessage,
                instruction
        );
    }

    /**
     * Gửi request Gemini với token tối ưu
     */
    private String sendToGemini(String prompt) throws Exception {
        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject userMsg = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject partText = new JsonObject();
        partText.addProperty("text", prompt);
        parts.add(partText);

        userMsg.add("parts", parts);
        contents.add(userMsg);
        root.add("contents", contents);

        // Cấu hình giảm token
        JsonObject config = new JsonObject();
        config.addProperty("temperature", 0.7);
        config.addProperty("maxOutputTokens", 300);
        config.addProperty("topP", 0.8);
        root.add("generationConfig", config);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (json.has("candidates")) {
            return json.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }

        return "⚠️ Không nhận được phản hồi.";
    }


    private String sendToGroq(String prompt) throws Exception {
        // Tạo JSON body theo format OpenAI chuẩn
        JsonObject root = new JsonObject();

        // Thêm model
        root.addProperty("model", GROQ_MODEL);

        // Thêm messages
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        root.add("messages", messages);

        // Thêm các tham số trực tiếp vào root (KHÔNG dùng object "parameters")
        root.addProperty("temperature", 0.7);
        root.addProperty("max_tokens", 2048);
        root.addProperty("top_p", 0.8);

        // In ra để debug nếu cần (có thể xóa sau)
        System.out.println("Request JSON: " + root.toString());

        // Tạo request với Groq
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + groqApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // In ra response để debug
        System.out.println("Response status: " + response.statusCode());
        System.out.println("Response body: " + response.body());

        // Parse response theo format OpenAI
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        // Kiểm tra lỗi
        if (json.has("error")) {
            JsonObject error = json.getAsJsonObject("error");
            return "⚠️ Lỗi Groq API: " + error.get("message").getAsString();
        }

        // Lấy nội dung từ response
        if (json.has("choices")) {
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                String content = message.get("content").getAsString();
                return content;
            }
        }

        return "⚠️ Không nhận được phản hồi từ Groq.";
    }
    /**
     * Phân tích doanh thu tối ưu
     */
    public String analyzeRevenue(LocalDateTime from, LocalDateTime to) {
        try {
            List<Invoice> invoices = invoiceRepository.findByCreatedDateBetween(from, to);
            if (invoices.isEmpty()) return "Không có dữ liệu";

            double revenue = invoices.stream().mapToDouble(Invoice::getTotalAmount).sum();
            long orders = invoices.size();

            // Top 3 sản phẩm
            Map<String, Integer> productSales = new HashMap<>();
            for (Invoice inv : invoices) {
                invoiceDetailRepository.findByIdInvoice(inv.getId()).forEach(detail ->
                        productSales.merge(detail.getProductName(), detail.getQuantity(), Integer::sum));
            }

            String topProducts = productSales.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(e -> String.format("%s:%d", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", "));

            String prompt = String.format("""
                PTICH: %s-%s
                DT:%,.0fk | ĐH:%d | TB:%,.0fk
                TOP3: %s
                
                YC: nhận xét + đề xuất KD (2-3 dòng)
                """,
                    from.format(DateTimeFormatter.ofPattern("dd/MM")),
                    to.format(DateTimeFormatter.ofPattern("dd/MM")),
                    revenue / 1000,
                    orders,
                    (revenue / orders) / 1000,
                    topProducts
            );

            return sendToGemini(prompt);

        } catch (Exception e) {
            return "❌ Lỗi: " + e.getMessage();
        }
    }
}
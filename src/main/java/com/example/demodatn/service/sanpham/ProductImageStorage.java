package com.example.demodatn.service.sanpham;
import com.example.demodatn.exception.CatalogValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.nio.file.*;
import java.io.*;
import java.util.*;

@Service
public class ProductImageStorage {
    private final Path root;
    private final long maxBytes;
    public ProductImageStorage(@Value("${shop.upload-dir:uploads/products}") String directory,
                               @Value("${shop.upload-max-bytes:5242880}") long maxBytes) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = Math.min(maxBytes, 5L * 1024 * 1024);
    }
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > maxBytes) throw invalid("Ảnh vượt dung lượng cho phép (" + maxBytes / 1024 / 1024 + " MB).");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        String mime = Optional.ofNullable(file.getContentType()).orElse("");
        boolean png = name.endsWith(".png") && mime.equals("image/png");
        boolean jpg = (name.endsWith(".jpg") || name.endsWith(".jpeg")) && mime.equals("image/jpeg");
        boolean webp = name.endsWith(".webp") && mime.equals("image/webp");
        if (!png && !jpg && !webp) throw invalid("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        try (InputStream raw = file.getInputStream(); var input = ImageIO.createImageInputStream(raw)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("Nội dung file không phải ảnh hợp lệ.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(png && format.equals("png")) && !(jpg && (format.equals("jpeg") || format.equals("jpg"))) && !(webp && format.equals("webp")))
                    throw invalid("Định dạng nội dung ảnh không khớp loại file.");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 16_000_000)
                    throw invalid("Ảnh tối đa 16 triệu điểm ảnh.");
                var decoded = reader.read(0);
                Files.createDirectories(root);
                String filename = UUID.randomUUID() + (png ? ".png" : webp ? ".webp" : ".jpg");
                Path target = root.resolve(filename).normalize();
                if (!target.getParent().equals(root)) throw invalid("Đường dẫn ảnh không hợp lệ.");
                try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                    if (webp) { try (InputStream source = file.getInputStream()) { source.transferTo(output); } }
                    else if (!ImageIO.write(decoded, png ? "png" : "jpg", output)) throw new IOException("Image encoding failed");
                } catch (IOException ex) {
                    Files.deleteIfExists(target);
                    throw ex;
                }
                return "/uploads/products/" + filename;
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw invalid("Không thể đọc/lưu ảnh. Vui lòng thử lại với ảnh JPG/PNG/WEBP hợp lệ."); }
    }
    public void removeNewFile(String url) {
        if (url == null || !url.matches("/uploads/products/[a-f0-9-]+\\.(png|jpg|webp)")) return;
        try { Files.deleteIfExists(root.resolve(url.substring(url.lastIndexOf('/')+1))); }
        catch (IOException ignored) { /* Orphaned file can be cleaned separately; never remove existing images. */ }
    }
    private CatalogValidationException invalid(String message) { return new CatalogValidationException("image", message); }
}


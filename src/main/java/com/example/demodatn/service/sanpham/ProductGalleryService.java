package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductImage;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductGalleryService {
    public static final String PLACEHOLDER="/anh/anh-san-pham-thay-the.svg";
    private final ProductImageRepository repository;
    private final ProductImageStorage storage;

    public List<ProductImage> list(Integer id) {
        return repository.findByProduct_IdOrderByPositionAscIdAsc(id);
    }

    /** Gọi trong transaction của AdminCatalogService, đã khóa Product khi sửa. */
    public void save(Product product, AdminProductForm form) {
        List<ProductImage> old = list(product.getId());
        Set<Long> removed = new HashSet<>(form.getRemovedImageIds());
        if (!old.stream().map(ProductImage::getId).toList().containsAll(removed))
            throw invalid("Ảnh cần xóa không thuộc sản phẩm này.");
        LinkedHashMap<String,ProductImage> rows = new LinkedHashMap<>();
        old.stream().filter(i -> !removed.contains(i.getId())).forEach(i -> rows.put("existing:"+i.getId(),i));
        List<MultipartFile> uploads = new ArrayList<>();
        if (form.getImages()!=null) Arrays.stream(form.getImages()).filter(f -> f!=null && !f.isEmpty()).forEach(uploads::add);
        if (form.getImage()!=null && !form.getImage().isEmpty()) uploads.add(form.getImage());
        if (rows.size()+uploads.size()>8) throw invalid("Mỗi sản phẩm có tối đa 8 ảnh.");
        if (Boolean.TRUE.equals(product.getStatus()) && rows.isEmpty() && uploads.isEmpty())
            throw invalid("Bạn cần ít nhất 1 ảnh khi bật bán sản phẩm.");
        List<String> keys = new ArrayList<>(rows.keySet());
        for (int i=0;i<uploads.size();i++) keys.add("new:"+i);
        List<String> order = form.getImageOrder().isEmpty() ? keys : form.getImageOrder();
        if (order.size()!=keys.size() || new HashSet<>(order).size()!=keys.size() || !new HashSet<>(order).equals(new HashSet<>(keys)))
            throw invalid("Thứ tự ảnh không hợp lệ. Bạn hãy tải lại trang.");
        String main = form.getMainImageKey();
        if (main==null || main.isBlank()) main=old.stream().filter(i->i.isMain() && !removed.contains(i.getId())).map(i->"existing:"+i.getId()).findFirst().orElse(keys.isEmpty()?null:order.get(0));
        if (main!=null && !keys.contains(main)) throw invalid("Ảnh chính không nằm trong bộ ảnh hiện tại.");
        for (int i=0;i<uploads.size();i++) {
            String path = storage.store(uploads.get(i));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) storage.removeNewFile(path); }
            });
            ProductImage image=new ProductImage(); image.setProduct(product); image.setPath(path);
            rows.put("new:"+i,image);
        }
        // Xóa cờ chính trước khi gán lại để không vi phạm unique index trong flush.
        old.forEach(i->i.setMain(false));
        repository.saveAllAndFlush(old);
        List<ProductImage> deleted=old.stream().filter(i->removed.contains(i.getId())).toList();
        repository.deleteAll(deleted); repository.flush();
        for (int i=0;i<order.size();i++) { ProductImage image=rows.get(order.get(i)); image.setPosition(i); image.setMain(false); }
        repository.saveAllAndFlush(rows.values());
        if (main!=null) { ProductImage image=rows.get(main); image.setMain(true); product.setImageUrl(image.getPath()); }
        else product.setImageUrl(null);
        repository.flush();
        removeAfterCommit(deleted.stream().map(ProductImage::getPath).toList());
    }

    public void removeForProduct(Integer id) {
        List<ProductImage> rows=list(id);
        repository.deleteAll(rows); repository.flush();
        removeAfterCommit(rows.stream().map(ProductImage::getPath).toList());
    }

    private void removeAfterCommit(List<String> paths) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { paths.stream().filter(path->!repository.existsByPath(path)).forEach(storage::removeNewFile); }
        });
    }
    private CatalogValidationException invalid(String message) { return new CatalogValidationException("image",message); }
}

package com.web.service;

import com.web.entity.ShoeAttribute;
import com.web.repository.ShoeAttributeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoeAttributeService {
    private final ShoeAttributeRepository repository;
    public ShoeAttributeService(ShoeAttributeRepository repository) { this.repository = repository; }

    @Transactional
    public ShoeAttribute save(Long id, ShoeAttribute.Kind kind, String rawName, boolean active) {
        return save(id,kind,rawName,active,null);
    }

    @Transactional
    public ShoeAttribute save(Long id, ShoeAttribute.Kind kind, String rawName, boolean active, String description) {
        if (kind == null) throw new IllegalArgumentException("Vui lòng chọn nhóm thuộc tính.");
        if (description != null && description.strip().length() > 500) throw new IllegalArgumentException("Mô tả tối đa 500 ký tự.");
        String name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Tên phải có từ 1 đến 100 ký tự.");
        ShoeAttribute value = id == null ? new ShoeAttribute() : repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại."));
        if (id != null && value.getKind() != kind) throw new IllegalArgumentException("Không được đổi loại thuộc tính.");
        if (repository.existsByKindAndNameIgnoreCaseAndIdNot(kind, name, id == null ? -1L : id))
            throw new IllegalArgumentException("Tên đã tồn tại trong nhóm này.");
        value.setKind(kind); value.setName(name); value.setActive(active);
        if (description != null) value.setDescription(description.strip());
        value = repository.save(value);
        if (value.getCode() == null && value.getId() != null) {
            value.setCode("F6S-" + switch(kind) { case BRAND -> "TH"; case SOLE -> "DE"; case MATERIAL -> "CL"; } + String.format(java.util.Locale.ROOT,"%06d",value.getId()));
            value = repository.save(value);
        }
        return value;
    }

    public Long validate(Long id, ShoeAttribute.Kind kind, Long previousId) {
        if (id == null) return null;
        ShoeAttribute value = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thuộc tính không tồn tại."));
        if (value.getKind() != kind || (!value.isActive() && !id.equals(previousId)))
            throw new IllegalArgumentException("Thuộc tính không hợp lệ hoặc đã ngừng sử dụng.");
        return id;
    }
}

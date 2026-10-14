package com.web.specification;

import com.web.entity.Invoice;
import com.web.entity.Product;
import com.web.entity.ProductCategory;
import com.web.enums.InvoiceStatus;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSpecification implements Specification<Invoice> {

    private InvoiceStatus invoiceStatus;

    private Date from;

    private Date to;

    private String search;

    @Override
    public Predicate toPredicate(Root<Invoice> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();
        query.distinct(true);

        // Nếu có search, chỉ lọc theo search và bỏ các điều kiện khác
        if (search != null && !search.isEmpty() && !search.isBlank()) {
            String searchPattern = "%" + search.trim() + "%";
            Predicate phonePredicate = cb.equal(root.get("phone"), search.trim());
            Predicate codePredicate = cb.equal(root.get("code"), search.trim());
            Predicate namePredicate = cb.like(cb.lower(root.get("fullName")), searchPattern.toLowerCase());
            Predicate addressPredicate = cb.like(cb.lower(root.get("addressDetail")), searchPattern.toLowerCase());
            return cb.or(phonePredicate, codePredicate, namePredicate, addressPredicate);
        }

        if (invoiceStatus != null) {
            predicate = cb.and(predicate, cb.equal(root.get("invoiceStatus"), invoiceStatus));
        }
        if (from != null && to != null) {
            LocalDateTime fromDateTime = from.toLocalDate().atStartOfDay();
            LocalDateTime toDateTime = to.toLocalDate().atTime(LocalTime.MAX); // 23:59:59.999999999
            predicate = cb.and(predicate,
                    cb.between(root.get("createdDate"), fromDateTime, toDateTime)
            );
        }


        return predicate;
    }
}

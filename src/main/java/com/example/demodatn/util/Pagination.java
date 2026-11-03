package com.example.demodatn.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.function.Function;

@Component
public class Pagination {
    private final int size;
    public Pagination(@Value("${shop.page-size}") int size) {
        if (size < 1) throw new IllegalArgumentException("shop.page-kích cỡ phải lớn hơn 0");
        this.size = size;
    }
    public int getSize() { return size; }
    public int index(Object value) {
        try { return (int)Math.min(Math.max(0,Long.parseLong(String.valueOf(value))),Integer.MAX_VALUE / size); }
        catch (NumberFormatException ex) { return 0; }
    }
    public PageRequest request(Object value, long total, Sort sort) {
        return PageRequest.of((int)Math.min(index(value), Math.max(0,(total-1)/size)),size,sort);
    }
    public <T> Page<T> load(Object value, Sort sort, Function<Pageable,Page<T>> query) {
        Page<T> result=query.apply(PageRequest.of(index(value),size,sort));
        int last=Math.max(0,result.getTotalPages()-1);
        return result.getNumber()>last ? query.apply(PageRequest.of(last,size,sort)) : result;
    }
    public Page<Map<String,Object>> sql(JdbcTemplate jdbc,String select,String count,List<Object> args,Object value) {
        long total=jdbc.queryForObject(count,Long.class,args.toArray());
        var request=request(value,total,Sort.unsorted());
        var parameters=new ArrayList<>(args);parameters.add(request.getOffset());parameters.add(size);
        return new PageImpl<>(jdbc.queryForList(select+" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",parameters.toArray()),request,total);
    }
    // Ngoại lệ lọc giá hiệu lực phía User; Home dùng khối cố định 8 phần tử.
    public <T> Page<T> slice(List<T> values,Object value,int pageSize) {
        int last=Math.max(0,(values.size()-1)/pageSize);
        int current=Math.min(index(value),last),start=current*pageSize;
        return new PageImpl<>(new ArrayList<>(values.subList(start,Math.min(start+pageSize,values.size()))),PageRequest.of(current,pageSize),values.size());
    }
}

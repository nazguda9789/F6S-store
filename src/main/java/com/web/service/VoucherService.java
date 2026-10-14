package com.web.service;

import com.web.entity.User;
import com.web.entity.Voucher;
import com.web.enums.Role;
import com.web.exception.MessageException;
import com.web.repository.InvoiceRepository;
import com.web.repository.UserRepository;
import com.web.repository.VoucherRepository;
import com.web.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserRepository userRepository;

    public Voucher saveOrUpdate(Voucher voucher) {
        if(voucher.getId() == null){
            Optional<Voucher> ex = voucherRepository.findByCode(voucher.getCode());
            if(ex.isPresent()){
                throw new MessageException("Mã voucher đã tồn tại");
            }
        }
        else{
            Optional<Voucher> ex = voucherRepository.findByCode(voucher.getCode(), voucher.getId());
            if(ex.isPresent()){
                throw new MessageException("Mã voucher đã tồn tại");
            }
        }
        if(voucher.getMinAmount() <= voucher.getDiscount()){
            throw new MessageException("Số tiền giảm phải nhỏ hơn đơn hàng tối thiểu");
        }
        Voucher result = voucherRepository.save(voucher);
        List<User> users = userRepository.findByRole(Role.ROLE_USER);
        for(User u : users){
            mailService.sendEmail(u.getEmail(), "Có mã khuyến mại mới",
                    "Sử dụng mã khuyến mại "+ voucher.getCode()+" để được giảm giá "+voucher.getDiscount()+"<br>"+
                            "với đơn hàng từ :"+voucher.getMinAmount()+" trở lên"
                    , false, true);
        }
        return result;
    }


    public void delete(Long id) {
        voucherRepository.deleteById(id);
    }

    public List<Voucher> findAll() {
        List<Voucher> list = voucherRepository.findAll();
        return list;
    }

    public List<Voucher> available() {
        List<Voucher> list = voucherRepository.available();
        return list;
    }

    public Optional<Voucher> findById(Long id) {
        Optional<Voucher> ex = voucherRepository.findById(id);
        if(ex.isEmpty()){
            throw new MessageException("Not found");
        }
        return ex;
    }

    public void block(Long id) {
        Optional<Voucher> ex = voucherRepository.findById(id);
        if(ex.isEmpty()){
            throw new MessageException("Not found");
        }
        if (ex.get().getBlock() == true) {
            ex.get().setBlock(false);
        } else {
            ex.get().setBlock(true);
        }
        voucherRepository.save(ex.get());
    }

    public Optional<Voucher> findByCode(String code, Double amount) {
        Optional<Voucher> ex = voucherRepository.findByCode(code);
        if(ex.isEmpty()){
            throw new MessageException("Mã voucher không khả dụng");
        }
        if(ex.get().getBlock() == true){
            throw new MessageException("Mã voucher không thể sử dụng");
        }
        Date now = new Date(System.currentTimeMillis());
        if(ex.get().getMinAmount() > amount){
            throw new MessageException("Số tiền đơn hàng chưa đủ, hãy mua thêm "+(ex.get().getMinAmount() - amount)+" để được áp dụng voucher");
        }
        return ex;
    }
}

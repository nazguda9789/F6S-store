package com.web.api.admin;

import com.web.enums.InvoiceStatus;
import com.web.repository.HistoryInvoiceStatusRepository;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/statistic")
public class StatisticApi {

    @Autowired
    private HistoryInvoiceStatusRepository historyInvoiceStatusRepository;

    @GetMapping("/revenue-year")
    public List<Double> doanhThu(@RequestParam("year") Integer year){
        List<Double> list = new ArrayList<>();
        for(int i=1; i< 13; i++){
            Double sum = historyInvoiceStatusRepository.doanhThuThang(i, year, InvoiceStatus.HOAN_THANH);
            if(sum == null){
                sum = 0D;
            }
            list.add(sum);
        }
        return list;
    }
}

package com.web.dto;

import com.web.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class StatusAvailable {

    public static Map<InvoiceStatus, List<InvoiceStatus>> available = Map.of(
            InvoiceStatus.DA_HUY, new ArrayList<>(),
            InvoiceStatus.DANG_CHO_VAN_CHUYEN, List.of(InvoiceStatus.DA_VAN_CHUYEN, InvoiceStatus.DA_HUY),
            InvoiceStatus.DA_VAN_CHUYEN, List.of(InvoiceStatus.HOAN_THANH, InvoiceStatus.TRA_VE, InvoiceStatus.KHONG_NHAN_DUOC_HANG, InvoiceStatus.YEU_CAU_DOI_HANG),
            InvoiceStatus.HOAN_THANH, List.of(InvoiceStatus.YEU_CAU_DOI_HANG),
            InvoiceStatus.TRA_VE, new ArrayList<>(),
            InvoiceStatus.KHONG_NHAN_DUOC_HANG, new ArrayList<>(),
            InvoiceStatus.YEU_CAU_DOI_HANG, List.of(InvoiceStatus.DA_DOI_HANG, InvoiceStatus.HOAN_THANH),
            InvoiceStatus.DA_DOI_HANG, List.of(InvoiceStatus.YEU_CAU_DOI_HANG, InvoiceStatus.HOAN_THANH)
    );
}

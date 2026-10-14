package com.web.ghn;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class GhnCreateOrderRequest {

    private int payment_type_id;
    private String note;
    private String required_note;
    private String return_phone;
    private String return_address;
    private int return_district_id;
    private String return_ward_code;
    private String client_order_code;
    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private int to_district_id;
    private int cod_amount;
    private int weight;
    private int length;
    private int width;
    private int height;
    private int service_type_id;
    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private String code;
        private int quantity;
        private int price;
        private int length;
        private int width;
        private int height;
        private int weight;
    }
}

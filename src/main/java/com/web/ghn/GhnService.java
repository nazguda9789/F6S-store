package com.web.ghn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service
public class GhnService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String createOrder(GhnCreateOrderRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", "9ce1bd74-9fc8-11ef-819a-9e59fa82dda0");
        headers.set("ShopId", "5448630");

        HttpEntity<GhnCreateOrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create",
                entity,
                String.class
        );

        return response.getBody();
    }
}

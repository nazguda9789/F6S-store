package com.web.ghn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class GhnClient {
    @Value("${ghn.api.key:}")
    private String apiKey;
    @Value("${ghn.shopId:}")
    private String shopId;
    @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String baseUrl;
    @Value("${ghn.from-district-id:3440}")
    private Integer fromDistrictId;
    @Value("${ghn.from-ward-code:13010}")
    private String fromWardCode;
    private final RestTemplate restTemplate;

    public GhnClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private Map<String, Object> request(String path, Map<String, Object> data, boolean needsShop) {
        if (!StringUtils.hasText(apiKey) || (needsShop && !StringUtils.hasText(shopId))) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Vận chuyển chưa được cấu hình. Vui lòng liên hệ cửa hàng.");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (needsShop) headers.set("ShopId", shopId);
        try {
            Map<String, Object> body = restTemplate.exchange(
                    baseUrl.replaceAll("/+$", "") + path, HttpMethod.POST,
                    new HttpEntity<>(data, headers), Map.class).getBody();
            if (body == null || !(body.get("code") instanceof Number)
                    || ((Number) body.get("code")).intValue() != 200 || body.get("data") == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Dịch vụ vận chuyển chưa trả về dữ liệu hợp lệ.");
            }
            return body;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Tạm thời không kết nối được dịch vụ vận chuyển. Vui lòng thử lại sau.");
        }
    }

    public Map<String, Object> calculateShippingFee(Integer weight, Integer district, String ward) {
        if (weight == null || weight <= 0 || district == null || district <= 0 || !StringUtils.hasText(ward)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khối lượng và địa chỉ giao hàng không hợp lệ.");
        }
        return request("/v2/shipping-order/fee", Map.of(
                "from_district_id", fromDistrictId, "from_ward_code", fromWardCode,
                "to_district_id", district, "to_ward_code", ward, "weight", weight,
                "service_type_id", 2), true);
    }

    public Map<String, Object> getProvince() {
        return request("/master-data/province", Map.of(), false);
    }
    public Map<String, Object> getDistrict(Integer provinceId) {
        if (provinceId == null || provinceId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỉnh không hợp lệ.");
        return request("/master-data/district", Map.of("province_id", provinceId), false);
    }
    public Map<String, Object> getWard(Integer districtId) {
        if (districtId == null || districtId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quận/huyện không hợp lệ.");
        return request("/master-data/ward", Map.of("district_id", districtId), false);
    }
}

package com.web.api.test;

import com.web.ghn.GhnCreateOrderRequest;
import com.web.ghn.GhnService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghn")
public class GhnController {

    private final GhnService ghnService;

    public GhnController(GhnService ghnService) {
        this.ghnService = ghnService;
    }

    @PostMapping("/create-order")
    public String createOrder(@RequestBody GhnCreateOrderRequest request) throws Exception {
        return ghnService.createOrder(request);
    }
}

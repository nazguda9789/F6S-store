package com.web.api.user;

import com.web.dto.ApiResult;
import com.web.dto.CartRequest;
import com.web.dto.CartResponse;
import com.web.dto.Result;
import com.web.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
public class CartUserApi {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addCart(@RequestBody CartRequest cartRequest, HttpSession session){
        Result<Integer> result =cartService.addCart(session, cartRequest);
        ApiResult<Integer> apiResult = null;
        if(result.isSuccess() == false){
            apiResult = new ApiResult<>(result.getMessage(), result.getData(), 500);
        }
        else{
            apiResult = new ApiResult<>(result.getMessage(), result.getData(), 200);
        }
        return new ResponseEntity<>(apiResult, HttpStatus.OK);
    }

    @GetMapping("/my-cart")
    public ResponseEntity<?> getCart(HttpSession session){
        CartResponse cartResponse = cartService.myCart(session);
        return new ResponseEntity<>(cartResponse, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{idProductDetail}")
    public ResponseEntity<?> deleteCart(@PathVariable Long idProductDetail,HttpSession session){
        Result<Map<String, Object>> result = cartService.deleteCart(session, idProductDetail);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/update-quantity/{idProductDetail}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long idProductDetail,@RequestParam Integer quantity,HttpSession session){
        Result<Double> result = cartService.updateQuantity(session, quantity, idProductDetail);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}

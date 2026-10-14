package com.web.mapper;

import com.web.dto.CartResponse;
import com.web.dto.CartSession;
import org.springframework.stereotype.Service;

@Service
public class CartMapper {

    public CartResponse.CartDetail cartSessionToDetail(CartSession cartSession){
        CartResponse.CartDetail cartDetail = new CartResponse.CartDetail();
        cartDetail.setPrice(cartSession.getProductDetail().getPrice() == null?cartSession.getProductView().getPrice():cartSession.getProductDetail().getPrice());
        cartDetail.setColorName(cartSession.getProductDetail().getColor().getName());
        cartDetail.setSizeName(cartSession.getProductDetail().getSize().getName());
        cartDetail.setImage(cartSession.getColorImage() == null ? cartSession.getProductView().getImage() : cartSession.getColorImage());
        cartDetail.setQuantity(cartSession.getQuantity());
        cartDetail.setProductName(cartSession.getProductView().getName());
        cartDetail.setDataId(cartSession.getProductDetail().getId());
        cartDetail.setSlug(cartSession.getProductView().getSlug());
        cartDetail.setProductId(cartSession.getProductView().getId());
        return cartDetail;
    }

    public CartResponse.CartDetail cartDetailInjectToDetail(CartResponse.CartDetailInject cartDetailInject){
        CartResponse.CartDetail cartDetail = new CartResponse.CartDetail();
        cartDetail.setPrice(cartDetailInject.getPrice());
        cartDetail.setColorName(cartDetailInject.getColorName());
        cartDetail.setSizeName(cartDetailInject.getSizeName());
        cartDetail.setImage(cartDetailInject.getImage());
        cartDetail.setQuantity(cartDetailInject.getQuantity());
        cartDetail.setProductName(cartDetailInject.getProductName());
        cartDetail.setDataId(cartDetailInject.getDataId());
        cartDetail.setSlug(cartDetailInject.getSlug());
        cartDetail.setProductId(cartDetailInject.getProductId());
        return cartDetail;
    }

}

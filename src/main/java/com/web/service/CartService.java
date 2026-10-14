package com.web.service;

import com.web.dto.*;
import com.web.entity.*;
import com.web.mapper.CartMapper;
import com.web.repository.*;
import com.web.utils.TypeSystem;
import com.web.utils.UserUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductColorImageRepository productColorImageRepository;

    @Autowired
    private DiscountProductRepository discountProductRepository;

    @Autowired
    private DiscountProductService discountProductService;

    @Autowired
    private UserUtils userUtils;

    @Autowired
    private CartMapper cartMapper;

    public Result<Integer> addCart(HttpSession session, CartRequest cartRequest){
        Long userId = userUtils.getIdUserLogged();
        if(userId == null){
            return addCartToSession(session, cartRequest);
        }
        else{
            return addCartToDatabase(userId, cartRequest);
        }
    }

    public Result<Double> updateQuantity(HttpSession session, Integer quantity, Long productDetailId){
        Long userId = userUtils.getIdUserLogged();
        if(userId == null){
            return updateQuantityNotLogin(productDetailId, quantity, session);
        }
        else{
            return updateQuantityWhenLogin(productDetailId, quantity, userId, session);
        }
    }

    public CartResponse myCart(HttpSession session){
        Long userId = userUtils.getIdUserLogged();
        if(userId == null){
            return getByNotLogin(session);
        }
        else{
            return getCartUserLogged(session, userId);
        }
    }

    public CartResponse myCartCheckout(HttpSession session, List<Long> details){
        Long userId = userUtils.getIdUserLogged();
        CartResponse cartResponse = null;
        if(userId == null){
            cartResponse = getByNotLogin(session);
        }
        else{
            cartResponse = getCartUserLogged(session, userId);
        }

        // Lấy tất cả dataId hiện có trong cart
        List<Long> availableIds = cartResponse.getCartDetails()
                .stream()
                .map(CartResponse.CartDetail::getDataId)
                .toList();

        // Kiểm tra nếu có detailId nào không tồn tại trong cart -> throw lỗi
        for (Long d : details) {
            if (!availableIds.contains(d)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }
        }

        // Lọc danh sách cartDetails theo detailIds
        List<CartResponse.CartDetail> filtered = cartResponse.getCartDetails()
                .stream()
                .filter(c -> details.contains(c.getDataId()))
                .toList();

        cartResponse.setCartDetails(new ArrayList<>(filtered));

        // Tính lại tổng tiền
        double total = cartResponse.getCartDetails()
                .stream()
                .mapToDouble(c -> c.getPrice() * c.getQuantity())
                .sum();

        cartResponse.setTotalAmount(total);

        return cartResponse;
    }

    public Result<Map<String, Object>> deleteCart(HttpSession session, Long idProductDetail) {
        Long userId = userUtils.getIdUserLogged();
        if(userId == null){
            return deleteNotLogin(session, idProductDetail);
        }
        else{
            return deleteWhenLogin(idProductDetail, userId);
        }
    }

    public CartResponse getByNotLogin(HttpSession session){
        CartResponse cartResponse = new CartResponse();
        if(session.getAttribute(TypeSystem.cartName) == null){
            List<CartSession> list = new ArrayList<>();
            session.setAttribute(TypeSystem.cartName, list);
        }
        List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
        for(CartSession c : list){
            CartResponse.CartDetail cartDetail = cartMapper.cartSessionToDetail(c);
            cartResponse.getCartDetails().add(cartDetail);
        }
        cartResponse.setTotalAmount(totalAmount(session));
        return cartResponse;
    }

    public CartResponse getCartUserLogged(HttpSession session,Long userId){
        CartResponse cartResponse = new CartResponse();
        cartResponse.setTotalAmount(totalAmount(session));
        List<CartResponse.CartDetailInject> cartDetailInjects = cartRepository.getAllDetailByUser(userId);
        for(CartResponse.CartDetailInject c : cartDetailInjects){
            CartResponse.CartDetail cartDetail = cartMapper.cartDetailInjectToDetail(c);
            cartResponse.getCartDetails().add(cartDetail);
        }
        return cartResponse;
    }



    public Integer getQuantityCart(HttpSession session){
        Long userId = userUtils.getIdUserLogged();
        if(userId == null){
            if(session.getAttribute(TypeSystem.cartName) == null){
                return 0;
            }
            else{
                List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
                return list.size();
            }
        }
        else{
            mergeCart(session);
            return cartRepository.countByUser(userId).intValue();
        }
    }

    public Result<Integer> addCartToSession(HttpSession session, CartRequest cartRequest){
        if(session.getAttribute(TypeSystem.cartName) == null){
            List<CartSession> list = new ArrayList<>();
            session.setAttribute(TypeSystem.cartName, list);
        }

        List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
        if(cartRequest.getQuantity() < 1){
            return new Result<>(false, list.size(),"Số lượng phải lớn hơn 0");
        }
        for(CartSession c : list){
            if(c.getProductDetail().getId().equals(cartRequest.getId())){
                c.setQuantity(c.getQuantity() + cartRequest.getQuantity());
                return new Result<>(true, list.size(), "Đã cập nhật số lượng sản phẩm trong giỏ hàng");
            }
        }

        Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(cartRequest.getId());
        if(productDetailOptional.isEmpty()){
            Result<Integer> result = new Result<>(false, list.size(),"Không tìm thấy chi tiết của sản phẩm bạn đang quan tâm");
            return result;
        }
        else if(productDetailOptional.get().getActive() == false){
            Result<Integer> result = new Result<>(false, list.size(),"Màu: "+productDetailOptional.get().getColor().getName()+", size: "+productDetailOptional.get().getSize().getName()+" đã ngừng bán ");
            return result;
        }
        else if(productDetailOptional.get().getStock() < 1){
            Result<Integer> result = new Result<>(false, list.size(),"Màu: "+productDetailOptional.get().getColor().getName()+", size: "+productDetailOptional.get().getSize().getName()+" đã hết hàng ");
            return result;
        }

        ProductView productView = productRepository.findViewById(productDetailOptional.get().getProductId());
        DiscountProduct discountProduct = discountProductRepository.findActiveDiscount(productView.getId(), LocalDateTime.now());
        if(discountProduct != null){
            productDetailOptional.get().setOldPrice(productDetailOptional.get().getPrice());
            productDetailOptional.get().setPrice(discountProductService.calculatePrice(productDetailOptional.get(), discountProduct));
        }
        String imageColor = productColorImageRepository.getImageByProductAndColor(productDetailOptional.get().getProductId(), productDetailOptional.get().getColor().getId());
        if(productView != null){
            list.add(new CartSession(productDetailOptional.get(), cartRequest.getQuantity(), productView, imageColor));
        }
        else{
            return new Result<>(false, list.size(),"Xin lỗi bạn, Sản phẩm "+productView.getName()+" đã ngưng bán");
        }

        Result<Integer> result = new Result<>(true, list.size(),"Thêm giỏ hàng thành công");
        return result;
    }

    public Result<Integer> addCartToDatabase(Long userId, CartRequest cartRequest){
        if(cartRepository.findIdByUserAndProductDetail(userId, cartRequest.getId()) != null){
            Result<Integer> result = new Result<>(false, cartRepository.countByUser(userId).intValue(),"Sản phẩm đã có trong giỏ hàng");
            return result;
        }
        Optional<ProductDetail> productDetailOptional = productDetailRepository.findById(cartRequest.getId());
        if(productDetailOptional.isEmpty()){
            Result<Integer> result = new Result<>(false, cartRepository.countByUser(userId).intValue(),"Không tìm thấy chi tiết của sản phẩm bạn đang quan tâm");
            return result;
        }
        else if(productDetailOptional.get().getActive() == false){
            Result<Integer> result = new Result<>(false, cartRepository.countByUser(userId).intValue(),"Màu: "+productDetailOptional.get().getColor().getName()+", size: "+productDetailOptional.get().getSize().getName()+" đã ngừng bán ");
            return result;
        }
        else if(productDetailOptional.get().getStock() < 1){
            Result<Integer> result = new Result<>(false, cartRepository.countByUser(userId).intValue(),"Màu: "+productDetailOptional.get().getColor().getName()+", size: "+productDetailOptional.get().getSize().getName()+" đã hết hàng ");
            return result;
        }
        Cart cart = new Cart();
        cart.setProductDetail(productDetailOptional.get());
        cart.setQuantity(cartRequest.getQuantity());
        cart.setUserId(userId);
        cartRepository.save(cart);
        Result<Integer> result = new Result<>(true, cartRepository.countByUser(userId).intValue(),"Thêm giỏ hàng thành công");
        return result;
    }

    public Result<Map<String, Object>> deleteNotLogin(HttpSession session, Long productDetailId){
        if(session.getAttribute(TypeSystem.cartName) == null){
            List<CartSession> list = new ArrayList<>();
            session.setAttribute(TypeSystem.cartName, list);
        }

        List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
        Boolean check = false;
        for(int i=0; i< list.size(); i++){
            if(list.get(i).getProductDetail().getId().equals(productDetailId)){
                check = true;
                list.remove(i);
                break;
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("size", list.size());
        map.put("total", totalAmount(session));
        return new Result<>(check, map,check==false?"Có lỗi khi xóa":"Xóa giỏ hàng thành công");
    }

    public Result<Map<String, Object>> deleteWhenLogin(Long productDetailId, Long userId){
        int numRecordDel = cartRepository.deleteByUserAndDetailId(userId, productDetailId);

        Map<String, Object> map = new HashMap<>();
        map.put("size", cartRepository.countByUser(userId));
        map.put("total", cartRepository.totalCartByUser(userId));
        return new Result<>(numRecordDel > 0, map,numRecordDel < 1?"Có lỗi khi xóa":"Xóa giỏ hàng thành công");
    }

    public Double totalAmount(HttpSession session){
        Long userId = userUtils.getIdUserLogged();
        Double amount = 0D;
        if(userId == null){
            if(session.getAttribute(TypeSystem.cartName) == null){
                List<CartSession> list = new ArrayList<>();
                session.setAttribute(TypeSystem.cartName, list);
            }

            List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
            for(CartSession c : list){
                if(c.getProductDetail().getPrice() == null){
                    amount += c.getQuantity() * c.getProductView().getPrice();
                }
                else {
                    amount += c.getQuantity() * c.getProductDetail().getPrice();
                }
            }
        }
        else{
            amount = cartRepository.totalCartByUser(userId);
        }
        return amount;
    }

    public void mergeCart(HttpSession session){
        Long userId = userUtils.getIdUserLogged();
        if(userId != null){
            if(session.getAttribute(TypeSystem.cartName) != null){
                List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
                List<Cart> cartsMerge = new ArrayList<>();
                for(CartSession c : list){
                    Long idCart = cartRepository.findIdByUserAndProductDetail(userId, c.getProductDetail().getId());
                    if(idCart == null){
                        Cart cart = new Cart();
                        cart.setQuantity(c.getQuantity());
                        cart.setUserId(userId);
                        cart.setProductDetail(c.getProductDetail());
                        cartsMerge.add(cart);
                    }
                }
                if(cartsMerge.size() > 0){
                    cartRepository.saveAll(cartsMerge);
                }
            }
        }
    }

    public Result<Double> updateQuantityNotLogin(Long productDetailId, Integer quantity, HttpSession session){
        if(session.getAttribute(TypeSystem.cartName) == null){
            List<CartSession> list = new ArrayList<>();
            session.setAttribute(TypeSystem.cartName, list);
        }
        List<CartSession> list = (List<CartSession>) session.getAttribute(TypeSystem.cartName);
        Boolean check = false;
        Double total = totalAmount(session);
        if(!quantity.equals(1) && !quantity.equals(-1)){
            return new Result<>(check, total, "Số lượng phải là 1 hoặc -1");
        }

        for(int i=0; i< list.size(); i++){
            if(list.get(i).getProductDetail().getId().equals(productDetailId)){
                Integer quan = list.get(i).getQuantity() + quantity;
                if(quan == 0){
                    return new Result<>(check, total, "Số lượng không được = 0");
                }
                else{
                    check = true;
                    list.get(i).setQuantity(quan);
                }
            }
        }
        total = totalAmount(session);
        return new Result<>(check, total,check==false?"Có lỗi khi thay đổi số lượng":"Thay đô số lượng thành công");
    }

    public Result<Double> updateQuantityWhenLogin(Long productDetailId, Integer quantity, Long userId, HttpSession session){
        Integer quantityCart = cartRepository.getQuantityByDetailAndUser(productDetailId, userId);
        if(quantityCart == null){
            return new Result<>(false, totalAmount(session), "Không tồn tại giỏ hàng");
        }
        else if(!quantity.equals(1) && !quantity.equals(-1)){
            return new Result<>(false, totalAmount(session), "Số lượng phải là 1 hoặc -1");
        }
        else{
            Integer quan = quantityCart + quantity;
            if(quan < 1){
                return new Result<>(false, totalAmount(session), "Số lượng không được < 0");
            }
            else{
                cartRepository.updateQuantity(userId,productDetailId, quan);
                return new Result<>(true, totalAmount(session), "Cập nhật số lượng thành công");
            }
        }
    }

    public Float getWeightCart(HttpSession session, CartResponse cartResponse){
        if(cartResponse == null){
            cartResponse = myCart(session);
        }
        List<CartResponse.CartDetail> cartDetails = cartResponse.getCartDetails();
        List<Long> idProducs = cartDetails.stream()
                .map(CartResponse.CartDetail::getProductId)
                .toList();
        List<ProductWeight> productWeights = productRepository.sumWeightByIds(idProducs);
        Float weight = 0F;
        for(CartResponse.CartDetail c : cartDetails){
            for(ProductWeight p : productWeights){
                if(p.getId().equals(c.getProductId())){
                    weight += p.getWeight() * c.getQuantity();
                }
            }
        }

        return weight;
    }
}

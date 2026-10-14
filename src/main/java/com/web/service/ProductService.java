package com.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web.dto.*;
import com.web.entity.*;
import com.web.exception.MessageException;
import com.web.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private InvoiceDetailRepository invoiceDetailRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductColorImageRepository productColorImageRepository;

    @Autowired
    private DiscountProductRepository discountProductRepository;

    @Autowired
    private DiscountProductService discountProductService;

    @Autowired
    private CartRepository cartRepository;

    @Value("brand.name")
    String brandName;

    @Transactional
    public Product createProduct(ProductCreateRequest request) {
        // Tạo Product
        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setImage(request.getImage());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(request.getActive());
        product.setOlePrice(request.getOldPrice());
        product.setWeight(request.getWeight());

        product = productRepository.save(product);

        // Thêm ProductDetails
        List<ProductDetail> details = new ArrayList<>();
        Set<Long> colorIds = new HashSet<>();

        for (ProductCreateRequest.ProductDetailRequest detailRequest : request.getDetails()) {
            ProductDetail detail = new ProductDetail();
            detail.setProductId(product.getId());
            detail.setColorId(detailRequest.getColorId());
            detail.setSizeId(detailRequest.getSizeId());
            Color color = new Color();
            color.setId(detailRequest.getColorId());
            Size size = new Size();
            size.setId(detailRequest.getSizeId());
            detail.setColor(color);
            detail.setSize(size);
            detail.setStock(detailRequest.getStock());
            detail.setPrice(detailRequest.getPrice());
            detail.setSku(detailRequest.getSku());
            detail.setOldPrice(detailRequest.getOldPrice());
            detail.setActive(true);
            details.add(detail);
            colorIds.add(detailRequest.getColorId());
        }
        productDetailRepository.saveAll(details);

        // Thêm ProductImages
        List<ProductImage> images = new ArrayList<>();
        for (String link : request.getImageLinks()) {
            ProductImage img = new ProductImage();
            img.setLinkImage(link);
            img.setProduct(product);
            images.add(img);
        }
        productImageRepository.saveAll(images);


        List<ProductCategory> pcs = new ArrayList<>();
        for (Long categoryId : request.getCategoryIds()) {
            ProductCategory pc = new ProductCategory();
            pc.setProduct(product);
            Category category = new Category();
            category.setId(categoryId);
            pc.setCategory(category);
            pcs.add(pc);
        }
        productCategoryRepository.saveAll(pcs);

        List<ProductColorImage> colorImages = new ArrayList<>();
        for (Long colorId : colorIds) {
            ProductColorImage pci = new ProductColorImage();
            pci.setProductId(product.getId());
            pci.setColorId(colorId);
            pci.setImage(null);
            colorImages.add(pci);
        }
        productColorImageRepository.saveAll(colorImages);
        return product;
    }


    @Transactional
    public Product updateProduct(ProductCreateRequest request, Long id) {
        // Tạo Product
        Product product = productRepository.findById(id).get();
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setImage(request.getImage());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());
        product.setCreatedAt(LocalDateTime.now());
        product.setActive(request.getActive());
        product.setOlePrice(request.getOldPrice());
        product.setWeight(request.getWeight());

        product = productRepository.save(product);

        // Thêm ProductDetails
        List<ProductDetail> details = new ArrayList<>();
        Set<Long> colorIds = new HashSet<>();
        for (ProductCreateRequest.ProductDetailRequest detailRequest : request.getDetails()) {
            if(checkExistDetail(detailRequest.getColorId(), detailRequest.getSizeId(), id) == false){
                ProductDetail detail = new ProductDetail();
                detail.setProductId(product.getId());
                detail.setColorId(detailRequest.getColorId());
                detail.setSizeId(detailRequest.getSizeId());
                Color color = new Color();
                color.setId(detailRequest.getColorId());
                Size size = new Size();
                size.setId(detailRequest.getSizeId());
                detail.setColor(color);
                detail.setSize(size);
                detail.setStock(detailRequest.getStock());
                detail.setPrice(detailRequest.getPrice());
                detail.setSku(detailRequest.getSku());
                detail.setActive(true);
                details.add(detail);
                colorIds.add(detailRequest.getColorId());
            }
        }
        productDetailRepository.saveAll(details);

        // Thêm ProductImages
        List<ProductImage> images = new ArrayList<>();
        for (String link : request.getImageLinks()) {
            ProductImage img = new ProductImage();
            img.setLinkImage(link);
            img.setProduct(product);
            images.add(img);
        }
        productImageRepository.saveAll(images);

        productCategoryRepository.deleteByProduct(product.getId());
        List<ProductCategory> pcs = new ArrayList<>();
        for (Long categoryId : request.getCategoryIds()) {
            ProductCategory pc = new ProductCategory();
            pc.setProduct(product);
            Category category = new Category();
            category.setId(categoryId);
            pc.setCategory(category);
            pcs.add(pc);
        }
        productCategoryRepository.saveAll(pcs);

        List<ProductColorImage> colorImages = new ArrayList<>();
        for (Long colorId : colorIds) {
            if(productColorImageRepository.checkExist(colorId, id) == null){
                ProductColorImage pci = new ProductColorImage();
                pci.setProductId(product.getId());
                pci.setColorId(colorId);
                pci.setImage(null);
                colorImages.add(pci);
            }
        }
        productColorImageRepository.saveAll(colorImages);

        return product;
    }

    public Page<Product> allProductByAdmin(ProductSearchAdmin search, Pageable pageable) {
        ProductSpecification spec = new ProductSpecification(search.getIdCategory(), search.getSearch(), search.getActive(), search.getSmallPrice(), search.getLargePrice());
        Page<Product> page = productRepository.findAll(spec, pageable);
        List<Long> productIds = page.getContent().stream().map(Product::getId).toList();
        List<ProductStatisticProjection> list = productRepository.getProductStatistics(productIds);
        List<Product> products = page.getContent();
        // làm tiếp
        Map<Long, ProductStatisticProjection> statMap = list.stream()
                .collect(Collectors.toMap(ProductStatisticProjection::getProductId, Function.identity()));
        for (Product product : products) {
            ProductStatisticProjection stat = statMap.get(product.getId());
            if (stat != null) {
                product.setStock(stat.getTotalStock());
            } else {
                product.setStock(0L);
                product.setSold(0L);
            }
        }
        return page;
    }

    public Long totalProduct(){
        return productRepository.count();
    }

    public Product findById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if(product.isPresent()){
            return product.get();
        }
        else {
            return null;
        }
    }

    public ProductDetail updateDetail(ProductDetail productDetail) {
        productDetailRepository.save(productDetail);
        return productDetail;
    }

    public String deleteDetail(Long id) {
        InvoiceDetail invoiceDetail = invoiceDetailRepository.findByDetail(id);
        if(invoiceDetail != null){
            throw new MessageException("Chi tiết sản phẩm này đã có người mua, hãy inactive nó");
        }

        ProductDetail productDetail = productDetailRepository.findById(id).get();
        Long colorId = productDetail.getColor().getId();
        Long productId = productDetail.getProductId();

        productDetailRepository.deleteById(id);
        Long idDetail = productDetailRepository.findByColorAndProductId(colorId, productId,id);
        if(idDetail == null){
            productColorImageRepository.deleteByColorAndProduct(colorId, productId);
            return String.valueOf(colorId);
        }
        return "no-delete";
    }

    @Transactional
    public void deleteProduct(Long id) {
        Long invoiceDetail = invoiceDetailRepository.countByProduct(id);
        if(invoiceDetail != null && invoiceDetail > 0){
            throw new MessageException("Sản phẩm này đã có người mua, hãy inactive nó");
        }
        productImageRepository.deleteByProduct(id);
        cartRepository.deleteByProduct(id);
        discountProductRepository.deleteByProductId(id);
        productDetailRepository.deleteByProduct(id);
        productCategoryRepository.deleteByProduct(id);
        productRepository.deleteById(id);
    }

    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

    public Page<ProductView> getSanPhamBanChay(){
        Page<ProductView> page = productRepository.getSanPhamBanChay(pageable);
        return page;
    }

    public Page<ProductView> getProductByCategory(Long idCategory){
        Page<ProductView> page = productRepository.getProductByCategory(idCategory,pageable);
        return page;
    }

    public Page<ProductView> getProductByParam(String search, Pageable pageable){
        Page<ProductView> page = productRepository.getProductByParam("%"+search+"%",pageable);
        return page;
    }

    public Product findBySlug(String slug){
        Product product = productRepository.findBySlug(slug);
        return product;
    }

    public Boolean checkExistDetail(Long colorId, Long sizeId, Long productId){
        if(productDetailRepository.checkExist(colorId, sizeId, productId) != null){
            return true;
        }
        return false;
    }

    public List<ProductDetailView> getVariant(String slug){
        List<ProductDetailView> list = new ArrayList<>();
        Long idProduct = productRepository.findIdBySlug(slug);
        if(idProduct == null){
            throw new MessageException("Product not found");
        }
        DiscountProduct discountResponse = discountProductRepository.findActiveDiscount(idProduct, LocalDateTime.now());
        List<ProductDetail> productDetails = productDetailRepository.findByProductIdAndActive(idProduct);
        if(discountResponse != null){
            for(ProductDetail p : productDetails){
                p.setOldPrice(p.getPrice());
                p.setPrice(discountProductService.calculatePrice(p,discountResponse));
            }
        }
        List<ProductColorImageView> productColorImages = productColorImageRepository.findByProduct(idProduct);

        for(ProductColorImageView p : productColorImages){
            ProductDetailView productDetailView = new ProductDetailView();
            productDetailView.setColorCode(p.getCode());
            productDetailView.setColorName(p.getColorName());
            productDetailView.setColorId(p.getColorId());
            productDetailView.setImage(p.getImage());

            for(ProductDetail pd : productDetails){
                if(pd.getColorId() == p.getColorId()){
                    ProductDetailView.Sizes size = new ProductDetailView.Sizes();
                    size.setSizeName(pd.getSize().getName());
                    size.setSizeId(pd.getSize().getId());
                    size.setDetailId(pd.getId());
                    size.setSku(pd.getSku());
                    size.setStock(pd.getStock());
                    size.setPrice(pd.getPrice());
                    size.setOldPrice(pd.getOldPrice());
                    productDetailView.getSizes().add(size);
                }
            }
            list.add(productDetailView);
        }

        return list;
    }

    public List<ProductView> productSamePrice(Double price) {
        Double small = price - (price * 20 / 100);
        Double large = price + (price * 20 / 100);
        List<ProductView> list = productRepository.samePrice(small, large);
        return list;
    }

    public String genJsonByProduct(Product product, HttpServletRequest request){
        // Tạo JSON-LD
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonLd = mapper.createObjectNode();
        jsonLd.put("@context", "https://schema.org/");
        jsonLd.put("@type", "Product");
        jsonLd.put("name", product.getName());
        jsonLd.put("image", product.getImage());
        jsonLd.put("description", product.getMetaDescription());
        jsonLd.put("sku", String.valueOf(product.getId()));

        ObjectNode brand = mapper.createObjectNode();
        brand.put("@type", "Brand");
        brand.put("name", brandName);
        jsonLd.set("brand", brand);

        ObjectNode offers = mapper.createObjectNode();
        offers.put("@type", "Offer");
        String domain = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        // Ghép URL sản phẩm
        String productUrl = domain + "/product/" + product.getSlug();
        offers.put("url", productUrl);
        offers.put("priceCurrency", "VND");
        offers.put("price", product.getPrice().toString());
        offers.put("availability", "https://schema.org/InStock");
        jsonLd.set("offers", offers);

        try {
            String jsonLdString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonLd);
            return jsonLdString;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String genJsonByCategory(Category category, List<ProductView> products, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonLd = mapper.createObjectNode();

        jsonLd.put("@context", "https://schema.org/");
        jsonLd.put("@type", "ItemList");
        jsonLd.put("name", category.getName());
        jsonLd.put("description", category.getMetaDescription());

        ArrayNode itemListElement = mapper.createArrayNode();

        String domain = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        for (int i = 0; i < products.size(); i++) {
            ProductView product = products.get(i);
            ObjectNode listItem = mapper.createObjectNode();
            listItem.put("@type", "ListItem");
            listItem.put("position", i + 1);

            ObjectNode productNode = mapper.createObjectNode();
            productNode.put("@type", "Product");
            productNode.put("name", product.getName());
            productNode.put("image", product.getImage());
            productNode.put("sku", String.valueOf(product.getId()));

            ObjectNode brand = mapper.createObjectNode();
            brand.put("@type", "Brand");
            brand.put("name", "F6S");
            productNode.set("brand", brand);

            ObjectNode offers = mapper.createObjectNode();
            offers.put("@type", "Offer");
            String productUrl = domain + "/product/" + product.getSlug();
            offers.put("url", productUrl);
            offers.put("priceCurrency", "VND");
            offers.put("price", product.getPrice().toString());
            offers.put("availability", "https://schema.org/InStock");
            productNode.set("offers", offers);

            listItem.set("item", productNode);
            itemListElement.add(listItem);
        }

        jsonLd.set("itemListElement", itemListElement);

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonLd);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Page<ProductView> findByCategory(Long id, Pageable pageable) {
        return productRepository.findByCategory(id, pageable);
    }

    public Page<ProductView> searchFull(ProductSearch search, Pageable pageable) {
        Page<ProductView> page = null;
        if(search.getCategoriesId().size() > 0){
            page =  productRepository.searchFull(search.getMinPrice(), search.getMaxPrice(), search.getCategoriesId(), pageable);
        }
        else{
            page =  productRepository.searchFull(search.getMinPrice(), search.getMaxPrice(), pageable);
        }
        return page;
    }

    public Page<ProductResponseAdmin> getProductAdmin(Pageable pageable, String search){
        if(search == null){
            search = "";
        }
        search = "%"+search+"%";
        Page<ProductView> page = productRepository.findByParam(search, pageable);
        List<Long> listIdProduct = page.getContent().stream().map(ProductView::getId).toList();
        List<ProductDetail> productDetails = productDetailRepository.findAllByProductId(listIdProduct);

        // Gom chi tiết theo idProduct
        Map<Long, List<ProductDetail>> detailMap = productDetails.stream()
                .collect(Collectors.groupingBy(ProductDetail::getProductId));

        // Map sang DTO
        List<ProductResponseAdmin> responseList = page.getContent().stream().map(product -> {
            ProductResponseAdmin dto = new ProductResponseAdmin();
            dto.setProductView(product);
            dto.setProductDetails(detailMap.getOrDefault(product.getId(), new ArrayList<>()));
            return dto;
        }).toList();

        // Trả về Page DTO (dùng PageImpl)
        return new PageImpl<>(responseList, pageable, page.getTotalElements());
    }
}

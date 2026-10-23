async function getProductByCategory(e){
    var $btn = $(e);
    var category = $btn.data("category");
    var name = $btn.data("name");
    var slug = $btn.data("slug");
    document.getElementById("tencate").innerText = name
    document.getElementById("link-xem-them-sp").href = '/category/'+slug
    $(".btn-category-index").removeClass("active-category-index");
    $btn.addClass("active-category-index");
    const response = await fetch(`/api/v1/products/find-by-category/${category}`, {
    });
    var list = await response.json();
    console.log(list)
    var main = ``;
    for(i=0; i< list.length; i++){
        main +=
            `<article class="col-6 col-sm-6 col-md-4 col-lg-20p">
            <div class="product-card h-100" itemscope itemtype="https://schema.org/Product">
                <figure class="product-media m-0">
                    <a href="/product/${list[i].slug}" itemprop="url" aria-label="${list[i].name}">
                        <img class="img-product-list"
                             src="${list[i].image}"
                             loading="lazy" decoding="async"
                             alt="${list[i].name}"
                             itemprop="image" />
                    </a>
                </figure>
                <div class="product-body">
                    <div class="product-meta">
                        <span itemprop="sku">F6S</span>
                    </div>
                    <h3 class="product-title h6" itemprop="name">
                        <a href="/product/${list[i].slug}" class="product-name-list">${list[i].name}</a>
                    </h3>
                    <div class="product-price">
                    <span itemprop="offers" itemscope itemtype="https://schema.org/Offer">
                        <span itemprop="price" content="${list[i].finalPrice}">${formatVND(list[i].finalPrice)}</span>
                        <meta itemprop="priceCurrency" content="VND" />
                        <link itemprop="availability" href="https://schema.org/InStock" />
                        ${(list[i].discountAmount > 0) ? `
                            <span class="discount-product-list">
                                - ${list[i].discountAmount.toLocaleString('de-DE')} đ
                                ${(list[i].discountPercent > 0) ? `
                                    <span class="badge bg-danger">- ${list[i].discountPercent} %</span>
                                ` : ''}
                            </span>
                        ` : ''}
                    </span>
                    </div>
                </div>
            </div>
        </article>`
    }
    document.getElementById("listproductcategory").innerHTML = main;
}


$(function () {
    const input = document.getElementById('inputsearchmobile');
    $('#inputsearchmobile').on('keyup', function (e) {
        if(this.value != ''){
            searchProductModal(0, this.value )
        }
        if (e.key === 'Enter') {
            e.preventDefault(); // chặn submit form mặc định (nếu có)
            input.blur();
        }
    });
});

async function searchProductModal(page, search){
    var url = `/api/v1/products/search-by-param?page=${page}&size=5&search=${encodeURIComponent(search || '')}`;
    const response = await fetch(url, {
        method: 'GET'
    });
    if(response.status > 300){
        toastr.error("Đã có lỗi xảy ra"); return;
    }
    var result = await response.json();
    var totalPage = result.totalPages;
    var list = result.content;
    var main = '';
    for (i = 0; i < list.length; i++) {
        main += `<div class="singlesearch">
                    <div class="p40"><a href="/product/${list[i].slug}"><img class="imgprosearchp" src="${list[i].image}"></a></div>
                    <div class="p60">
                        <a href="/product/${list[i].slug}"><span class="tenspsearch">${list[i].name}</span><br>
                            <span class="tenspsearch">${formatVND(list[i].finalPrice)}</span></a>
                    </div>
                </div>`
    }
    document.getElementById("listproductsearchmobile").innerHTML = main;
    renderPagination(page, totalPage, search);
}


function renderPagination(currentPage, totalPages, search) {
    let maxVisiblePages = 3;
    let mainpage = '';
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="searchProductModal(${currentPage - 1}, '${search}')"><a class="page-link">&laquo;</a></li>`;
    }

    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="searchProductModal(0, '${search}')"><a class="page-link">1</a></li>`;
    }
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="searchProductModal(${i}, '${search}')" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="searchProductModal(${totalPages - 1}, '${search}')"><a class="page-link">${totalPages}</a></li>`;
    }
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="searchProductModal(${currentPage + 1}, '${search}')"><a class="page-link">&raquo;</a></li>`;
    }
    document.getElementById("pageable").innerHTML = mainpage;
}


let selectedDetailId = null;


async function getVariant() {
    const slug = window.location.pathname.split('/').pop();
    const list = await (await fetch(`/api/v1/products/variant/${slug}`)).json();
    const $thumbnailContainer = $("#thumbnail-container").empty();
    const $listSize = $("#list-size");
    console.log(list)
    // render color thumbnails
    $.each(list, function(i, color) {
        // 1. Tạo sẵn khung div với các thuộc tính chung
        const $thumb = $('<div>', {
            class: 'thumbnail' + (i === 0 ? ' active-thumbnail' : ''),
            'data-color-id': color.colorId
        });

        // 2. Kiểm tra nếu có ảnh thì append thẻ img
        if (color.image != null && color.image != '') {
            $thumb.append(
                $('<img>', { src: color.image, alt: color.colorName })
            );
        }
        // 3. Nếu không có ảnh thì thêm màu nền qua thuộc tính css() của jQuery
        else {
            $thumb.css('background', color.colorCode);
        }

        // 4. Đẩy phần tử vào container
        $thumbnailContainer.append($thumb);
    });

    // hàm render size cho color
    function renderSizes(color) {
        $listSize.empty();
        $.each(color.sizes, function(j, size) {
            const $btn = $('<button>', {
                type: 'button',
                class: 'btn btn-outline-secondary btn-sm me-2 btn-size-dt',
                html: size.sizeName + (j === 0 ? '<span class="checkmark"></span>' : ''),
                disabled: size.stock === 0,
                'data-detail-id': size.detailId,
                'data-detail-price': size.price,
                'data-detail-old-price': size.oldPrice,
                'data-sku':size.sku
            });
            $listSize.append($btn);
        });

        // set detailId mặc định là size đầu tiên còn stock
        const firstAvailable = color.sizes.find(s => s.stock > 0);
        selectedDetailId = firstAvailable ? firstAvailable.detailId : null;
        $("#product-code").text(firstAvailable.sku);
        if(firstAvailable.price != null && firstAvailable.price > 0){
            document.getElementById("price-product").innerText = formatVND(firstAvailable.price)
        }
        if(firstAvailable.oldPrice != null && firstAvailable.oldPrice > 0){
            $(".old-price-product-main").text(formatVND(firstAvailable.oldPrice))
        }
    }

    // render size cho color đầu tiên
    if (list.length > 0) {
        $("#color-name-detail").text(list[0].colorName);
        renderSizes(list[0]);
    }

    // click chọn color
    $thumbnailContainer.on('click', '.thumbnail', function() {
        const $this = $(this);
        const colorId = $this.data('color-id');
        const color = list.find(c => c.colorId === colorId);
        if (!color) return;

        // set active-thumbnail
        $thumbnailContainer.find('.thumbnail').removeClass('active-thumbnail');
        $this.addClass('active-thumbnail');

        // cập nhật tên color và size
        $("#color-name-detail").text(color.colorName);
        renderSizes(color);
    });

    // click chọn size
    $listSize.on('click', '.btn-size-dt', function() {
        if ($(this).is(':disabled')) return;
        $listSize.find('.checkmark').remove();
        $(this).append('<span class="checkmark"></span>');
        selectedDetailId = $(this).data('detail-id');
        $("#product-code").text($(this).data('sku'));

        if($(this).data('detail-price') != null && $(this).data('detail-price') > 0){
            $("#price-product").text(formatVND($(this).data('detail-price')));
        }
        if($(this).data('detail-old-price') != null && $(this).data('detail-old-price') > 0){
            $(".old-price-product-main").text(formatVND($(this).data('detail-old-price')));
        }
    });
}


async function searchFull(page){
    $("#filterProductModal").modal("hide")
    var categories = $('.checkbox-category:checked').map(function() {
        return $(this).val();
    }).get();
    var values = slider.noUiSlider.get();
    var min = Number(values[0]);
    var max = Number(values[1]);
    var sort = $("#sort").val()
    var payload = {
        categoriesId: categories,
        minPrice: min,
        maxPrice:max
    }
    const response = await fetch(`/api/v1/products/filter?page=${page}&size=16&sort=${sort}`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(payload)
    });
    var result = await response.json();
    console.log(result)
    var list = result.content
    var totalPage = result.totalPages;
    console.log(totalPage)
    var main = ``;
    for(i=0; i< list.length; i++){
        main +=
            `<article class="col-6 col-sm-6 col-md-4 col-lg-3">
            <div class="product-card h-100" itemscope itemtype="https://schema.org/Product">
                <figure class="product-media m-0">
                    <a href="/product/${list[i].slug}" itemprop="url" aria-label="${list[i].name}">
                        <img class="img-product-list"
                             src="${list[i].image}"
                             loading="lazy" decoding="async"
                             alt="${list[i].name}"
                             itemprop="image" />
                    </a>
                </figure>
                <div class="product-body">
                    <div class="product-meta">
                        <span itemprop="sku">F6S</span>
                    </div>
                    <h3 class="product-title h6" itemprop="name">
                        <a href="/product/${list[i].slug}" class="product-name-list">${list[i].name}</a>
                    </h3>
                    <div class="product-price">
                    <span itemprop="offers" itemscope itemtype="https://schema.org/Offer">
                        <span itemprop="price" content="${list[i].finalPrice}">${formatVND(list[i].finalPrice)}</span>
                        <meta itemprop="priceCurrency" content="VND" />
                        <link itemprop="availability" href="https://schema.org/InStock" />
                        ${(list[i].discountAmount > 0) ? `
                            <span class="discount-product-list">
                                - ${list[i].discountAmount.toLocaleString('de-DE')} đ
                                ${(list[i].discountPercent > 0) ? `
                                    <span class="badge bg-danger">- ${list[i].discountPercent} %</span>
                                ` : ''}
                            </span>
                        ` : ''}
                    </span>
                    </div>
                </div>
            </div>
        </article>`
    }
    document.getElementById("list-product-filter").innerHTML = main;
    renderPagination(page, totalPage)
}

function renderPagination(currentPage, totalPages) {
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="searchFull(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="searchFull(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="searchFull(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }

    // dấu ...
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang cuối
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="searchFull(${totalPages - 1})"><a class="page-link">${totalPages}</a></li>`;
    }

    // nút next
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="searchFull(${currentPage + 1})"><a class="page-link">&raquo;</a></li>`;
    }

    document.getElementById("pageable").innerHTML = mainpage;
}

function formatVND(amount) {
    if (typeof amount !== "number") {
        amount = Number(amount) || 0; // đảm bảo là number
    }
    return amount.toLocaleString('vi-VN') + " đ";
}

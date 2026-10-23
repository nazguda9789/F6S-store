const productSearchInput = document.getElementById('productSearch');
const productTableBody = document.getElementById('productTableBody');
const noProductFound = document.getElementById('noProductFound');
const orderItemsTableBody = document.getElementById('orderItemsTableBody');
const noOrderItem = document.getElementById('noOrderItem');
const createOrderBtn = document.getElementById('createOrderBtn');
$( document ).ready(function() {
    loadProductInvoice(0);
});

function searchSanPham(e){
    if (e.key === "Enter" || e.keyCode === 13) {
        loadProductInvoice(0);
    }
}

async function loadProductInvoice(page) {
    var search = $("#productSearch").val();
    const response = await fetch(`/adm-prj/qtht-001/api/product/product-admin?page=${page}&size=5&search=${search}`, {
        method: 'GET'
    });
    var result = await response.json();
    var totalPage = result.totalPages;
    console.log(result);
    renderPagination(page, totalPage)
    displayProducts(result.content)
}

function displayProducts(products) {
    productTableBody.innerHTML = '';
    if (products.length === 0) {
        noProductFound.style.display = 'block';
        return;
    }
    noProductFound.style.display = 'none';

    products.forEach(product => {
        const totalVariants = product.productDetails.length;

        let isFirstRow = true;
        product.productDetails.forEach(detail => {
                const row = document.createElement('tr');
                row.className = 'variant-row';

                let stockClass = '';
                if (detail.stock > 10) stockClass = 'in-stock';
                else if (detail.stock > 0) stockClass = 'low-stock';
                else stockClass = 'out-of-stock';

                if (isFirstRow) {
                    row.innerHTML = `
                            <td rowspan="${totalVariants}" style="vertical-align: middle;"><img src="${product.productView.image}" alt="${product.productView.name}" class="product-item"></td>
                            <td rowspan="${totalVariants}" style="vertical-align: middle;">${product.productView.name}</td>
                            <td rowspan="${totalVariants}" style="vertical-align: middle;">Đôi</td>
                            <td>${detail.color.name}</td>
                            <td>${detail.size.name}</td>
                            <td><span class="stock-info ${stockClass}">${detail.stock}</span></td>
                            <td>${formatVND(detail.price)}</td>
                        `;
                    isFirstRow = false;
                }
                else {
                    row.innerHTML = `
                            <td>${detail.color.name}</td>
                            <td>${detail.size.name}</td>
                            <td><span class="stock-info ${stockClass}">${detail.stock}</span></td>
                            <td>${formatVND(detail.price)}</td>
                        `;
                }

                row.addEventListener('click', () => addProductToOrder(product, detail));
                productTableBody.appendChild(row);
        });
    });
}

async function addProductToOrder(product, detail) {
    if (detail.stock <= 0) {
        swal({title: "Thông báo",text: "Sản phẩm này đã hết hàng",type: "error"},function() {});
        return;
    }
    var selectInvoice = document.getElementById("select-invoice").value;
    if(selectInvoice == '-1'){
        swal({title: "Thông báo",text: "Chưa có đơn hàng chờ nao được chọn",type: "error"},function() {});
        return;
    }
    else{
        const response = await fetch(`/adm-prj/qtht-001/api/order-draft/add-to-order?detailId=${detail.id}&orderId=${selectInvoice}`, {
            method: 'POST'
        });
        if(response.status > 300){
            toastr.error("Mã lỗi: "+response.status);return;
        }
        var result = await response.json();
        if(result.success == true){
            toastr.success(result.message)
            var data = result.data;
            var orderDetail = data.detail
            showSingleProductItem(orderDetail);
            $("#totalAmount").val(data.total)
        }
        else{
            toastr.error(result.message)
        }
    }
}


async function loadDetailOrder() {
    var selectInvoice = document.getElementById("select-invoice").value;
    if(selectInvoice == '-1'){
        toastr.error("Chưa có đơn hàng chờ nao được chọn"); return;
    }
    orderItemsTableBody.innerHTML = '';
    const res = await fetch(`/adm-prj/qtht-001/api/order-draft/find-detail-order?orderId=${selectInvoice}`, {
        method: 'POST'
    });
    if(res.status > 300){
        toastr.error("Mã lỗi: "+res.status);return;
    }
    var result = await res.json();
    if(result.success == true){
        $('.btn-hidden').css('display', 'inline');
        $('.btn-luutam').css('display', 'none');
        var order = result.data;
        $("#id-order").val(order.id)
        document.getElementById('id-order').dataset.customerId = order.customerId || '';
        $("#fullName").val(order.fullName)
        $("#phone").val(order.phone)
        $("#email").val(order.email)
        $("#addressDetail").val(order.addressDetail)
        $("#province").val(order.provinceCode).change();
        $("#ward").val(order.wardCode).change();
        $("#shipcost").val(order.shipCost)
        $("#discount").val(order.discount)
        $("#totalAmount").val(order.totalAmount)
        $("#note").val(order.note)
        $("#discount-code").val(order.voucherCode)
    }
    else{
        toastr.error(result.message)
    }

    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/find-detail-by-order?orderId=${selectInvoice}`, {
        method: 'POST'
    });
    var list = await response.json();
    if (list.length === 0) {
        noOrderItem.style.display = 'block';
        return;
    }
    noOrderItem.style.display = 'none';
    for(i=0; i< list.length; i++){
        showSingleProductItem(list[i]);
    }
}

function showSingleProductItem(detail){
    noOrderItem.style.display = 'none';
    orderItemsTableBody.innerHTML += `<tr id="tr-product-${detail.id}">
        <td>${detail.sku}</td>
        <td>${detail.productName}</td>
        <td>${detail.colorName} / ${detail.sizeName}</td>
         <td><input oninput="changeQuantityProductTam(${detail.id}, ${detail.quantity}, this, ${detail.price})" type="number" value="${detail.quantity}" style="width: 50px"></td>
         <td>${formatVND(detail.price)}</td>
         <td id="tong-tien-tam-${detail.id}">${formatVND(detail.price * detail.quantity)}</td>
         <td><button onclick="deleteOrderDetailById(${detail.id})" class="btn btn-outline-danger btn-sm"><i class="fa fa-trash-alt"></i></button></td>
    </tr>`;
}


async function deleteOrderDetailById(id) {
    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/delete-detail?detailId=${id}`, {
        method: 'DELETE'
    });
    if(response.status > 300){
        toastr.error("Mã lỗi: "+response.status);return;
    }
    var result = await response.json();
    if(result.success == true){
        toastr.success(result.message)
        document.getElementById("tr-product-"+id).remove();
        noOrderItem.style.display = orderItemsTableBody.children.length ? 'none' : 'block';
        $("#totalAmount").val(result.data)
    }
    else{
        toastr.error(result.message)
    }
}


async function deleteOrder() {
    var selectInvoice = document.getElementById("select-invoice").value;
    if(selectInvoice == '-1'){
        toastr.error("Chưa có đơn hàng chờ nao được chọn"); return;
    }
    var con = confirm("Xác nhận xóa đơn chờ này");
    if(con == false){
        return;
    }
    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/delete-order?orderId=${selectInvoice}`, {
        method: 'DELETE'
    });
    if(response.status > 300){
        toastr.error("Mã lỗi: "+response.status);return;
    }
    var result = await response.json();
    if(result.success == true){
        toastr.success(result.message)
        window.location.reload();
    }
    else{
        toastr.error(result.message)
    }
}

async function changeQuantityProductTam(idDetail, oldQuantity, e, price){
    var quantity = e.value;
    var num = Number(quantity);
    if (!isNaN(num)) {
        if(num == 0){
            toastr.error("Số lượng phải lớn hơn 0");
            e.value = oldQuantity
            return;
        }
        const response = await fetch(`/adm-prj/qtht-001/api/order-draft/change-quantity?detailId=${idDetail}&quantity=${num}`, {
            method: 'POST'
        });
        if(response.status > 300){
            toastr.error("Mã lỗi: "+response.status);return;
        }
        var result = await response.json();
        if(result.success == true){
            toastr.success(result.message)
            $("#totalAmount").val(result.data)
            document.getElementById("tong-tien-tam-"+idDetail).innerHTML = formatVND(num * price)
        }
        else{
            toastr.error(result.message)
        }
    }
    else {
        return;
    }
}

function renderPagination(currentPage, totalPages) {
    let maxVisiblePages = 3; // số trang hiển thị chính giữa
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="loadProductInvoice(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="loadProductInvoice(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="loadProductInvoice(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }

    // dấu ...
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang cuối
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="loadProductInvoice(${totalPages - 1})"><a class="page-link">${totalPages}</a></li>`;
    }

    // nút next
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="loadProductInvoice(${currentPage + 1})"><a class="page-link">&raquo;</a></li>`;
    }

    document.getElementById("pageable").innerHTML = mainpage;
}

function formatVND(amount) {
    if (typeof amount !== "number") {
        amount = Number(amount) || 0; // đảm bảo là number
    }
    return amount.toLocaleString('vi-VN') + " đ";
}


async function changeOrderToInvoice() {
    var selectInvoice = document.getElementById("select-invoice").value;
    if(selectInvoice == '-1'){
        toastr.error("Chưa có đơn hàng chờ nao được chọn"); return;
    }
    var con = confirm("Xác nhận chuyển đơn chờ này thành đơn hàng");
    if(con == false){
        return;
    }
    if (!await createDonTam(true)) return;
    let status = $('input[name="invoiceStatus"]:checked').val();
    let orderType = $('input[name="orderType"]:checked').val();
    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/save-order-to-invoice?orderId=${selectInvoice}&invoiceType=${orderType}&invoiceStatus=${status}`, {
        method: 'POST'
    });
    if(response.status > 300){
        toastr.error("Mã lỗi: "+response.status);return;
    }
    var result = await response.json();
    if(result.success == true){
        swal({
            title: "Thông báo",
            text: result.message,
            type: "success"
        },
        function() {
            window.location.reload();
        });
    }
    else{
        toastr.error(result.message)
    }
}


async function deleteOrderFromBE(idorder) {
    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/delete-order?orderId=${idorder}`, {
        method: 'DELETE'
    });
}

async function createDonTam(silent = false){
    var orderDraft = {
        id: $("#id-order").val(),
        customerId: Number(document.getElementById('id-order').dataset.customerId) || null,
        fullName: document.getElementById("fullName").value == "" ? "Khách lẻ":$("#fullName").val(),
        phone: document.getElementById("phone").value,
        email: document.getElementById("email").value,
        note: document.getElementById("note").value,
        addressDetail: document.getElementById("addressDetail").value,
        wardCode: document.getElementById("ward").value,
        wardName: $("#ward option:selected").data("ward-name"),
        provinceCode: document.getElementById("province").value,
        provinceName: $("#province option:selected").data("province-name"),
        totalAmount: parseFloat(document.getElementById("totalAmount").value) || 0,
        discount: parseFloat(document.getElementById("discount").value) || 0,
        voucherCode: document.getElementById("discount-code").value,
        shipCost: parseFloat(document.getElementById("shipcost").value) || 0,
    };
    const response = await fetch(`/adm-prj/qtht-001/api/order-draft/create`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(orderDraft)
    });
    if(response.status > 300){
        toastr.error("Mã lỗi: "+response.status);return;
    }
    var result = await response.json();
    if(result.success == true){
        if (!silent) swal({
            title: "Thông báo",
            text: result.message,
            type: "success"
        },
        function() {
        });
        var order = result.data;
        if(order != null){
            const select = document.getElementById('select-invoice');
            const option = new Option(`${order.id} - ${order.fullName || 'Khách lẻ'} - ${order.phone || ''}`, String(order.id));
            select.append(option);
            select.value = String(order.id);
            select.dispatchEvent(new Event('change'));
        }
    }
    else{
        toastr.error(result.message)
    }
    return result.success === true;
}

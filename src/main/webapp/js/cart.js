async function addCart(){
    if(selectedDetailId == null){
        toastr.warning("Hãy chọn màu sắc và kích thước sản phẩm");
        return false;
    }
    var payload = {
        id: selectedDetailId,
        quantity: $("#inputquantity").val()
    }
    if(payload.quantity < 1){
        toastr.error("Số lượng phải lớn hơn 0");
        return false;
    }
    const response = await fetch("/api/v1/cart/add", {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(payload)
    });
    if (response.status < 300) {
        var result = await response.json();
        if(result.statusCode == 200){
            document.getElementById("cart-quantity").innerText = result.data
        }
        if(result.message != null && result.message != ''){
            if(result.statusCode == 200){
                swal({
                    title: "Thông báo",
                    text: result.message,
                    type: "success"
                },
                function() {
                });
            }
            else{
                toastr.error(result.message)
            }
        }
        return true;
    }
    else {
        toastr.error("Đã có lỗi xảy ra");
        return false;
    }
}

async function buyNow(){
    var check = await addCart();
    if(check == false){
        return;
    }
    window.location.href = `/checkout?detail=`+selectedDetailId
}

$(document).ready(function () {
    $(document).on('click', '.delete-cart-item', function() {
        let delId = $(this).data('del');
        deleteCart(delId)
    });
    $(document).on('click', '.change-quantity-cart', function() {
        let detail = $(this).data('detail');
        let quantity = $(this).data('quantity');
        changeQuantityCart(detail, quantity)
    });
});


async function deleteCart(dataId){
    const response = await fetch(`/api/v1/cart/delete/${dataId}`, {
        method: 'DELETE'
    });
    if (response.status < 300) {
        var result = await response.json();
        var message = result.message;
        var data = result.data;
        console.log(data)
        if(result.success == true){
            swal({
                title: "Thông báo",
                text: message,
                type: "success"
            },
            function() {
                if(data.size > 0){
                    $('.product-item-cart-'+dataId).remove();
                    document.getElementById("tonggiatien").innerText = formatVND(data.total);
                    document.getElementById("slcart").innerText = data.size;
                }
                else{
                    window.location.reload();
                }
            });
        }
        else{
            toastr.error(message);
        }
    }
    else{
        toastr.error("Đã có lỗi xảy ra");
        return;
    }
}

function changeQuantity(type){
    var inputQuantity = document.getElementById("inputquantity");
    var currentValue = parseInt(inputQuantity.value) || 1;

    if(type === 'UP'){
        inputQuantity.value = currentValue + 1;
    }
    else if(type === 'DOWN'){
        if(currentValue > 1){
            inputQuantity.value = currentValue - 1;
        }
    }
}


function formatVND(amount) {
    if (typeof amount !== "number") {
        amount = Number(amount) || 0; // đảm bảo là number
    }
    return amount.toLocaleString('vi-VN') + " đ";
}

async function changeQuantityCart(dataId, quantity){
    const response = await fetch(`/api/v1/cart/update-quantity/${dataId}?quantity=${quantity}`, {
        method: 'POST'
    });
    if (response.status < 300) {
        var result = await response.json();
        var message = result.message;
        var data = result.data;
        console.log(data)
        if(result.success == true){
            document.getElementById("tonggiatien").innerText = formatVND(data);
            $('.input-quantity-cart-'+dataId).val(Number($('.input-quantity-cart-'+dataId).val()) + Number(quantity));
            $('.total-price-item-' + dataId).text(
                formatVND(Number($('.total-price-item-' + dataId).data('price')) *
                    Number($('.input-quantity-cart-' + dataId).val()))
            );
        }
        else{
            toastr.error(message);
        }
    }
    else{
        toastr.error("Đã có lỗi xảy ra");
        return;
    }
}


document.querySelectorAll(".btnthanhtoan").forEach(button => button.addEventListener("click", function () {
    let checked = document.querySelectorAll("input[type='checkbox']:checked");
    let ids = [];
    checked.forEach(cb => ids.push(cb.value));
    let uniqueIds = [...new Set(ids)];
    if (uniqueIds.length === 0) {
        swal("Thông báo", "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", "warning");
        return;
    }
    let queryString = uniqueIds.map(id => "detail=" + encodeURIComponent(id)).join("&");

    window.location.href = "/checkout?" + queryString;
}));


document.addEventListener("change", function (e) {
    if (e.target && e.target.type === "checkbox" && e.target.value) {
        let dataId = e.target.value;
        let isChecked = e.target.checked;

        // Tìm tất cả checkbox có cùng dataId
        let checkboxes = document.querySelectorAll("input[type='checkbox'][value='" + dataId + "']");

        checkboxes.forEach(cb => {
            if (cb !== e.target) {
                cb.checked = isChecked;
            }
        });
    }
});

async function filterDiscount(page) {
    var size = document.getElementById("showEntries").value
    var sort = document.getElementById("sortby").value

    var dateRange = $('#dateRange').val();
    var startDate = null;
    var endDate = null;
    if(dateRange != null && dateRange != ""){
        var picker = $('#dateRange').data('daterangepicker');
        startDate = picker.startDate.format('YYYY-MM-DDTHH:mm:ss');
        endDate = picker.endDate.format('YYYY-MM-DDTHH:mm:ss');
    }
    var url = `/adm-prj/qtht-001/api/v1/discount-product/admin/get-all?page=${page}&size=${size}&sort=${sort}`
    if(startDate != null && endDate != null){
        url += `&fromDate=${startDate}&toDate=${endDate}`
    }
    if(document.getElementById("loaigiamgia").value != ''){
        url += `&type=${document.getElementById("loaigiamgia").value}`
    }
    const response = await fetch(url, {
        method: 'POST',
    });
    var result = await response.json();
    var totalPage = result.totalPages;
    document.getElementById("timthay").innerHTML = result.totalElements
    document.getElementById("hienthi").innerHTML = result.numberOfElements
    var list = result.content
    var main = '';
    for (let i = 0; i < list.length; i++) {

        const d = list[i];

        let typeBadge = "";
        let valueHtml = "";

        if (d.discountProductType === "PERCENT") {
            typeBadge = `<span class="badge bg-success">% Sale</span>`;
            valueHtml = `<span class="discount-value text-success">${d.discountValue}%</span>`;
        } else {
            typeBadge = `<span class="badge bg-primary">VNĐ</span>`;
            valueHtml = `<span class="discount-value text-danger">${formatMoney(d.discountValue)}</span>`;
        }

        const now = new Date();
        const start = new Date(d.startDate);
        const end = new Date(d.endDate);

        let status = "";

        if (now < start) {
            status = `<span class="badge bg-warning status-badge">Sắp diễn ra</span>`;
        } else if (now > end) {
            status = `<span class="badge bg-secondary status-badge">Hết hạn</span>`;
        } else {
            status = `<span class="badge bg-success status-badge">Đang giảm</span>`;
        }

        main += `
    <tr class="fade-row" style="animation-delay:${i*0.05}s">
    
        <td><b>#${d.id}</b></td>

        <td>
            <div class="product-name">
                <i class="fa fa-tag text-primary"></i>
                ${d.product?.name || ""}
            </div>
        </td>

        <td class="text-muted">
            <i class="fa fa-clock"></i>
            ${formatDate(d.createdDate)}
        </td>

        <td>
            <span class="badge bg-light text-dark">
                <i class="fa fa-user"></i>
                ${d.user?.fullName || ""}
            </span>
        </td>

        <td class="text-center">
            ${typeBadge}
        </td>

        <td class="text-center">
            ${valueHtml}
        </td>

        <td class="text-center">
            ${d.maxDiscountValue ? formatMoney(d.maxDiscountValue) : "-"}
        </td>

        <td class="time-box">
            <div>
                <i class="fa fa-calendar text-success"></i>
                ${formatDate(d.startDate)}
            </div>
            <div class="text-muted">
                → ${formatDate(d.endDate)}
            </div>
            ${status}
        </td>

        <td class="sticky-col text-center">

            <button class="btn btn-sm btn-outline-primary"
                onclick="openUpdateDiscount(${d.id})">
                <i class="fa fa-edit"></i>
            </button>

            <button class="btn btn-sm btn-outline-danger"
                onclick="deleteDiscount(${d.id})">
                <i class="fa fa-trash"></i>
            </button>

        </td>
    </tr>
    `;
    }

    document.getElementById("listDiscountProduct").innerHTML = main;
    renderPagination(page, totalPage);
}

function formatMoney(value){
    if(!value) return "0đ";
    return value.toLocaleString('vi-VN') + "đ";
}

function formatDate(date){
    if(!date) return "";

    const d = new Date(date);

    return d.toLocaleString("vi-VN");
}


function renderPagination(currentPage, totalPages) {
    let maxVisiblePages = 3; // số trang hiển thị chính giữa
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="filterDiscount(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="filterDiscount(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="filterDiscount(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }

    // dấu ...
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang cuối
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="filterBlog(${totalPages - 1})"><a class="page-link">${totalPages}</a></li>`;
    }

    // nút next
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="filterBlog(${currentPage + 1})"><a class="page-link">&raquo;</a></li>`;
    }

    document.getElementById("pageable").innerHTML = mainpage;
}

async function loadProduct(){
    var categories = $("#danhmucselect").val();
    const response = await fetch(`/adm-prj/qtht-001/api/product/all-product-admin`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(categories)
    });
    var list = await response.json();
    var main = '';
    for(i=0; i<list.length; i++){
        main += `<option value="${list[i].id}" selected>${list[i].name}</option>`
    }
    document.getElementById("sanphamselect").innerHTML = main;
    $('#sanphamselect').select2({
        theme: "bootstrap-5",
        width: '100%',
    });
}


async function saveDiscount(){
    var listProductId = $("#sanphamselect").val();
    if(listProductId.length == 0){
        swal('Thông báo', 'Hãy chọn ít nhất 1 sản phẩm','error')
        return;
    }
    var dateRange = $('#dateRange').val();
    if(!dateRange){
        swal('Thông báo', 'Hãy chọn thời gian giảm giá','error')
        return;
    }
    var picker = $('#dateRange').data('daterangepicker');

    // convert sang LocalDateTime format cho Spring Boot
    var startDate = picker.startDate.format('YYYY-MM-DDTHH:mm:ss');
    var endDate = picker.endDate.format('YYYY-MM-DDTHH:mm:ss');

    var type = document.getElementById("loaigiamgia").value;
    var discountValue = Number(document.getElementById("giatrigiam").value);
    var maxDiscount = Number(document.getElementById("giamtoida").value);
    if(discountValue <= 0){
        swal('Thông báo', 'Giá trị giảm phải > 0','error')
        return;
    }
    if(type === 'PERCENT'){
        if(discountValue > 100){
            swal('Thông báo', 'Giá trị giảm không được > 100%','error')
            return;
        }

        if(!maxDiscount || maxDiscount <= 0){
            swal('Thông báo', 'Bạn phải nhập giá trị giảm tối đa','error')
            return;
        }
    }

    var payload = [];

    listProductId.forEach(id => {
        payload.push({
            productId: id,
            startDate: startDate,
            endDate: endDate,
            type: type,
            discountValue: discountValue,
            maxDiscountValue: type === 'FIXED' ? discountValue : maxDiscount
        });
    });

    const response = await fetch(`/adm-prj/qtht-001/api/v1/discount-product/admin/save`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(payload)
    });
    var result = await response.json();
    if(response.status < 300){
        if(Number(result.fail) > 0){
            showDiscountResult(result);
        }
        else{
            swal({
                title: "Thông báo",
                text: "thêm giảm giá sản phẩm thành công!",
                type: "success"
            },
            function() {
                window.location.replace('list')
            });
        }
    }
    else{
        swal('Thông báo', 'Có lỗi xảy ra khi lưu','error')
    }
}

function showDiscountResult(data){

    document.getElementById("discountResult").classList.remove("hidden");

    document.getElementById("successCount").innerText = data.success;
    document.getElementById("failCount").innerText = data.fail;

    if(data.fail > 0){

        let ul = document.getElementById("failProducts");
        ul.innerHTML = "";

        data.failProducts.forEach(p=>{
            ul.innerHTML += `<li>${p}</li>`;
        });

        document.getElementById("failListBox").classList.remove("hidden");
    }
}

function closeResult(){
    document.getElementById("discountResult").classList.add("hidden");
}

async function openUpdateDiscount(id){

    const response = await fetch(`/adm-prj/qtht-001/api/v1/discount-product/admin/${id}`);
    const d = await response.json();

    document.getElementById("updateDiscountId").value = d.id;
    document.getElementById("updateType").value = d.discountProductType;
    document.getElementById("updateValue").value = d.discountValue;
    document.getElementById("updateMaxValue").value = d.maxDiscountValue || "";

    document.getElementById("updateStartDate").value = formatInputDate(d.startDate);
    document.getElementById("updateEndDate").value = formatInputDate(d.endDate);

    toggleMaxDiscount();

    new bootstrap.Modal(document.getElementById("updateDiscountModal")).show();
}

function toggleMaxDiscount(){

    const type = document.getElementById("updateType").value;
    const maxGroup = document.getElementById("maxDiscountGroup");

    if(type === "PERCENT"){
        maxGroup.style.display = "block";
    }else{
        maxGroup.style.display = "none";

        // tự động set max = value
        const value = document.getElementById("updateValue").value;
        document.getElementById("updateMaxValue").value = value;
    }

}

async function updateDiscount(){

    const id = document.getElementById("updateDiscountId").value;
    const type = document.getElementById("updateType").value;

    let discountValue = parseFloat(document.getElementById("updateValue").value);
    let maxDiscountValue = parseFloat(document.getElementById("updateMaxValue").value);

    const startDate = document.getElementById("updateStartDate").value;
    const endDate = document.getElementById("updateEndDate").value;

    // validate giá trị giảm
    if(!discountValue || discountValue <= 0){
        swal('Thông báo',"Giá trị giảm phải lớn hơn 0",'error');
        return;
    }

    // validate percent
    if(type === "PERCENT" && discountValue > 100){
        swal('Thông báo',"Giảm phần trăm không được lớn hơn 100%",'error');
        return;
    }

    // validate ngày
    if(startDate >= endDate){
        swal('Thông báo',"Ngày bắt đầu phải trước ngày kết thúc",'error');
        return;
    }

    // nếu FIXED -> maxDiscountValue = discountValue
    if(type === "FIXED"){
        maxDiscountValue = discountValue;
    }

    // nếu PERCENT -> phải nhập max
    if(type === "PERCENT" && (!maxDiscountValue || maxDiscountValue <= 0)){
        swal('Thông báo',"Vui lòng nhập giảm tối đa",'error');
        return;
    }

    const data = {
        discountProductType: type,
        discountValue: discountValue,
        maxDiscountValue: type == 'PERCENT'?maxDiscountValue:discountValue,
        startDate: startDate,
        endDate: endDate
    };
    console.log(data)
    const response = await fetch(`/adm-prj/qtht-001/api/v1/discount-product/admin/update/${id}`,{
        method:"PUT",
        headers:{
            "Content-Type":"application/json"
        },
        body: JSON.stringify(data)
    });

    if(response.ok){
        swal('Thông báo',"Cập nhật thành công",'success');
        filterDiscount(0)
        $("#updateDiscountModal").modal('hide')
    }
    else{
        swal('Thông báo',"Cập nhật thất bại",'error');
    }
}

function formatInputDate(date){

    const d = new Date(date);

    const year = d.getFullYear();
    const month = String(d.getMonth()+1).padStart(2,'0');
    const day = String(d.getDate()).padStart(2,'0');
    const hour = String(d.getHours()).padStart(2,'0');
    const minute = String(d.getMinutes()).padStart(2,'0');

    return `${year}-${month}-${day}T${hour}:${minute}`;
}

async function deleteDiscount(id, btn) {
    // var con = confirm("Xác nhận xóa bài viết này?")
    // if (con == false) {
    //     return;
    // }
    // const response = await fetch(`/adm-prj/qtht-001/api/v1/discount-product/delete?id=${id}`, {
    //     method: 'DELETE'
    // });
    // if (response.status < 300) {
    //     toastr.success("xóa bài viết thành công!");
    //     filterDiscount(0)
    // }
    // else {
    //     if(response.status == exceptionCode){
    //         var result = await response.json();
    //         toastr.error(result.defaultMessage);
    //     }
    //     else{
    //         toastr.error("Xóa thất bại, lỗi: "+response.status);
    //     }
    // }

    swal({
        title: "Bạn có chắc muốn xóa khuyến mại này?",
        text: "Sau khi xóa sẽ không thể khôi phục lại được!",
        type: "warning",
        showCancelButton: true,
        confirmButtonColor: "#DD6B55",
        confirmButtonText: "Có, tôi muốn xóa!",
        cancelButtonText: "Không, giữ lại!",
        closeOnConfirm: false,
        closeOnCancel: true
    },
    async function(isConfirm){
        if (isConfirm) {
            const response = await fetch(`/adm-prj/qtht-001/api/v1/discount-product/delete?id=${id}`, {
                method: 'DELETE'
            });
            if (response.status < 300) {
                swal("Đã xóa!", "Khuyến mại đã được xóa thành công.", "success");
                filterDiscount(0)
            }
            else {
                if(response.status == 417){
                    var result = await response.json();
                    toastr.error(result.defaultMessage);
                }
                else{
                    // toastr.error("Xóa thất bại, lỗi: "+response.status);
                    swal("Lỗi!", "Xóa thất bại. Vui lòng thử lại.", "error");
                }
            }
        }
    });
}
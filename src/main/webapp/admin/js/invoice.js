async function filterInvoice(page) {
    var dto = {
        "invoiceType": document.getElementById('invoiceTypeFilter')?.value || null,
        "invoiceStatus": $(".active-status-list").attr("data-status") == ""?null:$(".active-status-list").attr("data-status"),
        "from": $("#fromDate").val(),
        "to": $("#toDate").val(),
        "search": $("#search").val(),
    }
    var size = document.getElementById("showEntries").value
    var sort = document.getElementById("sortby").value
    const response = await fetch(`/adm-prj/qtht-001/api/invoice/filter?page=${page}&size=${size}&sort=${sort}`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(dto)
    });
    var result = await response.json();
    console.log(result)
    var totalPage = result.totalPages;
    document.getElementById("timthay").innerHTML = result.totalElements
    document.getElementById("hienthi").innerHTML = result.numberOfElements
    var list = result.content
    var main = '';
    for (i = 0; i < list.length; i++) {
        main += `<tr id="invoice-tr-${list[i].id}">
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <input type="checkbox">
                            <span>${page * size + i + 1}</span>
                        </div>
                    </td>
                    <td>
                        <div class="d-flex flex-column order-details">
                            <span class="main-id">${list[i].code}</span>
                            <span class="status">${list[i].labelStatus}</span>
                            <a href="https://donhang.ghn.vn/?order_code=${list[i].trackingCode}" class="main-id">Vận đơn: ${list[i].trackingCode}</a>
                        </div>
                    </td>
                    <td>
                        <div class="d-flex flex-column receiver-details">
                            <span>${list[i].fullName}</span>
                            <span>Số điện thoại: <a href="tel:${list[i].phone}">${formatPhone(list[i].phone)}</a></span>
                            <span>Email: <a href="mailto:${list[i].email}">${list[i].email}</a></span>
                            <span class="status">Ngày tạo: ${list[i].createdDate}</span>
                            <span class="status">${list[i].addressDetail}, ${list[i].wardName},${list[i].districtName == null?'':list[i].districtName+', '} ${list[i].provinceName}</span>
                            <span>Ghi chú: <strong class="status">${list[i].note}</strong></span>
                        </div>
                    </td>
                    <td>
                        <div class="d-flex flex-column">
                            <span>${list[i].payType}</span>
                            <span>Tạm tính: <strong>${formatmoney(list[i].totalAmount)}</strong></span>
                            <span>Phí ship: ${formatmoney(list[i].shipCost)}</span>
                            <span class="cod-amount">Giảm giá: ${formatmoney(list[i].discount)}</span>
                            <h3 class="total-cost-invoice badge bg-success text-white">Tổng thanh toán: ${formatmoney(Number(list[i].totalAmount) - Number(list[i].discount) + Number(list[i].shipCost))}</h3>
                        </div>
                    </td>
                    <td>${list[i].weight} KG</td>
                    <td>
                        ${list[i].invoiceTypeLabel}
                    </td>
                    <td>
                        <a onclick="getHistoryStatus(${list[i].id})" data-bs-toggle="modal" data-bs-target="#lichSuModal" class="btn btn-outline-primary btn-sm"><i class="fa fa-clock"></i><br></a>
                        <a href="detail?id=${list[i].id}" target="_blank" class="btn btn-outline-secondary btn-sm"><i class="fa fa-eye"></i></a>
                        ${list[i].trackingCode == null?`<button onclick="vanDon()" title="Tạo vận đơn" class="btn btn-outline-dark btn-sm"><i class="fa fa-truck"></i></button>`:''}
                    </td>
                </tr>`
    }
    document.getElementById("list-invoice").innerHTML = main

    renderPagination(page, totalPage);
    if(list.length == 0){
        document.getElementById("list-invoice").innerHTML = `<tr>
            <td colspan="7">
                <div class="empty-table-message">
                    <p>Không có đơn hàng nào được tìm thấy.</p>
                    <a href="#" class="btn btn-primary">Tạo đơn hàng</a>
                </div>
            </td>
        </tr>`
    }
}

function vanDon(){
    toastr.error("Chức năng này chưa được hỗ trợ");
    return
}

function filterNotSearch(){
    filterInvoice(0)
}

function resetInvoiceFilters() {
    for (const id of ['search', 'fromDate', 'toDate', 'invoiceTypeFilter']) {
        const input = document.getElementById(id);
        if (input) { input.value = ''; if (input._flatpickr) input._flatpickr.clear(false); }
    }
    document.querySelectorAll('.btn-status-list').forEach(button => button.classList.remove('active-status-list'));
    const all = document.querySelector('.btn-status-list[data-stt=""]');
    if (all) { all.classList.add('active-status-list'); all.dataset.status = ''; }
    filterInvoice(0);
}

async function getHistoryStatus(id){
    const response = await fetch(`/adm-prj/qtht-001/api/invoice/history?id=${id}`, {
        method: 'POST'
    });
    if(response.status > 300){
        toastr.error("Đã có lỗi xảy ra"); return;
    }
    var result = await response.json();
    document.getElementById("ma-code-donhang").innerText = result.invoice.code
    var main = '';
    for (let i = 0; i < result.statuses.length; i++) {
        main += `
        <button class='btn status-update-btn ${result.statuses[i].active == true ?'status-update-btn-disable':''}' ${result.statuses[i].active == true?'disabled':''}
                onclick="updateStatus(this)"
                style="border: 1px solid ${result.statuses[i].color}; color: ${result.statuses[i].color}"
                data-status="${result.statuses[i].invoiceStatus}" data-invoice="${id}" data-label=" ${result.statuses[i].label}" data-inv-code="${result.invoice.code}"
                >
            <i class="${result.statuses[i].icon}"></i> ${result.statuses[i].label}
        </button>
    `;
    }
    document.getElementById("list-status-update").innerHTML = main

    var mainStt = '';
    for (let i = 0; i < result.historyInvoiceStatuses.length; i++) {
        mainStt += `
        <tr>
            <td data-label="Thời gian">${result.historyInvoiceStatuses[i].createdDate}</td>
            <td data-label="Trạng thái">
                <span style="color: ${result.historyInvoiceStatuses[i].color}">${result.historyInvoiceStatuses[i].label}</span>
            </td>
            <td data-label="Người cập nhật">${result.historyInvoiceStatuses[i].userId == null?'':'User: '+result.historyInvoiceStatuses[i].userId+'<br>'} 
            ${result.historyInvoiceStatuses[i].fullName == null?'':result.historyInvoiceStatuses[i].fullName}</td>
            <td data-label="Lý do">${result.historyInvoiceStatuses[i].note == null?'':result.historyInvoiceStatuses[i].note}</td>
        </tr>
        `;
    }
    document.getElementById("list-history").innerHTML = mainStt
}

async function updateStatus(e){
    var stt = e.getAttribute("data-status")
    var name = e.getAttribute("data-label")
    var invCode = e.getAttribute("data-inv-code")
    var id = e.getAttribute("data-invoice")
    swal({
        title: "Thông báo",
        text: "Xác nhận chuyển đổi đơn hàng "+invCode+" sang trạng thái "+name,
        type: "info",
        showCancelButton: true,
        confirmButtonText: "OK",
        cancelButtonText: "Hủy"
    }, function(isConfirm) {
        if (isConfirm) {
            actionChangeStatus(stt, name, invCode, id)
        }
    });
}

async function actionChangeStatus(stt, name, invCode, id){
    const response = await fetch(`/adm-prj/qtht-001/api/invoice/change-status?id=${id}&invoiceStatus=${stt}`, {
        method: 'POST'
    });
    if(response.status > 300){
        toastr.error("Đã có lỗi xảy ra"); return;
    }
    var result = await response.json();
    if(result.success == false){
        toastr.error(result.message);
    }
    else{
        getHistoryStatus(id);
        toastr.success("Cập nhật trạng thái đơn hàng thành công");
        document.getElementById("invoice-tr-"+id).remove()
    }
}



function renderPagination(currentPage, totalPages) {
    let maxVisiblePages = 3; // số trang hiển thị chính giữa
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="filterInvoice(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="filterInvoice(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="filterInvoice(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }

    // dấu ...
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang cuối
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="filterInvoice(${totalPages - 1})"><a class="page-link">${totalPages}</a></li>`;
    }

    // nút next
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="filterInvoice(${currentPage + 1})"><a class="page-link">&raquo;</a></li>`;
    }

    document.getElementById("pageable").innerHTML = mainpage;
}

function formatPhone(phone) {
    if (!phone || phone.length !== 10) return phone; // giữ nguyên nếu không đúng 10 số
    return phone.slice(0, 4) + '.' + phone.slice(4, 7) + '.' + phone.slice(7);
}

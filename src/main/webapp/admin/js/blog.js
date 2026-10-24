async function filterBlog(page) {
    let range = document.getElementById("datepicker").value;
    var from = null;
    var to = null;
    if(range != ""){
        from = range.split(" đến ")[0]
        to = range.split(" đến ")[1]
    }
    var searchDto = {
        "from": from,
        "to": to,
        "search": document.getElementById("search").value == ''?null:document.getElementById("search").value,
        "archived": document.getElementById('blogArchiveFilter')?.value === '' ? null : document.getElementById('blogArchiveFilter')?.value === 'true',
    }
    var size = document.getElementById("showEntries").value
    var sort = document.getElementById("sortby").value
    const response = await fetch(`/adm-prj/qtht-001/api/v1/blog/get-all-by-admin?page=${page}&size=${size}&sort=${sort}`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(searchDto)
    });
    var result = await response.json();
    var totalPage = result.totalPages;
    document.getElementById("timthay").innerHTML = result.totalElements
    document.getElementById("hienthi").innerHTML = result.numberOfElements
    var list = result.content
    var main = '';
    for (i = 0; i < list.length; i++) {
        main += `<tr>
                    <td>#${list[i].id}</td>
                    <td><img src="${list[i].imageBanner}" style="width: 100px;"></td>
                    <td>${list[i].title}</td>
                    <td>${list[i].createdDate}</td>
                    <td>${list[i].updateDate}</td>
                    <td>${list[i].user == null?'':list[i].user.fullName}</td>
                    <td>${list[i].numView}</td>
                     <td class="sticky-col d-flex">
                        <button onclick="deleteBlog(${list[i].id}, this)" class="btn btn-outline-danger btn-sm"><i class="fa fa-trash-alt"></i></button>
                        <a href="save?id=${list[i].id}"  class="btn btn-outline-primary btn-sm"><i class="fa fa-edit"></i><br></a>
                        <button id="btnviewdetail${list[i].id}" data-bs-toggle="modal" data-bs-target="#modalDetail" class="btn btn-outline-secondary btn-sm"><i class="fa fa-eye"></i></button>
                    </td>
                </tr>`
    }
    document.getElementById("listBlog").innerHTML = main
    for (let j = 0; j < list.length; j++) {
        document.getElementById("btnviewdetail" + list[j].id).addEventListener("click", function () {
            document.getElementById("tieudetintuc").innerHTML = list[j].title
            document.getElementById("matin").innerHTML = list[j].id
            document.getElementById("anhtin").src = list[j].imageBanner
            document.getElementById("ngaytao").innerHTML = list[j].createdDate
            document.getElementById("ngaysua").innerHTML = list[j].updateDate
            document.getElementById("nguoitao").innerHTML = list[j].user == null?'':list[j].user.fullName
            document.getElementById("seotitle").innerHTML = list[j].seoTitle
            document.getElementById("seodescription").innerHTML = list[j].metaDescription
            document.getElementById("noidungtin").innerHTML = list[j].content
        });
    }
    renderPagination(page, totalPage);
}


function renderPagination(currentPage, totalPages) {
    let maxVisiblePages = 3; // số trang hiển thị chính giữa
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="filterBlog(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="filterBlog(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="filterBlog(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
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

async function deleteBlog(id, btn) {
    var con = confirm("Xác nhận xóa bài viết này?")
    if (con == false) {
        return;
    }
    const response = await fetch(`/adm-prj/qtht-001/api/v1/blog/delete?id=${id}`, {
        method: 'DELETE'
    });
    if (response.status < 300) {
        toastr.success("xóa bài viết thành công!");
        const parentDiv = btn.parentElement.parentElement;
        if (parentDiv) {
            parentDiv.remove();
        }
    }
    else {
        if(response.status == exceptionCode){
            var result = await response.json();
            toastr.error(result.defaultMessage);
        }
        else{
            toastr.error("Xóa thất bại, lỗi: "+response.status);
        }
    }
}

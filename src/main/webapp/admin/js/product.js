var listFile = [];
var exceptionCode = 417;
async function filterProduct(page) {
    var productsearch = {
        "brandId": document.getElementById('filterBrand')?.value || null,
        "idCategory": document.getElementById("listcategory").value == -1?null:document.getElementById("listcategory").value,
        "smallPrice": document.getElementById("priceRange").value.split("-")[0],
        "largePrice": document.getElementById("priceRange").value.split("-")[1],
        "active": document.querySelector('input[name="statusFilter"]:checked').value,
        "search": document.getElementById("search").value == ''?null:document.getElementById("search").value,
    }
    var size = document.getElementById("showEntries").value
    var sort = document.getElementById("sortby").value
    const response = await fetch(`/adm-prj/qtht-001/api/product/filter?page=${page}&size=${size}&sort=${sort}`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(productsearch)
    });
    var result = await response.json();
    var totalPage = result.totalPages;
    document.getElementById("timthay").innerHTML = result.totalElements
    document.getElementById("hienthi").innerHTML = result.numberOfElements
    var list = result.content
    var main = '';
    for (i = 0; i < list.length; i++) {
        var listdm = '<div class="listtag">'
        for (j = 0; j < list[i].productCategories.length; j++) {
            listdm += `<a href="" class="tagcauhoi">.${list[i].productCategories[j].category.name}</a>`;
        }
        listdm += '</div>';
        main += `<tr class="${list[i].active == true?'':'tr-locked'}">
                    <td>${page * size + i + 1}</td>
                    <td>#${list[i].id}</td>
                    <td><img src="${list[i].image}" style="width: 100px;"></td>
                    <td>${listdm}</td>
                    <td>${list[i].name}</td>
                    <td>${formatmoney(list[i].price)}</td>
                     <td>${list[i].createdAt}</td>
                     <td>${list[i].stock}</td>
                     <td>${list[i].sold}</td>
                     <td><span class="badge ${list[i].active == true?'bg-success':'bg-danger'}">${list[i].active == true?`Đang hoạt động`:`Đã khóa`}</span></td>
                      <td class="sticky-col d-flex">
                        <button onclick="viewDetailPro(${list[i].id})" class="btn btn-outline-secondary btn-sm"><i class="fa fa-eye"></i></button>
                        <a href="/adm-prj/qtht-001/product/save?id=${list[i].id}" class="btn btn-sm btn-primary"><i class="fa fa-edit"></i></a>
                        <button onclick="deleteProduct(${list[i].id})" class="btn btn-outline-danger btn-sm"><i class="fa fa-trash-alt"></i></button>
                    </td>
                </tr>`
    }
    document.getElementById("listproduct").innerHTML = main

    renderPagination(page, totalPage);
}


function renderPagination(currentPage, totalPages) {
    let maxVisiblePages = 3; // số trang hiển thị chính giữa
    let mainpage = '';

    // nút prev
    if (currentPage > 0) {
        mainpage += `<li class="page-item pointer" onclick="filterProduct(${currentPage - 1})"><a class="page-link">&laquo;</a></li>`;
    }

    // trang đầu
    if (currentPage > 1) {
        mainpage += `<li class="page-item pointer" onclick="filterProduct(0)"><a class="page-link">1</a></li>`;
    }

    // dấu ...
    if (currentPage > 2) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang ở giữa
    for (let i = currentPage - 1; i <= currentPage + 1; i++) {
        if (i >= 0 && i < totalPages) {
            mainpage += `<li onclick="filterProduct(${i})" class="page-item pointer ${i === currentPage ? 'active' : ''}"><a class="page-link">${i + 1}</a></li>`;
        }
    }

    // dấu ...
    if (currentPage < totalPages - 3) {
        mainpage += `<li class="page-item disabled"><a class="page-link">...</a></li>`;
    }

    // trang cuối
    if (currentPage < totalPages - 2) {
        mainpage += `<li class="page-item pointer" onclick="filterProduct(${totalPages - 1})"><a class="page-link">${totalPages}</a></li>`;
    }

    // nút next
    if (currentPage < totalPages - 1) {
        mainpage += `<li class="page-item pointer" onclick="filterProduct(${currentPage + 1})"><a class="page-link">&raquo;</a></li>`;
    }

    document.getElementById("pageable").innerHTML = mainpage;
}

var linkbanner = document.getElementById("productimage")?.value || '';
async function saveProduct() {
    var uls = new URL(document.URL)
    var id = uls.searchParams.get("id");
    var url = '/adm-prj/qtht-001/api/product/save';
    if (id != null) {
        url = `/adm-prj/qtht-001/api/product/update?id=${id}`;
    }

    document.getElementById("loading").style.display = 'block'
    await uploadFile(document.getElementById("anhdaidien"));
    var listLinkImg = await uploadMultipleFileNotResp();
    var listDetail = await getProductDetailsFromUI();
    var product = {
        "name": document.getElementById("name").value,
        "brandId": document.getElementById('brandId')?.value || null,
        "soleId": document.getElementById('soleId')?.value || null,
        "materialId": document.getElementById('materialId')?.value || null,
        "slug": document.getElementById("slug").value,
        "image": linkbanner,
        "price": document.getElementById("giaban").value,
        "oldPrice": document.getElementById("giacu").value,
        "description": tinyMCE.get('editor').getContent(),
        "metaTitle": document.getElementById("metaTitle").value,
        "metaDescription": document.getElementById("metaDescription").value,
        "weight": document.getElementById("weight").value,
        "categoryIds": $("#categoryIds").val(),
        "active": document.getElementById("activeproduct").checked,
        "imageLinks": listLinkImg,
        "details": listDetail
    }
    console.log(product)
    const response = await fetch(url, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(product)
    });
    var result = await response.json();

    if (response.status < 300) {
        swal({ title: "Thông báo", text: "thêm/sửa sản phẩm thành công", type: "success" },
            function() { window.location.href = 'list' });
    } else {
        swal({title: "Thông báo",text: "thêm/sửa sản phẩm thất bại",type: "error"},
            function() { document.getElementById("loading").style.display = 'none' });
    }
}

async function uploadMultipleFileNotResp() {
    const formData = new FormData()
    for (i = 0; i < listFile.length; i++) {
        formData.append("file", listFile[i])
    }
    var urlUpload = '/adm-prj/qtht-001/api/upload/upload-multiple-file';
    const res = await fetch(urlUpload, {
        method: 'POST',
        body: formData
    });
    if (res.status < 300) {
        return await res.json();
    } else {
        return [];
    }
}


async function uploadFile(filePath) {
    const formData = new FormData()
    formData.append("file", filePath.files[0])
    var urlUpload = '/adm-prj/qtht-001/api/upload/upload-file';
    const res = await fetch(urlUpload, {
        method: 'POST',
        body: formData
    });
    if (res.status < 300) {
        linkbanner = await res.text();
    }
}



function getProductDetailsFromUI() {
    const detailRows = document.querySelectorAll(".tr-chitietsanpham");
    const details = [];

    detailRows.forEach(row => {
        const sizeSelect = row.querySelector(".select-size");
        const colorSelect = row.querySelector(".select-color");
        const stockInput = row.querySelector(".stock-detail");
        const skuInput = row.querySelector(".sku-detail");
        const activeCheckbox = row.querySelector(".active-detail");
        const price = row.querySelector(".price-detail");
        const oldprice = row.querySelector(".old-price-detail");

        const detail = {
            sizeId: Number(sizeSelect.value),
            colorId: Number(colorSelect.value),
            stock: stockInput.value,
            price: price.value,
            oldPrice: oldprice.value,
            sku: skuInput.value.trim(),
            active: activeCheckbox.checked
        };

        if(detail.sku != '' && detail.sku != null && detail.stock != '' && detail.stock != null){
            details.push(detail);
        }
    });

    return details;
}



function loadInit() {
    $('input#choosefile').change(function() {
        var files = $(this)[0].files;
    });
    document.querySelector('#choosefile').addEventListener("change", previewImages);

    function previewImages() {
        var files = $(this)[0].files;
        for (i = 0; i < files.length; i++) {
            listFile.push(files[i]);
        }

        var preview = document.querySelector('#preview');

        for (i = 0; i < files.length; i++) {
            readAndPreview(files[i]);
        }

        function readAndPreview(file) {

            // if (!/\.(jpe?g|png|gif|webp)$/i.test(file.name)) {
            //     return alert(file.name + " is not an image");
            // }

            var reader = new FileReader(file);

            reader.addEventListener("load", function() {
                var div = document.createElement('div');
                div.className = 'col-lg-2 col-md-3 col-sm-6 col-6';
                div.style.height = '120px';
                div.style.paddingTop = '5px';
                div.marginTop = '100px';
                preview.appendChild(div);

                var img = document.createElement('img');
                img.src = this.result;
                img.style.height = '85px';
                img.style.width = '90%';
                img.className = 'image-upload';
                img.style.marginTop = '5px';
                div.appendChild(img);

                var button = document.createElement('button');
                button.style.height = '30px';
                button.style.width = '90%';
                button.innerHTML = 'xóa'
                button.className = 'btn btn-warning';
                div.appendChild(button);

                button.addEventListener("click", function() {
                    div.remove();
                    console.log(listFile.length)
                    for (i = 0; i < listFile.length; i++) {
                        if (listFile[i] === file) {
                            listFile.splice(i, 1);
                        }
                    }
                    console.log(listFile.length)
                });
            });

            reader.readAsDataURL(file);

        }

    }

}


async function deleteImage(id, btn) {
    var con = confirm("Xác nhận xóa ảnh này?")
    if (con == false) {
        return;
    }
    const response = await fetch(`/adm-prj/qtht-001/api/product/delete-image?id=${id}`, {
        method: 'DELETE'
    });
    if (response.status < 300) {
        toastr.success("xóa ảnh thành công!");
        const parentDiv = btn.parentElement;
        if (parentDiv) {
            parentDiv.remove();
        }
    }
    else {
        toastr.error("Xóa ảnh thất bại, lỗi: "+response.status);
    }
}

async function saveDetail(iddetail){
    const detail = {
        id: document.getElementById("iddt" + iddetail).value,
        productId: 123, // nếu bạn có id sản phẩm cha, truyền từ ngoài vào hoặc giữ cố định
        colorId: document.getElementById("color" + iddetail).value,
        color:{
            id:document.getElementById("color" + iddetail).value
        },
        sizeId: document.getElementById("size" + iddetail).value,
        size:{
            id:document.getElementById("size" + iddetail).value
        },
        stock: parseInt(document.getElementById("stock" + iddetail).value),
        sku: document.getElementById("sku" + iddetail).value,
        active: document.getElementById("active" + iddetail).checked,
        price: document.getElementById("price-detail" + iddetail).value,
        oldPrice: document.getElementById("old-price-detail" + iddetail).value,
    };

    const response = await fetch(`/adm-prj/qtht-001/api/product/update-detail`, {
        method: 'POST',
        headers: new Headers({
            'Content-Type': 'application/json'
        }),
        body: JSON.stringify(detail)
    });
    if (response.status < 300) {
        toastr.success("Cập nhật chi tiết sản phẩm thành công")
    }
    else{
        if (response.status == exceptionCode) {
            var result = await response.json()
            toastr.warning(result.defaultMessage);
        }
        else{
            toastr.error("Có lỗi: "+response.status);
        }
    }
}



async function deleteDetail(id, btn) {
    var con = confirm("Xác nhận xóa chi tiết sản phẩm này?")
    if (con == false) {
        return;
    }
    const response = await fetch(`/adm-prj/qtht-001/api/product/delete-detail?id=${id}`, {
        method: 'DELETE'
    });
    if (response.status < 300) {
        toastr.success("xóa chi tiết sản phẩm thành công!");
        const parentDiv = btn.parentElement.parentElement;
        if (parentDiv) {
            parentDiv.remove();
        }
        var result = await response.text();
        if(result != ''){
            document.getElementById("productimagediv"+result).remove();
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

async function deleteProduct(id, btn) {
    // var con = confirm("Xác nhận xóa sản phẩm này?")
    // if (con == false) {
    //     return;
    // }
    // const response = await fetch(`/adm-prj/qtht-001/api/product/delete-product?id=${id}`, {
    //     method: 'DELETE'
    // });
    // if (response.status < 300) {
    //     toastr.success("xóa sản phẩm thành công!");
    //     const parentDiv = btn.parentElement.parentElement;
    //     if (parentDiv) {
    //         parentDiv.remove();
    //     }
    // }
    // else {
    //     if(response.status == exceptionCode){
    //         var result = await response.json();
    //         toastr.error(result.defaultMessage);
    //     }
    //     else{
    //         toastr.error("Xóa ảnh thất bại, lỗi: "+response.status);
    //     }
    // }

    swal({
        title: "Bạn có chắc muốn xóa sản phẩm này?",
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
            const response = await fetch(`/adm-prj/qtht-001/api/product/delete-product?id=${id}`, {
                method: 'DELETE'
            });
            if (response.status < 300) {
                swal({
                    title: "Thông báo",
                    text: "xóa sản phẩm thành công!",
                    type: "success"
                },
                function() {
                    window.location.reload()
                });
            }
            else {
                if(response.status == exceptionCode){
                    var result = await response.json();
                    toastr.error(result.defaultMessage);
                }
                else{
                    toastr.error("Xóa sản phẩm thất bại, lỗi: "+response.status);
                }
            }
        }
    });
}

function setToHop() {
    var listColorSelect = $("#colorList").val(); // mảng ID màu đã chọn
    var listSizeSelect = $("#sizeList").val();   // mảng ID size đã chọn
    var soLuong = $("#soluongtohop").val();   // mảng ID size đã chọn

    const templateRowHTML = document.getElementById("blockaddctsp").querySelector('tr').outerHTML;
    const targetTbody = document.getElementById("ctnctsp");

    // Xóa các dòng cũ nếu muốn reset
    // targetTbody.innerHTML = "";

    for (let i = 0; i < listColorSelect.length; i++) {
        for (let j = 0; j < listSizeSelect.length; j++) {
            // Clone 1 row từ template
            const tempTbody = document.createElement('tbody');
            tempTbody.innerHTML = templateRowHTML;
            const newRow = tempTbody.firstElementChild;

            // Gán giá trị cho select-size
            const selectSize = newRow.querySelector(".select-size");
            selectSize.value = listSizeSelect[j];

            // Gán giá trị cho select-color
            const selectColor = newRow.querySelector(".select-color");
            selectColor.value = listColorSelect[i];


            // ✅ Gán số lượng mặc định
            const stockInput = newRow.querySelector(".stock-detail");
            if (stockInput) {
                stockInput.value = soLuong;
            }

            // ✅ Gọi luôn setSku để sinh SKU mặc định
            setSku(selectSize);

            // Append vào tbody đích
            targetTbody.appendChild(newRow);
        }
    }
}


async function viewDetailPro(id){
    const response = await fetch(`/adm-prj/qtht-001/api/product/find-by-id?id=${id}`, {
    });
    var result = await response.json();
    var product = result.product;
    console.log(product)
    var productImages = result.productImages;
    var productDetails = result.productDetails;
    document.getElementById("tensanpham").innerHTML = product.name
    document.getElementById("masp").innerHTML = product.id
    document.getElementById("anhsp").src = product.image
    document.getElementById("ngaytao").innerHTML = product.createdAt
    document.getElementById("ngaysua").innerHTML = product.updatedAt
    document.getElementById("seodescription").innerHTML = product.metaDescription
    document.getElementById("soluongban").innerHTML = product.sold
    document.getElementById("noidungtin").innerHTML = product.description
    var main = '';
    for(i=0; i < productImages.length; i++){
        main += `<div class="single-img-detail">
                    <img src="${productImages[i].linkImage}">
                </div>`
    }
    document.getElementById("list-image-detail").innerHTML = main
    console.log(productDetails)
    generateVariant(productDetails)
    $("#modalDetail").modal("show")
}
function generateVariant(data) {
    // Group theo colorId
    const grouped = data.reduce((acc, item) => {
        const colorId = item.color.id;
        if (!acc[colorId]) {
            acc[colorId] = {
                color: item.color,
                productDetails: []
            };
        }
        acc[colorId].productDetails.push({
            id: item.id,
            productId: item.productId,
            colorId: item.colorId,
            color: item.color,
            size: item.size,
            stock: item.stock,
            price: item.price,
            sku: item.sku
        });
        return acc;
    }, {});

    console.log(grouped);

    // Render bảng
    const tbody = document.getElementById("productTable");
    tbody.innerHTML = "";
    Object.values(grouped).forEach(group => {
        const row = document.createElement("tr");

        // Màu sắc
        const tdColor = document.createElement("td");
        tdColor.innerHTML = `<span style="background:${group.color.code};padding:3px 8px;border-radius:4px;">${group.color.name}</span>`;
        row.appendChild(tdColor);

        // Thông tin chi tiết
        const tdDetails = document.createElement("td");
        tdDetails.innerHTML = group.productDetails
            .map(d => `${d.size.name} (SL: ${d.stock}, Giá: ${formatmoney(d.price)}, SKU: ${d.sku})`)
            .join("<br>");
        row.appendChild(tdDetails);

        tbody.appendChild(row);
    });
}


async function changeImage(btn) {
    const id = btn.getAttribute("data-id");
    const fileInput = document.getElementById("inputchangefile"+id);
    fileInput.click();
    fileInput.onchange = async function () {
        if (fileInput.files && fileInput.files[0]) {
            const file = fileInput.files[0];
            const formData = new FormData()
            formData.append("file", file)
            const res = await fetch(`/adm-prj/qtht-001/api/product/update-image-color/${id}`, {
                method: 'POST',
                body: formData
            });
            if (res.status < 300) {
                var linkImage = await res.text();
                document.getElementById("imgcolor"+id).src = linkImage;
            }
            else{
                toastr.error("Đã có lỗi xảy ra, status: "+res.status)
            }
        }
    };
}

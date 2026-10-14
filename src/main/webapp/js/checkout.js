async function loadHuyen(){
    var province = document.getElementById("province").value;
    let provinceName = $('#province').find(':selected').data('name');
    document.getElementById("provinceName").value = provinceName;
    var res = await fetch(`/api/shipping/public/district?provinceId=${province}`, {});
    var result = await res.json();
    var list = result.data;
    var main = '';
    for(i=0; i< list.length; i++){
        main += `<option data-name="${list[i].DistrictName}" value="${list[i].DistrictID}">${list[i].DistrictName}</option>`
    }
    document.getElementById("district").innerHTML = main
    $("#district").select2({
        placeholder: "Chọn huyện"
    });
    loadXa(list[0].DistrictID);
}

async function loadXa(){
    var district = document.getElementById("district").value;
    let districtName = $('#district').find(':selected').data('name');
    document.getElementById("districtName").value = districtName;
    var res = await fetch(`/api/shipping/public/wards?districtId=${district}`, {});
    var result = await res.json();
    var list = result.data;
    var main = '';
    for(i=0; i< list.length; i++){
        main += `<option data-name="${list[i].WardName}" value="${list[i].WardCode}">${list[i].WardName}</option>`
    }
    document.getElementById("ward").innerHTML = main
    $("#ward").select2({
        placeholder: "Chọn xã"
    });
    calShipping();
}

async function calShipping(){
    let wardName = $('#ward').find(':selected').data('name');
    document.getElementById("wardName").value = wardName;

    var district = document.getElementById("district").value;
    var ward = document.getElementById("ward").value;
    var khoiluong = document.getElementById("khoiluong").value;
    var res = await fetch(`/api/shipping/public/tinh-phi?toDistrictId=${district}&toWardCode=${ward}&weight=${khoiluong}`, {});
    var result = await res.json();
    var data = result.data;
    var shippingFee = data.total;
    document.getElementById("ship-cost-change").innerHTML = formatmoney(shippingFee)
    document.getElementById("phiship").value = shippingFee
}


function formatmoney(money) {
    const VND = new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
    });
    return VND.format(money);
}
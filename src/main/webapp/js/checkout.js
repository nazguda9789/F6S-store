// Address requests are versioned so a slow previous selection cannot overwrite the latest one.
let addressVersion = 0;
let feeVersion = 0;
function shippingStatus(message) {
    const status = document.getElementById("shipping-status");
    status.hidden = !message;
    status.textContent = message || "";
    document.querySelectorAll(".place-order-btn").forEach(button => button.disabled = Boolean(message));
}
async function shippingRequest(url) {
    const response = await fetch(url);
    if (!response.ok) throw new Error("Không tải được thông tin vận chuyển. Vui lòng thử chọn lại địa chỉ.");
    const result = await response.json();
    if (result.code !== 200 || result.data == null) throw new Error("Dữ liệu vận chuyển không hợp lệ.");
    return result.data;
}
function setAddressOptions(id, rows, key, label) {
    const select = document.getElementById(id);
    select.replaceChildren(...rows.map(row => {
        const option = new Option(row[label], row[key]);
        option.dataset.name = row[label];
        return option;
    }));
    if (window.jQuery) $(select).trigger("change.select2");
}
function selectedName(id) {
    return document.getElementById(id).selectedOptions[0]?.dataset.name || "";
}
async function loadHuyen() {
    const version = ++addressVersion;
    ++feeVersion;
    setAddressOptions("district", [], "DistrictID", "DistrictName");
    setAddressOptions("ward", [], "WardCode", "WardName");
    shippingStatus("Đang tải địa chỉ giao hàng…");
    document.getElementById("provinceName").value = selectedName("province");
    try {
        const rows = await shippingRequest("/api/shipping/public/district?provinceId=" +
                encodeURIComponent(document.getElementById("province").value));
        if (version !== addressVersion) return;
        if (!Array.isArray(rows) || !rows.length) throw new Error("Chưa có quận/huyện cho tỉnh đã chọn.");
        setAddressOptions("district", rows, "DistrictID", "DistrictName");
        await loadXa();
    } catch (error) { if (version === addressVersion) shippingStatus(error.message); }
}
async function loadXa() {
    const version = ++addressVersion;
    ++feeVersion;
    setAddressOptions("ward", [], "WardCode", "WardName");
    shippingStatus("Đang tải phường/xã…");
    document.getElementById("districtName").value = selectedName("district");
    try {
        const rows = await shippingRequest("/api/shipping/public/wards?districtId=" +
                encodeURIComponent(document.getElementById("district").value));
        if (version !== addressVersion) return;
        if (!Array.isArray(rows) || !rows.length) throw new Error("Chưa có phường/xã cho địa chỉ đã chọn.");
        setAddressOptions("ward", rows, "WardCode", "WardName");
        await calShipping();
    } catch (error) { if (version === addressVersion) shippingStatus(error.message); }
}
async function calShipping() {
    const version = ++feeVersion;
    shippingStatus("Đang tính phí vận chuyển…");
    document.getElementById("wardName").value = selectedName("ward");
    try {
        const params = new URLSearchParams({
            toDistrictId: document.getElementById("district").value,
            toWardCode: document.getElementById("ward").value,
            weight: document.getElementById("khoiluong").value
        });
        const data = await shippingRequest("/api/shipping/public/tinh-phi?" + params);
        if (version !== feeVersion) return;
        const fee = Number(data.total);
        if (!Number.isFinite(fee) || fee < 0) throw new Error("Phí vận chuyển không hợp lệ.");
        const subtotal = Number(document.getElementById("total-tam-tinh").dataset.provisional);
        const effective = subtotal >= 500000 ? 0 : fee;
        const label = document.querySelector(".ship-cost-change");
        label.textContent = formatmoney(effective);
        label.dataset.shipcost = effective;
        document.getElementById("phiship").value = effective;
        if (typeof calTotalPrice === "function") calTotalPrice();
        shippingStatus("");
    } catch (error) { if (version === feeVersion) shippingStatus(error.message); }
}
function formatmoney(money) {
    return new Intl.NumberFormat("vi-VN", {style: "currency", currency: "VND"}).format(money);
}

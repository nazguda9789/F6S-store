
            document.addEventListener("DOMContentLoaded", function() {
 document.body.classList.add("has-mobile-actions");
                const sizeRadios = document.querySelectorAll('.size-radio');
                const qtyInput = document.getElementById('quantity');
                const btnMinus = document.getElementById('btn-minus');
                const btnPlus = document.getElementById('btn-plus');
                const stockStatus = document.getElementById('stock-status');
                const sizeError = document.getElementById('size-error');
                
                const displayFinalPrice = document.getElementById('displayFinalPrice');
                const displayOriginalPrice = document.getElementById('displayOriginalPrice');
                const displayDiscount = document.getElementById('displayDiscount');

                let currentMaxQty = 0;

                function formatCurrency(value) {
                    return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
                }

                sizeRadios.forEach(radio => {
                    radio.addEventListener('change', function() {
                        sizeError.classList.add('d-none');
                        const qty = parseInt(this.dataset.qty);
                        currentMaxQty = qty;
                        qtyInput.value = 1;
                        qtyInput.max = qty;
                        
                        if (qty > 0) {
                            stockStatus.textContent = "Còn " + qty + " sản phẩm";
                            stockStatus.className = "ms-3 text-success";
                        } else {
                            stockStatus.textContent = "Hết hàng";
                            stockStatus.className = "ms-3 text-danger";
                            qtyInput.value = 0;
                        }
                        
                        // Update price
                        const finalPrice = parseFloat(this.dataset.finalprice);
                        const origPrice = parseFloat(this.dataset.origprice);
                        const hasPromo = this.dataset.haspromo === 'true';
                        const discount = this.dataset.discount;
                        
                        displayFinalPrice.textContent = formatCurrency(finalPrice);
                        if (hasPromo) {
                            if(displayOriginalPrice) {
                                displayOriginalPrice.textContent = formatCurrency(origPrice);
                                displayOriginalPrice.classList.remove('d-none');
                            }
                            if(displayDiscount) {
                                displayDiscount.textContent = "GIẢM -" + discount + "%";
                                displayDiscount.classList.remove('d-none');
                            }
                        } else {
                            if(displayOriginalPrice) displayOriginalPrice.classList.add('d-none');
                            if(displayDiscount) displayDiscount.classList.add('d-none');
                        }
                    });
                });

                btnMinus.addEventListener('click', function() {
                    let val = parseInt(qtyInput.value);
                    if (val > 1) {
                        qtyInput.value = val - 1;
                    }
                });

                btnPlus.addEventListener('click', function() {
                    let val = parseInt(qtyInput.value);
                    if (val < currentMaxQty) {
                        qtyInput.value = val + 1;
                    }
                });

                function submitForm(isBuyNow) {
                    let selected = false;
                    sizeRadios.forEach(r => { if(r.checked) selected = true; });
                    if (!selected) {
                        sizeError.classList.remove('d-none');
                        return;
                    }
                    const quantity = Number(qtyInput.value);
                    if (!Number.isInteger(quantity) || quantity < 1 || quantity > currentMaxQty) {
                        stockStatus.textContent = 'Nhập số lượng từ 1 đến ' + currentMaxQty + '.';
                        qtyInput.focus();
                        return;
                    }
                    
                    const form = document.getElementById('addToCartForm');
                    if (isBuyNow) {
                        const input = document.createElement('input');
                        input.type = 'hidden';
                        input.name = 'buyNow';
                        input.value = 'true';
                        form.appendChild(input);
                    }
                    form.setAttribute("aria-busy","true");form.querySelectorAll("button").forEach(b=>b.disabled=true);form.submit();
                }

                document.getElementById('btn-add-cart').addEventListener('click', () => submitForm(false));
                document.getElementById('btn-buy-now').addEventListener('click', () => submitForm(true));
                document.getElementById('btn-mobile-buy').addEventListener('click', () => submitForm(false));
            });
        

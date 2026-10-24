/* Cash is acknowledged by staff; no bank/VNPAY success is simulated here. */
document.addEventListener('DOMContentLoaded',()=>{
    const modal=document.getElementById('saveModal');if(!modal)return;
    const scan=document.createElement('form');scan.className='d-flex flex-wrap gap-2 border rounded p-3 mb-3';scan.id='pos-sku-form';
    const sku=document.createElement('input');sku.className='form-control';sku.maxLength=64;sku.required=true;sku.placeholder='Nhập hoặc quét SKU rồi Enter';sku.setAttribute('aria-label',sku.placeholder);sku.autocomplete='off';
    const add=document.createElement('button');add.className='btn btn-outline-primary';add.textContent='Thêm theo SKU';
    const notice=document.createElement('span');notice.setAttribute('role','status');scan.append(sku,add,notice);
    document.getElementById('select-invoice').closest('.container-fluid').prepend(scan);
    scan.addEventListener('submit',async event=>{
        event.preventDefault();add.disabled=true;
        try{
            if(Number(document.getElementById('select-invoice').value)<1)throw new Error('Chọn hoặc tạo hóa đơn chờ trước khi quét SKU.');
            if(!await createDonTam(true))throw new Error('Không lưu được thông tin đơn chờ trước khi thêm SKU.');
            const params=new URLSearchParams({orderId:document.getElementById('select-invoice').value,sku:sku.value});
            const response=await fetch('/adm-prj/qtht-001/api/order-draft/add-by-sku?'+params,{method:'POST'});
            if(!response.ok||response.redirected)throw new Error('Không thêm được SKU. Kiểm tra đăng nhập.');
            const result=await response.json();if(!result.success)throw new Error(result.message);
            notice.textContent='Đã thêm SKU: '+sku.value;await loadDetailOrder();sku.value='';sku.focus();
        }catch(error){notice.textContent=error.message||'Không kết nối được máy chủ.';}
        finally{add.disabled=false;}
    });
    const tender=document.getElementById('pos-cash-received'),cash=document.getElementById('pos-cash-change');
    const total=document.getElementById('pos-server-total'),button=document.getElementById('pos-quote-button'),suggestions=document.getElementById('pos-voucher-suggestions');
    const format=n=>new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(n);
    let quote=null,busy=false;
    const requiresCash=()=>document.querySelector('[name=orderType]:checked')?.value==='TAI_QUAY'&&document.querySelector('[name=invoiceStatus]:checked')?.value==='HOAN_THANH';
    const update=()=>{
        tender.disabled=!requiresCash();tender.required=requiresCash();
        if(!requiresCash()){cash.textContent='Không ghi nhận thu tiền mặt tại bước này.';return;}
        if(!quote){cash.textContent='Cần báo giá mới từ server trước khi đối chiếu tiền thừa.';return;}
        const paid=Number(tender.value);
        cash.textContent=tender.value===''?'Nhập tiền khách đưa.':!Number.isSafeInteger(paid)||paid<0?'Nhập số tiền VND nguyên hợp lệ.':paid<quote.payable?'Còn thiếu: '+format(quote.payable-paid):'Tiền thừa: '+format(paid-quote.payable);
    };
    async function refresh(){
        if(busy)return;busy=true;button.disabled=true;quote=null;suggestions.replaceChildren();total.textContent='Đang lưu và kiểm tra giá ở server…';update();
        try{
            if(!await createDonTam(true))throw new Error('Không lưu được đơn chờ. Kiểm tra thông tin/voucher.');
            const id=document.getElementById('select-invoice').value;
            const response=await fetch('/adm-prj/qtht-001/api/order-draft/quote?'+new URLSearchParams({orderId:id}));
            if(!response.ok||response.redirected)throw new Error('Không lấy được báo giá. Kiểm tra phiên đăng nhập.');
            const result=await response.json();if(!result.success)throw new Error(result.message);
            if(document.getElementById('select-invoice').value!==id)throw new Error('Hóa đơn đang chọn đã thay đổi. Kiểm tra lại tổng tiền.');
            quote=result.data.price;
            total.textContent='Tiền hàng '+format(quote.merchandise)+' − Giảm '+format(quote.discount)+' + Ship '+format(quote.shipping)+' = Phải thu '+format(quote.payable);
            result.data.vouchers.forEach((voucher,index)=>{
                const use=document.createElement('button');use.type='button';use.className='btn btn-outline-primary btn-sm';
                use.textContent=(index===0?'Gợi ý tốt nhất: ':'')+voucher.code+' (−'+format(voucher.discount)+')';
                use.addEventListener('click',()=>{
                    const select=document.getElementById('discount-code');
                    if(![...select.options].some(o=>o.value===voucher.code))select.add(new Option(voucher.code,voucher.code));
                    select.value=voucher.code;document.getElementById('discount').value=voucher.discount;refresh();
                });suggestions.append(use);
            });
        }catch(error){total.textContent=error.message||'Không thể báo giá.';}
        finally{busy=false;button.disabled=false;update();}
    }
    button.addEventListener('click',refresh);modal.addEventListener('shown.bs.modal',refresh);
    tender.addEventListener('input',update);document.querySelectorAll('[name=orderType],[name=invoiceStatus]').forEach(i=>i.addEventListener('change',update));
    document.getElementById('select-invoice').addEventListener('change',()=>{quote=null;tender.value='';update();});
    document.querySelectorAll('#discount,#shipcost,#discount-code').forEach(i=>i.addEventListener('input',()=>{quote=null;update();}));update();
});

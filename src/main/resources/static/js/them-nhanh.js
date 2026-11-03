document.addEventListener('submit',async event=>{
 const form=event.target;if(!form.matches('.quick-add-form'))return;event.preventDefault();if(form.dataset.busy)return;
 const message=form.querySelector('.quick-add-msg');const show=(text,ok=false)=>{message.textContent=text;message.classList.remove('d-none','text-success','text-danger');message.classList.add(ok?'text-success':'text-danger');};
 const selected=form.querySelector('[name=variantId]:checked');if(!selected){show('Vui lòng chọn kích cỡ và chất liệu còn hàng.');return;}
 form.dataset.busy='true';storeUI.busy(form,true);show('Đang thêm vào giỏ…');
 try{const data=await storeUI.post('/cart/add-ajax',{variantId:selected.value,quantity:1});document.querySelectorAll('.cart-badge').forEach(b=>b.textContent=String(data.totalQuantity));show(data.message||'Đã thêm vào giỏ.',true);}
 catch(error){show(error.message||'Lỗi kết nối. Vui lòng thử lại.');}
 finally{delete form.dataset.busy;storeUI.busy(form,false);}
});

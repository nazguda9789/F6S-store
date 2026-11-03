document.addEventListener('DOMContentLoaded',()=>{
 let pending=false;
 const controls=[...document.querySelectorAll('.cart-qty-input,.cart-remove-btn')];
 const change=async(url,data)=>{
  if(pending)return;pending=true;controls.forEach(c=>c.disabled=true);document.getElementById('store-content').setAttribute('aria-busy','true');storeUI.message('Đang cập nhật giỏ hàng…');
  try{await storeUI.post(url,data);location.reload();}
  catch(error){storeUI.message(error.message||'Không thể kết nối. Giỏ hàng được giữ nguyên.');pending=false;controls.forEach(c=>c.disabled=false);document.getElementById('store-content').removeAttribute('aria-busy');}
 };
 document.querySelectorAll('.cart-qty-input').forEach(input=>{const original=input.value;input.addEventListener('change',()=>{
  const quantity=Number(input.value);if(!Number.isInteger(quantity)||quantity<1||quantity>Number(input.max)){storeUI.message('Số lượng phải từ 1 đến tồn kho hiện tại.');input.value=original;return;}
  change('/cart/update-ajax',{variantId:input.dataset.variantId,quantity}).then(()=>{if(!pending)input.value=original;});
 });});
 document.querySelectorAll('.cart-remove-btn').forEach(button=>button.addEventListener('click',async()=>{
  if(pending)return;if(await storeUI.ask(button.getAttribute('aria-label')+' khỏi giỏ hàng?',button))change('/cart/remove-ajax',{variantId:button.dataset.variantId});
 }));
});

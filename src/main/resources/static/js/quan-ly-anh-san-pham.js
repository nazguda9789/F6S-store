(() => {
  const manager = document.querySelector('[data-image-manager]');
  if (!manager) return;
  const input = manager.querySelector('#images'), list = manager.querySelector('[data-image-list]');
  const fields = manager.querySelector('[data-image-fields]'), message = manager.querySelector('#image-error');
  const counter = manager.querySelector('[data-image-count]'), form = manager.closest('form');
  let rows = [...list.children].map(el => ({el,id:el.dataset.imageId,main:el.dataset.main==='true'}));
  const removed = new Set();
  function hidden(name,value) { const field=document.createElement('input');field.type='hidden';field.name=name;field.value=value;fields.append(field); }
  function sync() {
    const files=new DataTransfer();const uploads=rows.filter(r=>r.file);uploads.forEach(r=>files.items.add(r.file));input.files=files.files;
    if(rows.length && !rows.some(r=>r.main)) rows[0].main=true;
    fields.replaceChildren();removed.forEach(id=>hidden('removedImageIds',id));
    rows.forEach((r,index)=>{
      const key=r.id?'existing:'+r.id:'new:'+uploads.indexOf(r);
      hidden('imageOrder',key);if(r.main)hidden('mainImageKey',key);
      r.el.classList.toggle('is-main',r.main);r.el.querySelector('[data-main-image]').textContent=r.main?'Ảnh chính':'Đặt ảnh chính';
      r.el.querySelector('[data-main-image]').setAttribute('aria-pressed',String(r.main));
      r.el.querySelector('[data-move-image="-1"]').disabled=index===0;r.el.querySelector('[data-move-image="1"]').disabled=index===rows.length-1;
      list.append(r.el);
    });
    counter.textContent='Đã chọn '+rows.length+'/8 ảnh'+(rows.length?'':' — thêm ảnh để bắt đầu.');
  }
  input.addEventListener('change',()=>{
    const selected=[...input.files];message.textContent='';
    const invalid=selected.find(f=>f.size>5*1024*1024 || !['image/jpeg','image/png','image/webp'].includes(f.type) || !/\.(jpe?g|png|webp)$/i.test(f.name));
    if(rows.length+selected.length>8 || invalid) {
      message.textContent=rows.length+selected.length>8?'Mỗi sản phẩm có tối đa 8 ảnh.':'Chỉ chọn ảnh JPG, PNG, WEBP, mỗi ảnh tối đa 5 MB.';sync();return;
    }
    selected.forEach(file=>{
      const el=document.createElement('article');el.className='image-manager-item';
      const image=document.createElement('img');image.src=URL.createObjectURL(file);image.alt='Ảnh vừa chọn: '+file.name;image.width=160;image.height=160;
      const actions=document.createElement('div');actions.className='image-manager-actions';
      [['Đặt ảnh chính','mainImage',''],['↑','moveImage','-1'],['↓','moveImage','1'],['Xóa ảnh','removeImage','']].forEach(([text,key,value])=>{
        const button=document.createElement('button');button.type='button';button.className='btn btn-outline-secondary btn-sm';button.textContent=text;button.dataset[key]=value;
        if(value)button.setAttribute('aria-label',value==='-1'?'Đưa ảnh lên trước':'Đưa ảnh ra sau');actions.append(button);
      });
      el.append(image,actions);rows.push({el,file,main:false,objectUrl:image.src});
    });form.dataset.dirty='true';sync();
  });
  list.addEventListener('click',event=>{
    const button=event.target.closest('button');if(!button)return;
    const index=rows.findIndex(r=>r.el===button.closest('article'));if(index<0)return;
    const row=rows[index];
    if(button.hasAttribute('data-remove-image')) {
      if(!window.confirm('Bạn muốn xóa ảnh này? Ảnh chỉ bị xóa khi bạn lưu sản phẩm.'))return;
      if(row.id)removed.add(row.id);if(row.objectUrl)URL.revokeObjectURL(row.objectUrl);row.el.remove();rows.splice(index,1);
    } else if(button.hasAttribute('data-main-image')) rows.forEach(r=>r.main=r===row);
    else if(button.hasAttribute('data-move-image')) { const to=index+Number(button.dataset.moveImage);if(to>=0 && to<rows.length)[rows[index],rows[to]]=[rows[to],rows[index]]; }
    form.dataset.dirty='true';sync();
  });
  form.addEventListener('submit',event=>{
    if(form.querySelector('[name=status]').value==='true' && !rows.length) {
      event.preventDefault();event.stopImmediatePropagation();message.textContent='Bạn cần ít nhất 1 ảnh khi bật bán sản phẩm.';input.focus();
    }
  },true);
  window.addEventListener('pagehide',()=>rows.filter(r=>r.objectUrl).forEach(r=>URL.revokeObjectURL(r.objectUrl)));
  sync();
})();

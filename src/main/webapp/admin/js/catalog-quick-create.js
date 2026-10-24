/* Bee-style inline creation. Uses existing services, real IDs, no product-form reset. */
document.addEventListener('DOMContentLoaded',()=>{
    const form=document.getElementById('formadd');
    if(!form || !location.pathname.startsWith('/adm-prj/')) return;
    const groups=[['categoryIds','CATEGORY','Danh mục'],['brandId','BRAND','Thương hiệu'],['soleId','SOLE','Loại đế'],['materialId','MATERIAL','Chất liệu'],['colorList','COLOR','Màu sắc'],['sizeList','SIZE','Kích cỡ']];
    const dialog=document.createElement('dialog');dialog.className='catalog-quick-dialog';dialog.setAttribute('aria-labelledby','catalog-quick-title');
    // Static markup only. User/database values are always assigned using textContent/value.
    dialog.innerHTML='<form method="dialog" class="catalog-quick-form"><h2 id="catalog-quick-title"></h2><p class="text-muted">Thuộc tính sẽ được lưu ngay; sản phẩm chỉ được lưu khi bạn bấm Lưu sản phẩm.</p><label>Tên <input name="name" class="form-control" required maxlength="100" autocomplete="off"></label><label data-quick="COLOR">Mã màu HEX <input name="code" class="form-control" pattern="#[0-9a-fA-F]{6}" placeholder="#111827" value="#111827"></label><label data-quick="CATEGORY">Đường dẫn <input name="slug" class="form-control" maxlength="255" pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="giay-chay-bo"></label><label data-quick="CATEGORY">Nhóm cha <select name="parentId" class="form-select"><option value="">Danh mục gốc mới</option></select></label><label data-quick="ATTRIBUTE">Mô tả <textarea name="description" class="form-control" maxlength="500"></textarea></label><p role="alert" class="text-danger" data-error></p><div class="catalog-quick-actions"><button type="button" class="btn btn-outline-secondary" data-cancel>Hủy</button><button type="submit" class="btn btn-primary">Thêm và chọn</button></div></form>';
    document.body.append(dialog);
    const quick=dialog.querySelector('form'), error=dialog.querySelector('[data-error]'), submit=quick.querySelector('[type=submit]');
    let current, busy=false;
    for(const root of document.querySelectorAll('#categoryIds option[data-root="true"]')) quick.elements.parentId.add(new Option(root.textContent,root.value));
    const close=()=>{if(!busy)dialog.close();};quick.querySelector('[data-cancel]').addEventListener('click',close);
    dialog.addEventListener('cancel',event=>{if(busy)event.preventDefault();});
    function slugify(value){return value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');}
    let manualSlug=false;quick.elements.slug.addEventListener('input',()=>{manualSlug=true;});
    quick.elements.name.addEventListener('input',()=>{if(current?.[1]==='CATEGORY'&&!manualSlug)quick.elements.slug.value=slugify(quick.elements.name.value);});
    for(const config of groups){
        const input=document.getElementById(config[0]), wrapper=input?.closest('.bee-builder-field');if(!wrapper)continue;
        const button=document.createElement('button');button.type='button';button.className='catalog-quick-add';button.dataset.quickGroup=config[1];button.textContent='+ Thêm '+config[2].toLocaleLowerCase('vi');
        button.addEventListener('click',()=>{
            current=config;quick.reset();manualSlug=false;error.textContent='';
            quick.elements.name.maxLength=config[1]==='SIZE'?20:(config[1]==='CATEGORY'?255:100);
            dialog.querySelector('h2').textContent='Thêm '+config[2].toLocaleLowerCase('vi');
            for(const label of quick.querySelectorAll('[data-quick]')){
                const show=label.dataset.quick===config[1] || (label.dataset.quick==='ATTRIBUTE'&&['BRAND','SOLE','MATERIAL'].includes(config[1]));
                label.hidden=!show;label.querySelectorAll('input,select,textarea').forEach(i=>{i.disabled=!show;});
            }
            quick.elements.slug.required=config[1]==='CATEGORY';quick.elements.code.required=config[1]==='COLOR';
            dialog.showModal();quick.elements.name.focus();
        });wrapper.append(button);
    }
    quick.addEventListener('submit',async event=>{
        event.preventDefault();if(busy||!quick.reportValidity())return;
        busy=true;submit.disabled=true;error.textContent='';
        try{
            const data={group:current[1],name:quick.elements.name.value};
            for(const field of ['code','slug','description'])if(!quick.elements[field].disabled)data[field]=quick.elements[field].value;
            if(!quick.elements.parentId.disabled && quick.elements.parentId.value)data.parentId=Number(quick.elements.parentId.value);
            const response=await fetch('/adm-prj/qtht-001/shoe-attributes/quick-create',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
            if(response.redirected || response.status===401 || response.status===403)throw new Error('Phiên đăng nhập hết hạn hoặc bạn không có quyền thêm thuộc tính.');
            const result=await response.json();if(!response.ok)throw new Error(result.message||'Không thể thêm thuộc tính.');
            const target=document.getElementById(current[0]);
            target.add(new Option(result.name,String(result.id),false,true));
            target.dispatchEvent(new Event('change',{bubbles:true}));
            // New variants are cloned from this template; also extend existing selectors without changing their values.
            if(['COLOR','SIZE'].includes(current[1])){
                const selector=current[1]==='COLOR'?'.select-color':'.select-size';
                document.querySelectorAll(selector).forEach(select=>{if(![...select.options].some(o=>o.value===String(result.id)))select.add(new Option(result.name,String(result.id)));});
            }
            if(current[1]==='CATEGORY'&&!data.parentId)quick.elements.parentId.add(new Option(result.name,String(result.id)));
            dialog.close();
        }catch(failure){error.textContent=failure.message||'Không kết nối được máy chủ. Dữ liệu sản phẩm vẫn được giữ.';}
        finally{busy=false;submit.disabled=false;}
    });
});

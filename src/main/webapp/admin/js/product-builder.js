/* Reuse actual form controls and persisted variant rows; never duplicate inputs. */
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formadd');
    if (!form) {
        const header = document.querySelector('.card-header-product');
        const search = document.getElementById('search');
        if (!document.getElementById('listproduct') || !header || !search) return;
        const oldSearchWrap = search.closest('.col-md-3');
        const searchRow = document.createElement('div'); searchRow.className = 'bee-product-search';
        const label = document.createElement('label'); label.textContent = 'Tên sản phẩm'; label.htmlFor = 'search';
        search.placeholder = 'Tìm kiếm sản phẩm theo tên hoặc mã...';
        searchRow.append(search); const addProduct = header.querySelector('a[href="save"]'); if(addProduct) searchRow.append(addProduct);
        const filters = document.createElement('div'); filters.className = 'bee-product-filters';
        const status = header.querySelector('.status-toggle'); status.className = 'bee-product-status';
        status.querySelectorAll('input').forEach(i=>i.classList.remove('btn-check'));
        status.querySelectorAll('label').forEach(l=>l.className = '');
        for (const [node,title] of [[status,'Trạng thái'],[document.getElementById('listcategory'),'Danh mục'],[document.getElementById('filterBrand'),'Thương hiệu']]) {
            const field = document.createElement('div'); const caption = document.createElement('label'); caption.textContent = title;
            if(node.id) caption.htmlFor = node.id; field.append(caption,node); filters.append(field);
        }
        const advanced = document.createElement('details'); advanced.className = 'bee-product-advanced';
        const summary = document.createElement('summary'); summary.textContent = 'Lọc giá bổ sung';
        advanced.append(summary,document.getElementById('priceRange'));
        header.replaceChildren(label,searchRow,filters,advanced);
        oldSearchWrap?.remove();
        const stats = document.querySelector('.card-body > .row.mb-3');
        if(stats) document.querySelector('main .table-responsive')?.after(stats);
        let timer; search.addEventListener('input',()=>{ clearTimeout(timer); timer=setTimeout(()=>filterProduct(0),300); });
        for(const selector of ['#listcategory','#priceRange','[name="statusFilter"]','#showEntries','#sortby']) document.querySelectorAll(selector).forEach(i=>i.addEventListener('change',()=>filterProduct(0)));
        return;
    }
    form.classList.add('bee-product-builder');
    const productName = document.getElementById('name'); productName.required = true; productName.maxLength = 255;
    // Paste/mobile input must generate a slug too; renaming an existing shoe must not change its URL.
    const productSlug=document.getElementById('slug');
    let customSlug=new URLSearchParams(location.search).has('id') || Boolean(productSlug.value);
    productName.removeAttribute('onkeyup');
    productSlug.addEventListener('input',()=>{customSlug=true;});
    productName.addEventListener('input',()=>{
        if(!customSlug)productSlug.value=productName.value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[đĐ]/g,'d').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
    });
    for(const id of ['giaban','giacu','weight']) {
        const input = document.getElementById(id); input.type = 'number'; input.step = 'any'; input.min = id === 'weight' ? '0.001' : '0'; input.required = id !== 'giacu';
    }
    document.title = (new URLSearchParams(location.search).has('id') ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm') + ' · F6S';
    const auxiliary = document.createElement('div'); auxiliary.hidden = true;
    auxiliary.append(...form.childNodes); form.append(auxiliary);
    function section(title, open = true) {
        const box = document.createElement('details'); box.className = 'bee-builder-section'; box.open = open;
        const heading = document.createElement('summary'); heading.textContent = title;
        const body = document.createElement('div'); body.className = 'bee-builder-body';
        box.append(heading, body); form.append(box); return body;
    }
    function field(id, title, target) {
        const input = document.getElementById(id); if (!input) return;
        const wrapper = document.createElement('div'); wrapper.className = 'bee-builder-field';
        const label = document.createElement('label'); label.htmlFor = id; label.textContent = title;
        let control = input.closest('.bootstrap-select') || input;
        if (input.multiple && control === input) {
            input.classList.remove('selectpicker');
            const picker = document.createElement('details'); picker.className = 'bee-multi-picker';
            const summary = document.createElement('summary');
            const search = document.createElement('input'); search.type = 'search'; search.className = 'form-control'; search.placeholder = 'Tìm ' + title.toLowerCase(); search.setAttribute('aria-label',search.placeholder);
            search.addEventListener('input',()=>{ for(const option of input.options) option.hidden = !option.textContent.toLocaleLowerCase('vi').includes(search.value.toLocaleLowerCase('vi')); });
            const update = () => {
                summary.replaceChildren();
                if (!input.selectedOptions.length) summary.textContent = 'Chọn ' + title.toLowerCase() + '…';
                for(const option of input.selectedOptions) {
                    const chip = document.createElement('span'); chip.className = 'bee-selection-chip'; chip.append(document.createTextNode(option.textContent));
                    const remove = document.createElement('button'); remove.type = 'button'; remove.textContent = '×'; remove.setAttribute('aria-label','Bỏ ' + option.textContent);
                    remove.addEventListener('click',event=>{event.preventDefault();event.stopPropagation();option.selected=false;input.dispatchEvent(new Event('change',{bubbles:true}));});
                    chip.append(remove);summary.append(chip);
                }
            };
            input.addEventListener('change',update); label.addEventListener('click',()=>{picker.open=true;});
            picker.append(summary,search,input);control=picker;update();
        }
        wrapper.append(label, control); target.append(wrapper);
    }
    const identity = section(new URLSearchParams(location.search).has('id') ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm');
    field('name', 'Tên sản phẩm', identity);
    const attributes = section('Thuộc tính');
    for (const [id,title] of [['categoryIds','Danh mục'],['brandId','Thương hiệu'],['soleId','Loại đế'],['materialId','Chất liệu'],['sizeList','Kích cỡ'],['colorList','Màu sắc'],['giaban','Giá bán mặc định (đ)'],['giacu','Giá cũ (đ)'],['weight','Cân nặng chung của sản phẩm (kg)'],['soluongtohop','Số lượng mặc định']]) field(id,title,attributes);
    const generate = auxiliary.querySelector('button[onclick="setToHop()"]');
    if (generate) { generate.textContent = 'Tạo các biến thể đã chọn'; attributes.append(generate); }
    const variants = section('Danh sách các sản phẩm cùng loại');
    const note = document.createElement('p'); note.className = 'text-muted';
    note.textContent = 'Giữ nguyên số lượng/giá đã nhập khi tạo thêm tổ hợp. Cân nặng hiện áp dụng chung; SKU được cấp sau khi lưu.';
    variants.append(note);
    const tbody = document.getElementById('ctnctsp'); const table = tbody.closest('table');
    const nameHeading = document.createElement('th'); nameHeading.textContent = 'Sản phẩm'; table.querySelector('thead tr').prepend(nameHeading);
    const scroll = document.createElement('div'); scroll.className = 'table-responsive'; scroll.append(table); variants.append(scroll);
    const add = auxiliary.querySelector('button[onclick="addChiTietSanPham()"]'); if (add) variants.append(add);
    const empty = document.createElement('p'); empty.className = 'bee-builder-empty'; empty.textContent = 'Chọn màu sắc và kích cỡ để tạo biến thể.'; variants.append(empty);
    const observer = new MutationObserver(group);
    function group() {
        observer.disconnect();
        tbody.querySelectorAll('.bee-variant-group').forEach(r=>r.remove());
        const rows = [...tbody.querySelectorAll('tr')].filter(r=>r.querySelector('.select-color'));
        const groups = new Map();
        rows.forEach(row=>{
            row.querySelector('.sku-detail').readOnly = true;
            for(const selector of ['.stock-detail','.price-detail','.old-price-detail']) {
                const input = row.querySelector(selector); input.type = 'number'; input.min = '0'; input.step = selector === '.stock-detail' ? '1' : 'any';
            }
            const color = row.querySelector('.select-color'); const size = row.querySelector('.select-size');
            const key = color.value;
            if (!groups.has(key)) groups.set(key,{name:color.selectedOptions[0]?.textContent || '',rows:[]});
            let caption = row.querySelector('.bee-variant-name');
            if (!caption) { caption = document.createElement('td'); caption.className = 'bee-variant-name'; row.prepend(caption); }
            caption.textContent = (document.getElementById('name').value || 'Sản phẩm') + ' [' + (color.selectedOptions[0]?.textContent || '') + ' - ' + (size.selectedOptions[0]?.textContent || '') + ']';
            groups.get(key).rows.push(row);
        });
        for (const value of groups.values()) {
            const header = document.createElement('tr'); header.className = 'bee-variant-group';
            const cell = document.createElement('td'); cell.colSpan = 9; cell.textContent = 'Các sản phẩm màu ' + value.name; header.append(cell);
            tbody.append(header,...value.rows);
        }
        empty.hidden = rows.length > 0;
        observer.observe(tbody,{childList:true});
    }
    tbody.addEventListener('change', event=>{ if(event.target.matches('select')) group(); });
    document.getElementById('name').addEventListener('input',group); group();
    const images = section('Hình ảnh sản phẩm');
    for (const selector of ['#imgpreview','#choosefile','#preview']) {
        const node = document.querySelector(selector); if (node) images.append(node.closest('[class*="col-sm-"]') || node);
    }
    const savedImages = auxiliary.querySelector('.image-upload'); if(savedImages) images.append(savedImages.closest('.row'));
    const colorImages = auxiliary.querySelector('.color-image-product'); if(colorImages) images.append(colorImages);
    images.querySelectorAll('img').forEach(image=>{
        image.onerror = () => { image.onerror = null; image.src = '/image/f6s-placeholder.svg'; };
        if(!image.getAttribute('src') || (image.complete && image.naturalWidth===0)) image.src = '/image/f6s-placeholder.svg';
    });
    const extra = section('Mô tả và thiết lập bổ sung',false);
    for(const [id,title] of [['slug','Đường dẫn sản phẩm'],['metaTitle','Tiêu đề SEO'],['metaDescription','Mô tả SEO']]) field(id,title,extra);
    const description = document.getElementById('ex1-tabs-2'); description.classList.remove('tab-pane','fade'); extra.append(description);
    const active = document.getElementById('activeproduct'); const activeLabel = active.closest('label'); extra.append(activeLabel);
    const loading = document.getElementById('loading'); form.append(loading);
    const submit = document.createElement('button'); submit.type = 'submit'; submit.className = 'btn btn-primary bee-builder-submit'; submit.textContent = 'Lưu sản phẩm'; form.append(submit);
});

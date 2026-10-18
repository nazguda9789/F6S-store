// Offline lookup for legacy three-level addresses; new addresses remain editable.
document.addEventListener('DOMContentLoaded', async () => {
    const province = document.getElementById('local-province');
    const ward = document.getElementById('local-ward');
    if (!province || !ward) return; // GHN keeps its own identifiers and fee lookup.
    const district = document.getElementById('local-district');
    const wardName = document.getElementById('local-ward-name');
    const help = document.getElementById('local-address-help');
    let provinces = [];
    const normalize = text => text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
    const refresh = select => {
        if (window.jQuery && jQuery.fn.select2) jQuery(select).trigger('change.select2');
    };
    const resetWards = () => {
        ward.replaceChildren(new Option('', ''));
        wardName.value = '';
        district.value = '';
        const selected = provinces.find(item => item.name === province.value);
        (selected?.wards || []).forEach(item => {
            // Duplicate ward names are distinguished by their district, not by name alone.
            const option = new Option(`${item.name} — ${item.district}`, item.code);
            option.dataset.name = item.name;
            option.dataset.district = item.district;
            ward.add(option);
        });
        ward.value = '';
        refresh(ward);
    };
    const selectWard = () => {
        const option = ward.selectedOptions[0];
        wardName.value = option?.dataset.name || ward.value;
        district.value = option?.dataset.district || '';
    };
    if (window.jQuery && jQuery.fn.select2) {
        // Select2 emits jQuery change events, which native addEventListener misses.
        jQuery(province).on('change', resetWards);
        jQuery(ward).on('change', selectWard);
        [province, ward].forEach(select => jQuery(select).select2({
            width: '100%', tags: true,
            placeholder: select === province ? 'Tìm tỉnh / thành phố' : 'Tìm phường / xã (có thể gõ không dấu)',
            matcher: (params, data) => !params.term || normalize(data.text || '').includes(normalize(params.term)) ? data : null,
            createTag: params => {
                const text = params.term.trim();
                return text && text.length <= 100 ? {id: text, text: `Dùng địa chỉ: ${text}`, newTag: true} : null;
            }
        }));
    } else {
        // No third-party UI loaded: preserve the ability to place an order manually.
        [province, ward].forEach(select => {
            const input = document.createElement('input');
            input.id = select.id;
            input.name = select === province ? 'provinceName' : '';
            input.className = 'form-control';
            input.required = true;
            input.maxLength = 100;
            if (select === ward) input.addEventListener('input', () => { wardName.value = input.value; });
            select.replaceWith(input);
        });
        help.textContent = 'Nhập tỉnh/thành phố → phường/xã → quận/huyện (nếu có).';
        return;
    }
    try {
        const response = await fetch('/data/addresses-legacy.json');
        if (!response.ok) throw new Error('Address lookup unavailable');
        const data = await response.json();
        if (!Array.isArray(data) || !data.length) throw new Error('Empty address lookup');
        provinces = data;
        // Do not overwrite an address already typed while the file was loading.
        provinces.forEach(item => {
            if (![...province.options].some(option => option.value === item.name)) province.add(new Option(item.name, item.name));
        });
        refresh(province);
    } catch (error) {
        help.textContent = 'Không tải được danh mục. Bạn vẫn có thể gõ tên tỉnh/thành phố và phường/xã, rồi chọn “Dùng địa chỉ”; quận/huyện nhập nếu có.';
    }
});

/* POS presentation uses persisted drafts and the existing order APIs.
 * No synthetic invoice tabs, successful-payment messages or client-side prices. */
document.addEventListener('DOMContentLoaded', () => {
    const select = document.querySelector('#select-invoice');
    if (!select) return;
    const root = document.querySelector('#layoutSidenav_content main .container-fluid');
    root.classList.add('pos-workspace');
    const heading = document.createElement('h1');
    heading.textContent = 'Bán hàng tại quầy';
    const subtitle = document.createElement('p');
    subtitle.className = 'text-muted';
    subtitle.textContent = 'Chọn hóa đơn chờ, thêm giày theo màu / kích cỡ và kiểm tra thông tin trước khi lưu đơn.';
    const tabs = document.createElement('div');
    tabs.className = 'pos-draft-tabs';
    tabs.setAttribute('role', 'group');
    tabs.setAttribute('aria-label', 'Chọn hóa đơn chờ');
    root.prepend(heading, subtitle, tabs);
    const actions = document.createElement('div'); actions.className = 'pos-toolbar';
    const newDraft = document.createElement('button'); newDraft.type = 'button'; newDraft.className = 'btn btn-primary'; newDraft.id = 'pos-new-draft';
    newDraft.textContent = '+ Tạo hóa đơn chờ';
    const chooseCustomer = document.createElement('button'); chooseCustomer.type = 'button'; chooseCustomer.className = 'btn btn-outline-primary'; chooseCustomer.id = 'pos-choose-customer';
    chooseCustomer.textContent = 'Chọn khách hàng';
    const status = document.createElement('p'); status.className = 'text-muted'; status.setAttribute('role', 'status'); status.id = 'pos-action-status';
    actions.append(newDraft, chooseCustomer); tabs.after(actions, status);
    newDraft.addEventListener('click', async () => {
        newDraft.disabled = true;
        try {
            const response = await fetch('/adm-prj/qtht-001/api/order-draft/new', {method:'POST'});
            if (!response.ok) throw new Error('Không tạo được đơn chờ. Kiểm tra đăng nhập và thử lại.');
            const result = await response.json();
            if (!result.success || !result.data?.id) throw new Error(result.message || 'Không tạo được đơn chờ.');
            const option = new Option(result.data.id + ' - Khách lẻ', String(result.data.id));
            select.append(option); select.value = option.value;
            await window.loadDetailOrder(); render();
            status.textContent = 'Đã tạo hóa đơn #' + option.value + '. Chọn giày và lưu thay đổi thông tin trước khi chuyển đơn.';
        } catch(error) { status.textContent = error.message; }
        finally { newDraft.disabled = false; }
    });
    const picker = document.createElement('dialog'); picker.className = 'pos-customer-dialog'; picker.setAttribute('aria-labelledby', 'pos-customer-title');
    const customerTitle = document.createElement('h2'); customerTitle.id = 'pos-customer-title'; customerTitle.className = 'fs-5'; customerTitle.textContent = 'Chọn khách hàng';
    const searchForm = document.createElement('form'); searchForm.className = 'd-flex gap-2 mb-3';
    const search = document.createElement('input'); search.className = 'form-control'; search.maxLength = 100; search.placeholder = 'Tên, email hoặc số điện thoại'; search.setAttribute('aria-label', search.placeholder);
    const submit = document.createElement('button'); submit.className = 'btn btn-primary'; submit.textContent = 'Tìm kiếm'; searchForm.append(search, submit);
    const results = document.createElement('div'); results.className = 'pos-customer-results'; results.setAttribute('role', 'status');
    const close = document.createElement('button'); close.type = 'button'; close.className = 'btn btn-secondary mt-3'; close.textContent = 'Đóng'; close.addEventListener('click', () => picker.close());
    picker.append(customerTitle, searchForm, results, close); document.body.append(picker);
    const loadCustomers = async () => {
        submit.disabled = true; results.textContent = 'Đang tải…';
        try {
            const response = await fetch('/adm-prj/qtht-001/api/order-draft/customers?' + new URLSearchParams({search: search.value}));
            if (!response.ok) throw new Error('Không tải được khách hàng. Vui lòng thử lại.');
            const customers = await response.json(); results.replaceChildren();
            if (!customers.length) results.textContent = 'Không tìm thấy khách hàng đã xác thực đang hoạt động. Có thể nhập khách lẻ trên form.';
            customers.forEach(customer => {
                const line = document.createElement('div'); line.className = 'pos-customer-row';
                const label = document.createElement('span'); label.textContent = [customer.fullName, customer.phone, customer.email].filter(Boolean).join(' · ');
                const choose = document.createElement('button'); choose.type = 'button'; choose.className = 'btn btn-outline-primary btn-sm'; choose.textContent = 'Chọn';
                choose.addEventListener('click', () => {
                    for (const [id, value] of [['fullName',customer.fullName],['phone',customer.phone],['email',customer.email],['addressDetail',customer.address]]) {
                        const input = document.getElementById(id); if (input) { input.value = value || ''; input.dispatchEvent(new Event('input', {bubbles:true})); }
                    }
                    status.textContent = 'Đã điền thông tin liên hệ. Kiểm tra lại tỉnh/phường, sau đó bấm Cập nhật đơn chờ để lưu.';
                    picker.close();
                });
                line.append(label, choose); results.append(line);
            });
        } catch(error) { results.textContent = error.message; }
        finally { submit.disabled = false; }
    };
    searchForm.addEventListener('submit', event => { event.preventDefault(); loadCustomers(); });
    chooseCustomer.addEventListener('click', () => {
        if (Number(select.value) < 1) { status.textContent = 'Tạo hoặc chọn hóa đơn chờ trước khi chọn khách hàng.'; return; }
        picker.showModal(); search.focus(); loadCustomers();
    });
    const render = () => {
        tabs.replaceChildren();
        const options = [...select.options].filter(option => Number(option.value) > 0);
        if (!options.length) {
            const empty = document.createElement('span');
            empty.className = 'text-muted';
            empty.textContent = 'Chưa có hóa đơn chờ. Nhập khách hàng hoặc để Khách lẻ, rồi chọn Lưu đơn chờ.';
            tabs.append(empty);
        }
        options.forEach(option => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'pos-draft-tab';
            button.textContent = 'Hóa đơn #' + option.value;
            button.title = option.textContent;
            button.setAttribute('aria-pressed', String(select.value === option.value));
            button.addEventListener('click', async () => {
                tabs.querySelectorAll('button').forEach(item => item.disabled = true);
                select.value = option.value;
                try { await window.loadDetailOrder(); }
                finally { render(); }
            });
            tabs.append(button);
        });
    };
    render();
    select.addEventListener('change', render);
    new MutationObserver(render).observe(select, { childList: true, subtree: true });

    // Existing product picker becomes a modal while all IDs/event handlers stay intact.
    const productColumn = document.querySelector('#productSearch')?.closest('.col-lg-7');
    const cartColumn = document.querySelector('#orderItemsTableBody')?.closest('.col-lg-5');
    if (productColumn && cartColumn && window.bootstrap) {
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'pos-product-picker';
        modal.tabIndex = -1;
        modal.setAttribute('aria-labelledby', 'pos-product-title');
        const dialog = document.createElement('div');
        dialog.className = 'modal-dialog modal-xl modal-dialog-scrollable';
        const content = document.createElement('div');
        content.className = 'modal-content';
        const header = document.createElement('div');
        header.className = 'modal-header';
        const title = document.createElement('h2');
        title.id = 'pos-product-title';
        title.className = 'modal-title fs-5';
        title.textContent = 'Chọn sản phẩm giày';
        const close = document.createElement('button');
        close.type = 'button'; close.className = 'btn-close';
        close.setAttribute('data-bs-dismiss', 'modal'); close.setAttribute('aria-label', 'Đóng');
        header.append(title, close);
        const body = document.createElement('div');
        body.className = 'modal-body';
        productColumn.className = 'col-12';
        body.append(productColumn);
        content.append(header, body); dialog.append(content); modal.append(dialog);
        document.body.append(modal);
        cartColumn.className = 'col-12';
        const toolbar = document.createElement('div');
        toolbar.className = 'pos-toolbar';
        const list = document.createElement('a');
        list.className = 'btn btn-outline-primary';
        list.href = location.pathname.replace('/create', '/list');
        list.textContent = 'Danh sách hóa đơn';
        const add = document.createElement('button');
        add.type = 'button'; add.className = 'btn btn-primary';
        add.textContent = '+ Thêm sản phẩm';
        add.setAttribute('data-bs-toggle', 'modal');
        add.setAttribute('data-bs-target', '#pos-product-picker');
        toolbar.append(list, add);
        cartColumn.prepend(toolbar);
        modal.addEventListener('shown.bs.modal', () => document.querySelector('#productSearch').focus());
    }
    const empty = document.querySelector('#noOrderItem');
    if (empty) {
        const mark = document.createElement('div'); mark.className = 'pos-empty-mark'; mark.textContent = '＋'; mark.setAttribute('aria-hidden', 'true');
        empty.replaceChildren(mark, document.createTextNode('Chưa có giày trong hóa đơn. Chọn hóa đơn chờ rồi bấm Thêm sản phẩm.'));
    }
    const totals = document.createElement('section');
    totals.className = 'pos-summary'; totals.setAttribute('aria-label', 'Tóm tắt hóa đơn');
    const title = document.createElement('h2'); title.textContent = 'Thông tin hóa đơn';
    totals.append(title);
    const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
    const rows = [['Khách hàng', 'fullName'], ['Tiền hàng', 'totalAmount'], ['Giảm giá nhập tay', 'discount'], ['Phí vận chuyển', 'shipcost']];
    const outputs = rows.map(([label, id]) => {
        const line = document.createElement('div'); const name = document.createElement('span'); name.textContent = label;
        const output = document.createElement('strong'); line.append(name, output); totals.append(line);
        return { id, output };
    });
    const note = document.createElement('p'); note.textContent = 'Giá trị tham khảo từ form hiện tại. Mã giảm giá và tổng cuối cùng được xử lý khi lưu/chuyển đơn; chưa phải xác nhận đã thanh toán.';
    totals.append(note); root.append(totals);
    const update = () => outputs.forEach(({ id, output }) => {
        const value = document.getElementById(id)?.value;
        output.textContent = id === 'fullName' ? value || 'Khách lẻ' : formatter.format(Number(value) || 0);
    });
    root.addEventListener('input', update);
    // Existing draft API updates form values programmatically rather than emitting input.
    const timer = setInterval(update, 600);
    window.addEventListener('pagehide', () => clearInterval(timer), { once: true });
    update();
});

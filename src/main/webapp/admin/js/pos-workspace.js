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

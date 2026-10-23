/* Progressive layout only: no changes to order/account API behavior. */
document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.querySelector('#sidenavAccordion .sb-sidenav-menu > .nav');
    const main = document.querySelector('#layoutSidenav_content main') || document.querySelector('#layoutSidenav_content');
    if (!sidebar || !main) return;
    if (sidebar.dataset.workspaceReady) return;
    sidebar.dataset.workspaceReady = 'true';
    const prefix = location.pathname.startsWith('/emp-prj/') ? '/emp-prj/qtht-001' : '/adm-prj/qtht-001';
    const sale = document.createElement('a');
    sale.className = 'nav-link';
    sale.href = prefix + '/invoice/create';
    const icon = document.createElement('span');
    icon.className = 'sb-nav-link-icon';
    icon.textContent = '＋';
    sale.append(icon, document.createTextNode('Bán hàng tại quầy'));
    sidebar.prepend(sale);
    // Keep existing links and collapse IDs: server-side permissions stay authoritative.
    const rename = (selector, title) => {
        const link = sidebar.querySelector(selector);
        if (!link) return;
        [...link.childNodes].filter(node => node.nodeType === 3).forEach(node => node.remove());
        const caption = document.createElement('span');
        caption.textContent = title;
        link.insertBefore(caption, link.querySelector('.number-counter-navbar, .sb-sidenav-collapse-arrow'));
    };
    rename('[data-bs-target="#collapseProducts"]', 'Quản lý sản phẩm');
    rename('[data-bs-target="#collapseInvoice"]', 'Quản lý giao dịch');
    rename('[data-bs-target="#collapseVoucher"]', 'Giảm giá');
    rename('[data-bs-target="#collapseVendors"]', 'Quản lý tài khoản');
    rename('[data-bs-target="#collapseBlog"]', 'Nội dung website');
    rename('[data-bs-target="#collapseHeThong"]', 'Cấu hình hệ thống');
    if (!location.pathname.startsWith('/emp-prj/')) {
        const productMenu = sidebar.querySelector('#collapseProducts nav');
        for (const [kind, label] of [['BRAND', 'Thương hiệu'], ['SOLE', 'Đế giày'], ['MATERIAL', 'Chất liệu']]) {
            const link = document.createElement('a'); link.className = 'navchild';
            link.href = prefix + '/shoe-attributes?kind=' + kind; link.textContent = label;
            productMenu?.append(link);
        }
        const gallery = document.createElement('a'); gallery.className = 'nav-link';
        gallery.href = prefix + '/image-library'; gallery.textContent = 'Thư viện hình ảnh';
        sidebar.append(gallery);
    }
    // Reuse server-rendered links instead of exposing new routes or permissions.
    const content = sidebar.querySelector('#collapseBlog nav');
    const settings = sidebar.querySelector('#collapseHeThong nav');
    if (content) {
        sidebar.querySelectorAll('#collapseHeThong a[href*="/banner/"]').forEach(link => content.append(link));
        const contact = sidebar.querySelector('a[href$="/contact/list"]');
        if (contact) {
            contact.classList.remove('nav-link');
            contact.classList.add('navchild');
            content.append(contact);
        }
    }
    if (settings) {
        sidebar.querySelectorAll('#collapseQuangCao a').forEach(link => settings.append(link));
        sidebar.querySelector('[data-bs-target="#collapseQuangCao"]')?.remove();
        sidebar.querySelector('#collapseQuangCao')?.remove();
    }
    sidebar.querySelectorAll('[data-bs-target]').forEach(toggle => {
        toggle.setAttribute('aria-controls', toggle.dataset.bsTarget.slice(1));
        // Old route arrays reference links moved to other groups; derive them anew.
        const panel = sidebar.querySelector(toggle.dataset.bsTarget);
        if (panel) toggle.dataset.urls = JSON.stringify([...panel.querySelectorAll('a[href]')].map(a => new URL(a.href).pathname));
    });
    const accounts = sidebar.querySelector('#collapseVendors nav');
    if (accounts) {
        for (const [role, label] of [['ROLE_EMPLOYEE', 'Nhân viên'], ['ROLE_USER', 'Khách hàng đăng ký']]) {
            const link = document.createElement('a'); link.className = 'navchild';
            link.href = prefix + '/user/list?role=' + role; link.textContent = label;
            accounts.prepend(link);
        }
    }
    const customerGroup = sidebar.querySelector('#collapseCustomers');
    if (accounts && customerGroup) {
        const customer = customerGroup.querySelector('a');
        if (customer) { customer.textContent = 'Khách hàng giao dịch'; accounts.append(customer); }
        sidebar.querySelector('[data-bs-target="#collapseCustomers"]')?.remove();
        customerGroup.remove();
    }
    const firstProduct = sidebar.querySelector('#collapseProducts a[href$="/product/list"]');
    if (firstProduct) {
        firstProduct.textContent = 'Sản phẩm giày';
        firstProduct.parentElement.prepend(firstProduct);
    }
    const report = sidebar.querySelector('a[href$="/thong-ke/doanh-thu"]');
    if (report) { rename('a[href$="/thong-ke/doanh-thu"]', 'Tổng quan – Thống kê'); sidebar.prepend(report); }
    // Primary selling workflow first, supporting modules below.
    let cursor = sale;
    for (const id of ['collapseInvoice', 'collapseProducts', 'collapseVendors', 'collapseCustomers', 'collapseVoucher', 'collapseBlog', 'collapseHeThong']) {
        const toggle = sidebar.querySelector(`[data-bs-target="#${id}"]`);
        const panel = sidebar.querySelector('#' + id);
        if (toggle && panel) { cursor.after(toggle, panel); cursor = panel; }
    }
    const brand = document.createElement('a');
    brand.className = 'workspace-brand';
    brand.href = prefix + '/invoice/list';
    const mark = document.createElement('strong');
    mark.textContent = 'F6S';
    const tagline = document.createElement('span');
    tagline.textContent = 'SPORT STORE · WORKSPACE';
    brand.append(mark, tagline);
    sidebar.parentElement.prepend(brand);
    const links = [...sidebar.querySelectorAll('a[href]')];
    let current;
    links.forEach(link => {
        link.classList.remove('active');
        link.removeAttribute('aria-current');
        const target = new URL(link.href);
        const kindMatches = !target.searchParams.has('kind') || target.searchParams.get('kind') === (new URLSearchParams(location.search).get('kind') || 'BRAND');
        const currentRole = new URLSearchParams(location.search).get('role');
        const roleMatches = target.pathname.endsWith('/user/list') ? target.searchParams.get('role') === currentRole : true;
        if (target.pathname === location.pathname && kindMatches && roleMatches && link.getAttribute('href') !== '#') {
            link.classList.add('active');
            link.setAttribute('aria-current', 'page');
            current = link;
            const panel = link.closest('.collapse');
            if (panel) {
                panel.classList.add('show');
                const toggle = sidebar.querySelector(`[data-bs-target="#${panel.id}"]`);
                toggle?.classList.remove('collapsed');
                toggle?.setAttribute('aria-expanded', 'true');
            }
        }
    });
    const group = current?.closest('.collapse');
    if (group) sidebar.querySelector(`[data-bs-target="#${group.id}"]`)?.classList.add('workspace-current-group');
    const breadcrumb = document.createElement('nav');
    breadcrumb.className = 'workspace-heading';
    breadcrumb.setAttribute('aria-label', 'Đường dẫn trang');
    const home = document.createElement('a');
    home.href = prefix + '/invoice/list';
    home.textContent = 'F6S Workspace';
    const label = document.createElement('span');
    label.textContent = current ? current.textContent.trim() : document.title;
    breadcrumb.append(home, document.createTextNode(' / '), label);
    main.prepend(breadcrumb);
    main.classList.toggle('workspace-sale', location.pathname.endsWith('/invoice/create'));
    const toggle = document.querySelector('#sidebarToggle');
    if (toggle) {
        toggle.setAttribute('aria-label', 'Đóng hoặc mở menu quản trị');
        toggle.setAttribute('aria-controls', 'layoutSidenav_nav');
        const syncToggle = () => toggle.setAttribute('aria-expanded', String(
            matchMedia('(min-width: 992px)').matches !== document.body.classList.contains('sb-sidenav-toggled')
        ));
        syncToggle();
        new MutationObserver(syncToggle).observe(document.body, { attributes: true, attributeFilter: ['class'] });
        window.addEventListener('resize', syncToggle);
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape' && !matchMedia('(min-width: 992px)').matches) {
                document.body.classList.remove('sb-sidenav-toggled');
                syncToggle();
            }
        });
    }
});

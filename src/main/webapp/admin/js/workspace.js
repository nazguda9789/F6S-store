/* The module hierarchy is server-rendered in common/workspace-sidebar.html.
 * JavaScript only handles current-page state and responsive presentation. */
document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.querySelector('#sidenavAccordion .sb-sidenav-menu > .nav');
    const main = document.querySelector('#layoutSidenav_content main') || document.querySelector('#layoutSidenav_content');
    if (!sidebar || !main || sidebar.dataset.workspaceReady) return;
    sidebar.dataset.workspaceReady = 'true';
    const prefix = location.pathname.startsWith('/emp-prj/') ? '/emp-prj/qtht-001' : '/adm-prj/qtht-001';
    const params = new URLSearchParams(location.search);
    let current;
    sidebar.querySelectorAll('[data-bs-target]').forEach(toggle => {
        const panel = document.querySelector(toggle.dataset.bsTarget);
        toggle.dataset.urls = JSON.stringify([...panel.querySelectorAll('a[href]')].map(a => new URL(a.href).pathname));
    });
    sidebar.querySelectorAll('a[href]').forEach(link => {
        link.classList.remove('active'); link.removeAttribute('aria-current');
        if (link.getAttribute('href') === '#') return;
        const target = new URL(link.href);
        const kindMatches = !target.searchParams.has('kind') || target.searchParams.get('kind') === (params.get('kind') || 'BRAND');
        const roleMatches = !target.pathname.endsWith('/user/list') || target.searchParams.get('role') === params.get('role');
        if (target.pathname === location.pathname && kindMatches && roleMatches) {
            current = link; link.classList.add('active'); link.setAttribute('aria-current', 'page');
            let panel = link.closest('.collapse');
            while (panel) {
                panel.classList.add('show');
                const toggle = sidebar.querySelector('[data-bs-target="#' + panel.id + '"]');
                toggle?.classList.remove('collapsed'); toggle?.classList.add('workspace-current-group');
                toggle?.setAttribute('aria-expanded', 'true');
                panel = panel.parentElement.closest('.collapse');
            }
        }
    });
    const breadcrumb = document.createElement('nav'); breadcrumb.className = 'workspace-heading';
    breadcrumb.setAttribute('aria-label', 'Đường dẫn trang');
    const home = document.createElement('a'); home.href = prefix + '/invoice/list'; home.textContent = 'F6S';
    const label = document.createElement('span'); label.textContent = current ? current.textContent.trim() : document.title;
    breadcrumb.append(home, document.createTextNode(' / '), label); main.prepend(breadcrumb);
    main.classList.toggle('workspace-sale', location.pathname.endsWith('/invoice/create'));
    const productGroup = sidebar.querySelector('.bee-product-nav-group');
    const productToggle = productGroup?.querySelector('[data-bs-target="#collapseProducts"]');
    if (productGroup) {
        const syncFlyout = () => {
            if (!matchMedia('(min-width: 992px)').matches) return;
            productGroup.style.setProperty('--bee-menu-top', Math.max(70, Math.min(productGroup.getBoundingClientRect().top, innerHeight - 300)) + 'px');
            productToggle.setAttribute('aria-expanded', String(productGroup.matches(':hover') || productGroup.contains(document.activeElement)));
        };
        for (const event of ['mouseenter','mouseleave','focusin']) productGroup.addEventListener(event,syncFlyout);
        productGroup.addEventListener('focusout',()=>setTimeout(syncFlyout,0));
        window.addEventListener('resize',syncFlyout);syncFlyout();
    }
    const toggle = document.querySelector('#sidebarToggle');
    if (toggle) {
        toggle.setAttribute('aria-label', 'Đóng hoặc mở menu quản trị'); toggle.setAttribute('aria-controls', 'layoutSidenav_nav');
        const sync = () => toggle.setAttribute('aria-expanded', String(matchMedia('(min-width: 992px)').matches !== document.body.classList.contains('sb-sidenav-toggled')));
        sync(); new MutationObserver(sync).observe(document.body, {attributes:true,attributeFilter:['class']});
        window.addEventListener('resize', sync);
        document.addEventListener('keydown', event => { if (event.key === 'Escape' && !matchMedia('(min-width: 992px)').matches) { document.body.classList.remove('sb-sidenav-toggled'); sync(); } });
    }
});

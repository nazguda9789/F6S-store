/* Shared presentation layer. Existing forms, APIs, IDs and payment states stay intact. */
document.documentElement.classList.add('f6s-bee-ui');
document.addEventListener('DOMContentLoaded', () => {
    const header = document.querySelector('#menu-main');
    const form = document.querySelector('.bee-header-search');
    form?.addEventListener('submit', async event => {
        event.preventDefault();
        const dialog = document.getElementById('searchProductModal');
        const input = document.getElementById('inputsearchmobile');
        if (!window.bootstrap) return;
        if (!dialog || !input) {
            // Auth/error pages do not include the storefront footer search modal.
            let fallback = document.getElementById('bee-standalone-search');
            if (!fallback) {
                fallback = document.createElement('div'); fallback.id = 'bee-standalone-search'; fallback.className = 'modal fade'; fallback.tabIndex = -1;
                fallback.setAttribute('aria-labelledby', 'bee-search-title');
                const box = document.createElement('div'); box.className = 'modal-dialog modal-lg';
                const content = document.createElement('div'); content.className = 'modal-content';
                const head = document.createElement('div'); head.className = 'modal-header';
                const title = document.createElement('h2'); title.id = 'bee-search-title'; title.className = 'modal-title fs-5'; title.textContent = 'Tìm sản phẩm F6S';
                const close = document.createElement('button'); close.type = 'button'; close.className = 'btn-close'; close.setAttribute('data-bs-dismiss', 'modal'); close.setAttribute('aria-label', 'Đóng');
                const body = document.createElement('div'); body.className = 'modal-body'; body.setAttribute('role', 'status');
                head.append(title, close); content.append(head, body); box.append(content); fallback.append(box); document.body.append(fallback);
            }
            const body = fallback.querySelector('.modal-body'); body.textContent = 'Đang tìm sản phẩm…';
            bootstrap.Modal.getOrCreateInstance(fallback).show();
            try {
                const query = new URLSearchParams({page:0,size:8,search:document.getElementById('bee-search-input').value.trim()});
                const response = await fetch('/api/v1/products/search-by-param?' + query);
                if (!response.ok) throw new Error('Không tải được kết quả. Vui lòng thử lại.');
                const data = await response.json(); body.replaceChildren();
                if (!data.content?.length) body.textContent = 'Không tìm thấy sản phẩm phù hợp.';
                data.content?.forEach(product => {
                    const link = document.createElement('a'); link.className = 'bee-search-result'; link.href = '/product/' + encodeURIComponent(product.slug);
                    link.textContent = product.name; body.append(link);
                });
            } catch(error) { body.textContent = error.message; }
            return;
        }
        input.value = document.getElementById('bee-search-input').value.trim();
        bootstrap.Modal.getOrCreateInstance(dialog).show();
        if (typeof window.searchProductModal === 'function') window.searchProductModal(0, input.value);
        else input.dispatchEvent(new KeyboardEvent('keyup', {key:'Enter',bubbles:true}));
    });
    if (header) {
        const observer = new ResizeObserver(() => {
            document.documentElement.style.setProperty('--bee-header-height', Math.ceil(header.getBoundingClientRect().height) + 'px');
        });
        observer.observe(header);
        header.querySelectorAll('.item-custom-menu').forEach(link => {
            const path = new URL(link.href).pathname;
            const active = link.getAttribute('href') !== 'javascript:void(0)' && path === location.pathname;
            link.classList.toggle('active-menu', active);
            if (active) link.setAttribute('aria-current', 'page');
            else link.removeAttribute('aria-current');
        });
    }
    const target = document.querySelector('main, .bee-home-intro, .shipping-section, .login-section');
    if (target && !document.getElementById('bee-main')) {
        target.id = 'bee-main'; target.tabIndex = -1;
        const skip = document.createElement('a'); skip.className = 'bee-skip-link'; skip.href = '#bee-main'; skip.textContent = 'Đến nội dung chính';
        document.body.prepend(skip);
    }
});

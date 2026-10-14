$( document ).ready(function() {
    if (window.innerWidth < 992) {
        document.querySelectorAll('.item-category-header > a').forEach(link => {
            link.addEventListener('click', function (e) {
                e.preventDefault();

                const parent = this.parentElement;

                // Đóng tất cả submenu khác trước
                document.querySelectorAll('.item-category-header.open').forEach(item => {
                    if (item !== parent) {
                        item.classList.remove('open');
                    }
                });

                // Toggle cái vừa click
                parent.classList.toggle('open');
            });
        });

        // Khi click ra ngoài thì đóng tất cả submenu
        document.addEventListener('click', function (e) {
            const isClickInside = e.target.closest('.item-category-header');
            if (!isClickInside) {
                document.querySelectorAll('.item-category-header.open').forEach(item => {
                    item.classList.remove('open');
                });
            }
        });
    }
});
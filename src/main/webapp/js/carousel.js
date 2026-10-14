$(function() {
    $('.owl-2').owlCarousel({
        loop: false,
        margin: 10,
        nav: true,
        dots: true,
        responsive: {
            0: { items: 2, nav: true, loop: false },
            480: { items: 2, nav: true, loop: false },
            768: { items: 3, nav: true, loop: false },
            1000: { items: 5, nav: true, loop: false }
        }
    });


})


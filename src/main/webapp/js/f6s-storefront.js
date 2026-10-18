// Avoid broken-image icons when a legacy external image is unavailable.
function recoverStoreImage(image) {
    if (!(image instanceof HTMLImageElement)) return;
    if (image.dataset.f6sFallback === 'done') return;
    if (image.src.startsWith('http://res.cloudinary.com/')) {
        image.src = image.src.replace('http://', 'https://');
        return;
    }
    image.dataset.f6sFallback = 'done';
    image.src = '/image/f6s-placeholder.svg';
}
document.addEventListener('error', event => recoverStoreImage(event.target), true);
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('img').forEach(image => {
        if (image.getAttribute('src')?.startsWith('http://res.cloudinary.com/')) {
            image.src = image.src.replace('http://', 'https://');
        } else if (image.complete && image.naturalWidth === 0) {
            recoverStoreImage(image);
        }
    });
});

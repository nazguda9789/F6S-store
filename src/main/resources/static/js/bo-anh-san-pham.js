(() => {
  document.querySelectorAll('[data-product-gallery]').forEach(gallery=>{
    const large=gallery.querySelector('[data-gallery-large]');
    const buttons=[...gallery.querySelectorAll('[data-gallery-thumb]')];
    if(buttons.length<2)return;
    const status=gallery.querySelector('[data-gallery-status]');
    let index=Math.max(0,buttons.findIndex(b=>b.dataset.main==='true'));
    function show(next,scroll=false) {
      index=(next+buttons.length)%buttons.length;
      const image=buttons[index].querySelector('img');large.src=image.src;large.alt=image.alt;
      buttons.forEach((button,i)=>{button.classList.toggle('is-current',i===index);button.setAttribute('aria-current',String(i===index));});
      status.textContent='Ảnh '+(index+1)+' / '+buttons.length;
      if(scroll)buttons[index].scrollIntoView({behavior:'smooth',block:'nearest',inline:'nearest'});
    }
    buttons.forEach((button,i)=>button.addEventListener('click',()=>show(i)));
    gallery.querySelector('[data-gallery-prev]').addEventListener('click',()=>show(index-1,true));
    gallery.querySelector('[data-gallery-next]').addEventListener('click',()=>show(index+1,true));
    gallery.addEventListener('keydown',event=>{
      if(event.key==='ArrowLeft'||event.key==='ArrowRight'){event.preventDefault();show(index+(event.key==='ArrowLeft'?-1:1),true);}
    });
    let start=null;
    large.addEventListener('pointerdown',event=>{start={x:event.clientX,y:event.clientY};large.setPointerCapture(event.pointerId);});
    large.addEventListener('pointerup',event=>{
      if(start){const dx=event.clientX-start.x,dy=event.clientY-start.y;if(Math.abs(dx)>40 && Math.abs(dx)>Math.abs(dy))show(index+(dx<0?1:-1),true);}start=null;
    });
    large.addEventListener('pointercancel',()=>start=null);
    large.addEventListener('dragstart',event=>event.preventDefault());
    show(index);
  });
})();

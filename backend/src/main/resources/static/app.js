const fa = n => new Intl.NumberFormat('fa-IR').format(n);
let cartCount=0;
async function loadProducts(){
  const grid=document.querySelector('#grid');
  try{
    const res=await fetch('/api/products'); if(!res.ok) throw new Error();
    const items=await res.json(); document.querySelector('#result-count').textContent=`${fa(items.length)} کالا`;
    if(!items.length){grid.innerHTML='<p>هنوز محصولی اضافه نشده است.</p>';return;}
    grid.innerHTML=items.map(p=>`<article class="card"><div class="photo">${p.imageUrl?`<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}">`:'✳'}</div><div class="details"><h3>${escapeHtml(p.name)}</h3><p>${escapeHtml(p.description||'')}</p><div class="buy"><span class="price">${fa(p.price)} تومان</span><button ${p.stock<1?'disabled':''} onclick="addToCart()">${p.stock<1?'ناموجود':'افزودن +'}</button></div></div></article>`).join('');
  }catch{grid.innerHTML='<p>ارتباط با فروشگاه برقرار نشد. دوباره صفحه را بارگذاری کنید.</p>';}
}
function addToCart(){document.querySelector('#count').textContent=fa(++cartCount)}
function escapeHtml(value){return String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
loadProducts();

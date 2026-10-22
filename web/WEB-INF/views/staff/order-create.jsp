<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Tạo order tại bàn</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
<style>
.oc-filter{display:flex;gap:10px;flex-wrap:wrap;margin:0 0 12px}.oc-cats{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 14px}.oc-cats button{padding:8px 16px;border-radius:999px;border:1px solid #e3ddd3;background:#fff;color:#444;font-size:.95rem;cursor:pointer}.oc-cats button:hover{border-color:#7a1f1f}.oc-cats button.active{background:#7a1f1f;border-color:#7a1f1f;color:#fff}
.oc-filter input{padding:9px 12px;border:1px solid #ddd;border-radius:10px;font-size:1rem}.oc-filter input{flex:1;min-width:180px}.oc-none{color:#888;display:none}
.oc-layout{display:grid;grid-template-columns:minmax(0,3fr) minmax(0,1fr);gap:20px;align-items:start}
.oc-panel{position:sticky;top:16px;background:#fff;border:1px solid #e8e1d6;border-radius:14px;padding:14px 16px}
.oc-panel h2{margin:0 0 8px;font-size:1.05rem}
.oc-lines{list-style:none;margin:0;padding:0}
.oc-lines li{display:grid;grid-template-columns:minmax(0,1fr) 84px;align-items:center;column-gap:10px;padding:8px 0;border-bottom:1px solid #f0ebe3}
.oc-lines li .oc-note{grid-column:1 / -1}
.oc-lines .oc-note-btn{background:#fff !important;color:#7a1f1f !important;border:1px solid #d9d2c6 !important}
.oc-lines .oc-note-btn.has{background:#fff3cd !important}
.oc-lines small{display:block;color:#7a1f1f;font-weight:600}
.oc-right{display:flex;align-items:center;justify-content:flex-end;gap:8px;flex:none}.oc-right strong{display:inline-block;width:34px;text-align:right}
.oc-note-btn{border:1px solid #e3ddd3;background:#fff;border-radius:6px;padding:0;height:16px;line-height:14px;font-size:9px !important;letter-spacing:0;overflow:hidden;white-space:nowrap;min-width:0;font-weight:400;width:34px;text-align:center;cursor:pointer;color:#7a1f1f;margin:0}.oc-note-btn.has{background:#fff3cd}
.oc-note{flex-basis:100%;margin-top:6px;padding:6px 10px;border:1px solid #ddd;border-radius:8px;font-size:.9rem;box-sizing:border-box}
.oc-total{display:flex;justify-content:space-between;align-items:center;padding:12px 0}
.oc-total strong{font-size:1.2rem;color:#7a1f1f}
.oc-send{width:100%;padding:13px;border:0;border-radius:12px;background:#7a1f1f;color:#fff;font-weight:700;cursor:pointer}
.oc-send:disabled{background:#c9c4bb;cursor:not-allowed}
.oc-empty{color:#888}
/* Chỉ dùng trên máy tính: mở rộng khung trang và xếp món thành lưới 2 cột. */
.orders-page.oc-page{max-width:none;width:auto;margin:24px 24px 40px;padding:0}
.oc-page .order-form{max-width:none;width:100%}
.oc-page .oc-layout{width:100%}
.oc-page .dish-choice{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}
.oc-page .dish-choice-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 14px;margin:0}
.oc-page .dish-choice-row input{width:72px;flex:none;margin:0}
.oc-page .form-grid{max-width:none}
.oc-panel{max-height:calc(100vh - 32px);overflow:auto}
</style></head><body><jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page oc-page"><h1>Order cho <c:out value="${table.tableName}"/></h1><c:if test="${not empty orderMessage}"><p class="notice"><c:out value="${orderMessage}"/></p></c:if>
<p>Order sẽ ở trạng thái chờ duyệt; bạn xác nhận với bếp rồi duyệt từng món ở bước sau.</p>
<form id="orderForm" action="${pageContext.request.contextPath}/manage-orders" method="post" class="order-form"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="action" value="create"><input type="hidden" name="tableId" value="${table.tableId}">
<div class="form-grid"><label>Tên khách (tùy chọn)<input name="customerName" maxlength="100"></label><label>Số điện thoại (tùy chọn)<input name="customerPhone" maxlength="20"></label><label>Ghi chú<input name="note" maxlength="500"></label></div>
<div class="oc-layout">
<div><h2>Chọn món</h2>
<div class="oc-filter"><input type="search" id="ocSearch" placeholder="Tìm món ăn" autocomplete="off"></div>
<div class="oc-cats" id="ocCat"><button type="button" class="active" data-cat="">Tất cả</button><c:forEach var="cat" items="${categories}"><button type="button" data-cat="${cat.categoryId}"><c:out value="${cat.categoryName}"/></button></c:forEach></div>
<p class="oc-none" id="ocNone">Không tìm thấy món phù hợp.</p>
<div class="dish-choice"><c:forEach var="dish" items="${dishes}"><label class="dish-choice-row" data-cat="${dish.categoryId}" data-search="<c:out value='${dish.dishName}'/>"><span><c:out value="${dish.dishName}"/> — <fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ / <c:out value="${dish.unit}"/></span><input type="number" min="0" max="99" name="quantity_${dish.dishId}" value="" placeholder="0" data-name="<c:out value='${dish.dishName}'/>" data-price="${dish.price}" aria-label="Số lượng ${dish.dishName}"></label></c:forEach></div></div>
<aside class="oc-panel" aria-label="Món đã chọn"><c:if test="${not empty approvedItems}">
<h2>Đã gọi (đã duyệt)</h2>
<ul class="oc-lines oc-done"><c:forEach var="it" items="${approvedItems}"><li><div><c:out value="${it.dishName}"/><c:if test="${not empty it.itemNote}"><small style="color:#666;font-weight:400">Note: <c:out value="${it.itemNote}"/></small></c:if></div><div class="oc-right"><strong>x<c:out value="${it.quantity}"/></strong><span style="font-size:.7rem;width:42px;text-align:center;color:${it.itemStatus eq 'SERVED' ? '#14532d' : '#b34100'}">${it.itemStatus eq 'SERVED' ? 'Đã phục vụ' : 'Đang nấu'}</span></div></li></c:forEach></ul>
<hr style="border:0;border-top:2px dashed #e3ddd3;margin:12px 0">
</c:if>
<h2>Món mới chọn <span id="ocCount"></span></h2>
<ul class="oc-lines" id="ocLines"></ul><p class="oc-empty" id="ocEmpty">Chưa chọn món nào.</p>
<div class="oc-total">Tổng tiền <strong id="ocTotal">0 đ</strong></div>
<button type="submit" class="oc-send" id="ocSend" disabled>Gửi order chờ duyệt</button></aside>
</div></form></main>
<script>
(function(){
  var form=document.getElementById('orderForm'),inputs=form.querySelectorAll('input[data-price]'),
      lines=document.getElementById('ocLines'),empty=document.getElementById('ocEmpty'),
      totalEl=document.getElementById('ocTotal'),countEl=document.getElementById('ocCount'),send=document.getElementById('ocSend');
  var notes={},open={};
  function fmt(n){return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g,',')+' đ';}
  function render(){
    lines.innerHTML='';var total=0,count=0;
    inputs.forEach(function(i){
      var q=parseInt(i.value,10)||0;if(q<1)return;if(q>99){q=99;i.value=99;}
      var sub=q*parseFloat(i.dataset.price);total+=sub;count+=q;
      var id=i.name.substring(9),li=document.createElement('li'),name=document.createElement('div'),s=document.createElement('small'),qty=document.createElement('strong'),right=document.createElement('div'),btn=document.createElement('button');
      name.textContent=i.dataset.name;s.textContent=fmt(sub);name.appendChild(s);qty.textContent='x'+q;
      btn.type='button';btn.className='oc-note-btn'+(notes[id]?' has':'');btn.textContent='Note';
      right.className='oc-right';right.appendChild(qty);right.appendChild(btn);li.appendChild(name);li.appendChild(right);
      var inp=document.createElement('input');inp.type='text';inp.name='note_'+id;inp.maxLength=255;inp.className='oc-note';inp.placeholder='Ví dụ: không hành, ít cay';inp.value=notes[id]||'';
      inp.style.display=(open[id]||notes[id])?'block':'none';
      inp.addEventListener('input',function(){notes[id]=inp.value;btn.className='oc-note-btn'+(inp.value?' has':'');});
      inp.addEventListener('keydown',function(e){if(e.key==='Enter')e.preventDefault();});
      btn.addEventListener('click',function(){var show=inp.style.display==='none';inp.style.display=show?'block':'none';open[id]=show;if(show)inp.focus();});
      li.appendChild(inp);lines.appendChild(li);
    });
    empty.style.display=count?'none':'block';totalEl.textContent=fmt(total);
    countEl.textContent=count?'('+count+')':'';send.disabled=!count;
  }
  var rows=form.querySelectorAll('.dish-choice-row'),q=document.getElementById('ocSearch'),cat={value:''},catBox=document.getElementById('ocCat'),none=document.getElementById('ocNone');
  function norm(s){return s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');}
  function filter(){
    var k=norm(q.value.trim()),c=cat.value,shown=0;
    rows.forEach(function(r){var ok=(!c||r.dataset.cat===c)&&(!k||norm(r.dataset.search).indexOf(k)>-1);r.style.display=ok?'':'none';if(ok)shown++;});
    none.style.display=shown?'none':'block';
  }
  q.addEventListener('input',filter);catBox.addEventListener('click',function(e){var b=e.target.closest('button[data-cat]');if(!b)return;catBox.querySelectorAll('button').forEach(function(x){x.classList.toggle('active',x===b);});cat.value=b.dataset.cat;filter();});
  q.addEventListener('keydown',function(e){if(e.key==='Enter')e.preventDefault();});
  inputs.forEach(function(i){
    i.addEventListener('input',render);
    // Lăn chuột không đổi số lượng: bỏ focus để trang cuộn bình thường (chỉ click mũi tên hoặc gõ số mới đổi).
    i.addEventListener('wheel',function(){i.blur();},{passive:true});
  });
  form.addEventListener('submit',function(e){if(!confirm('Gửi order này để chờ duyệt?'))e.preventDefault();});
  render();
})();
</script></body></html>

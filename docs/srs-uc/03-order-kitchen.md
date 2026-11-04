# Group 3 – Order Management & Kitchen

Nguồn đối chiếu: `src/java/controller/order/*` (CartServlet, CustomerMenuServlet, CustomerCheckoutServlet, TableOrderServlet, ManageOrdersServlet, KitchenServlet), `src/java/dao/OrderDAO.java`.
Nội dung viết theo hành vi hiện tại của mã. Mục **[Cần chốt]** là chỗ nhóm phải quyết định.

## Trạng thái trong code (dùng chung cho các UC dưới đây)

| Đối tượng | Trạng thái | Ý nghĩa | Nguồn |
|---|---|---|---|
| Order | PENDING | Khách vừa gửi, chờ nhân viên duyệt món | `OrderDAO.java:267` |
| Order | OPEN | Đã có ít nhất một món được duyệt; bếp đang làm | `OrderDAO.java:331` |
| Order | PAID | Đã thanh toán và giải phóng bàn | `OrderDAO.java` (markPaid) |
| Order | CANCELLED | Đã huỷ (khách, nhân viên, hoặc tất cả món bị từ chối) | `OrderDAO.java:321, 558` |
| Item | PENDING | Món mới gửi, chờ duyệt | `OrderDAO.java:278` |
| Item | COOKING | Đã duyệt, bếp đang làm | `OrderDAO.java:301` |
| Item | SERVED | Đã phục vụ | `OrderDAO.java:362` |
| Item | REJECTED | Nhân viên không duyệt món này | `OrderDAO.java:295` |

Không có trạng thái "Preparing", "Confirmed" hay "Unconfirmed" như SRS ghi cũ. Dùng bảng trên trong SRS.

---

## UC07 - Place Order

**Created by:** Trần Trọng Hiếu
**Primary Actor:** Guest, Customer
**Secondary Actor:** Server (approves the order), Kitchen Staff (receives the order)
**Description:** As a Guest or Customer, I want to order dishes at my table by scanning the table QR code, so that the kitchen can prepare them after staff approval.

**Preconditions**
1. The user has scanned the QR code of a table, so the table is known.
2. The table has no order waiting for approval.

**Input fields – Step 1: Guest information (first visit to the table)**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Full Name | Text, max 30 | Yes | Trimmed; not empty; length ≤ 30 | "Họ và tên là bắt buộc và tối đa 30 ký tự." |
| 2 | Phone Number | Digits, 10 | Yes | Trimmed; exactly 10 digits; starts with 0 | "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số." |

The Guest information is kept in the session until the order is placed.

**Input fields – Step 2: Cart**

| # | Field | Type | Validation rule | Behaviour when invalid |
|---|---|---|---|---|
| 1 | Quantity | Whole number | Must be 1 to 99 | Not added to the cart |
| 2 | Note per dish | Text | Longer than 100 characters is cut to 100 | Cut silently |

**Main Sequence**
1. Guest scans the table QR code. The system remembers the table.
2. Guest enters Full Name and Phone Number (Step 1 fields). The system stores them.
3. Guest browses the menu (UC05, UC06) and adds dishes with quantities (Step 2 fields).
4. Guest opens the cart and optionally types a note for each dish.
5. Guest clicks "Gửi order" (Send order).
6. The system creates an order with status PENDING and all items with status PENDING.
7. The system clears the cart and shows "Order đã gửi, đang chờ nhân viên duyệt." Guest sees the order status page.

**Alternative Sequences**
- **A1 – No table from QR (step 1 or 5):** "Vui lòng quét mã QR trên bàn để gọi món."
- **A2 – Guest information invalid (step 2):** the message from Step 1 table is shown. The system does not move on.
- **A3 – Empty cart (step 5):** "Bạn chưa chọn món nào."
- **A4 – Table already waiting for approval (step 5):** "Bàn đang có order chờ nhân viên duyệt, vui lòng đợi duyệt xong rồi gọi thêm."
- **A5 – Dish stopped or table unavailable (step 6):** "Không thể gửi order. Món có thể đã ngừng phục vụ hoặc bàn không khả dụng." The cart is kept.
- **A6 – Quantity out of range (step 3):** the item is not added. Quantities 1 to 99 only.
- **A7 – Logged-in Customer:** the order is linked to the Customer account. Points and vouchers are applied at payment (UC23, UC26, UC27).

**Post conditions**
1. A new order exists with status PENDING. Its items have status PENDING.
2. The table cannot take another order until staff approves this one.

**Business Rules**
- **BR-01 Table from QR:** a Guest order needs a table from the QR code. Free-typed table numbers are not accepted.
- **BR-02 One waiting order per table:** while an order is PENDING, the table cannot receive another order.
- **BR-03 Delivery and takeaway:** not implemented. Orders are always for a table. Remove delivery and takeaway from the SRS, or add them as future items.
- **BR-04 Item note:** at most 100 characters. Longer notes are cut silently.
- **BR-05 Stock check:** not implemented at order time. Sold-out handling is done by staff (UC14, UC15).

---

## UC08 - Cancel Order

**Created by:** Trần Trọng Hiếu
**Primary Actor:** Guest, Customer, Server (staff cancel is Server only, `ManageOrdersServlet.java:94`)
**Secondary Actor:** Kitchen Staff
**Description:** As a Guest, Customer or Server, I want to cancel an order that is still waiting, so that a wrong or unwanted order is stopped.

**Preconditions**
1. The order exists.
2. The order is PENDING (waiting for staff approval).

**Input fields**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Reason | Text | Yes | Trimmed; not empty | "Cần nhập lý do để hủy đơn." |

**Main Sequence – Customer cancel**
1. Customer opens "My orders" and selects a PENDING order.
2. Customer clicks "Hủy đơn" and enters a reason.
3. The system cancels the order and all its PENDING items.
4. The system shows "Đã hủy đơn hàng."

**Main Sequence – Staff cancel**
1. Server (or Manager) opens the order in "Manage orders".
2. Server clicks cancel and enters a reason.
3. The system cancels the order and shows "Đã hủy đơn hàng."

**Alternative Sequences**
- **A1 – No reason entered:** the system does not cancel. Shows the message from row 1.
- **A2 – Order no longer PENDING:** the system does not cancel and shows "Không thể thực hiện thao tác này với đơn hàng."
- **A3 – Customer cancels someone else's order:** the system does not cancel.

**Post conditions**
1. Order status is CANCELLED. Its PENDING items are removed from the kitchen queue.

**Business Rules**
- **BR-01 Only before approval:** once staff approves any item, the customer cannot cancel. Staff can change items with UC13 or UC30.
- **BR-02 Reason required:** every cancellation stores a reason.
- **BR-03 Manager authorisation:** not implemented. Staff cancel is allowed for Server only. **[Cần chốt]** The old SRS said a Manager must approve cancelling a confirmed item; the code does not do this. Choose: implement, or remove the rule.

---

## UC13 - Edit Order

**Created by:** Vũ Đức Dũng
**Primary Actor:** Server
**Secondary Actor:** None
**Description:** As a Server, I want to change items of a table order so that customer requests are handled.

**Preconditions**
1. The Server is logged in.
2. The table has an open order (status OPEN or PENDING).

**Input fields (Server screen)**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Table | Choice | Yes | Must be a valid table with an order | "Hãy chọn bàn và ít nhất một món." |
| 2 | Customer Name | Text, max 30 | No | Length ≤ 30 | "Tên khách tối đa 30 ký tự." |
| 3 | Customer Phone | Digits, 10 | No | If entered: starts with 0, exactly 10 digits | "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số." |
| 4 | Order Note | Text, max 100 | No | Length ≤ 100 | "Ghi chú tối đa 100 ký tự." |
| 5 | Dish and quantity | Number | At least one dish | **[Cần kiểm]** quantity range not confirmed in this review | – |
| 6 | Item note | Text | No | Cut to 100 characters | Cut silently |

If a validation fails, the system shows the message and keeps the entered values.

**Main Sequence**
1. Server opens the table (table layout, UC12).
2. Server selects "Order" for the table.
3. Server adds dishes, changes quantities, or adds notes.
4. Server clicks save.
5. The system validates fields 2–4 and saves the new items with status PENDING.
6. The system shows the saved order.

**Alternative Sequences**
- **A1 – Validation fails (step 5):** the system shows the message and waits.
- **A2 – Save fails:** "Không thể tạo order. Hãy kiểm tra bàn và các món đã chọn."

**Post conditions**
1. New items are saved. They wait for approval (UC14).

**Business Rules**
- **BR-01 Items already cooking:** changing a COOKING item is done by the kitchen (UC30), not by this use case.
- **BR-02 Recalculation:** the order total is recalculated from item lines after each change.

---

## UC14 - Confirm Order

**Created by:** Vũ Đức Dũng
**Primary Actor:** Server
**Secondary Actor:** Kitchen Staff
**Description:** As a Server, I want to approve the dishes of a waiting order, so that the kitchen starts cooking the approved dishes.

**Preconditions**
1. The Server is logged in.
2. The order has PENDING items.

**Input fields**

| # | Field | Type | Required | Validation rule |
|---|---|---|---|---|
| 1 | Approve or reject each item | Choice per item | Yes | Each item: approve with a quantity, or reject |
| 2 | Approved quantity | Number | Yes for approved items | Must be a positive whole number not greater than the requested quantity **[Cần kiểm]** |

**Main Sequence**
1. Server opens "Manage orders" and selects a PENDING order.
2. The system shows the items with requested quantities.
3. Server checks with the kitchen that the dishes are available (verbal check).
4. Server sets approved quantities or rejects items, then clicks approve.
5. The system marks approved items COOKING and rejected items REJECTED.
6. The system sends the approved items to the kitchen screen.
7. The system shows "Đã duyệt món, các món được duyệt đã lên màn hình bếp."

**Alternative Sequences**
- **A1 – Nothing approved:** the order is cancelled. The system shows "Không món nào được duyệt nên đơn đã được hủy."
- **A2 – Order changed by someone else:** the approval fails and the page is reloaded.
- **A3 – Dish sold out (step 3):** Server rejects that item (or reduces the quantity) and approves the rest.

**Post conditions**
1. Order status is OPEN (if any item was approved) or CANCELLED.
2. Approved items are visible to the kitchen (UC30).

**Business Rules**
- **BR-01 Only Server approves:** other roles get a 403 error.
- **BR-02 Per-item approval:** approval is per item, not for the whole order.
- **BR-03 Kitchen sees approved items only:** the kitchen does not see PENDING or REJECTED items.
- **BR-04 Stock:** the system does not check stock. The check is verbal (see UC15).

---

## UC15 - Update Dish Status (Server)

**Created by:** Vũ Đức Dũng
**Primary Actor:** Server
**Secondary Actor:** Kitchen Staff
**Status in current code:** **Not implemented for Server.** Only Manager can show or hide a dish, and that changes the whole menu (UC31), not a per-day sold-out flag.

**Description:** As a Server, I want to mark a dish as sold out, so that the menu shows the change.

**Decision needed [Cần chốt]:**
- (a) Remove UC15 from this release. Sold-out is handled by verbal check and UC14 rejection.
- (b) Add a "sold out" flag per dish, changed by Server, and hidden from the Guest and Customer menu. The field would be a new column on Dish.

---

## UC30 - View Order List (Kitchen)

**Created by:** Vũ Đức Dũng
**Primary Actor:** Chef
**Secondary Actor:** Server and Manager (also have access, see BR-04)
**Description:** As a Chef, I want to see the approved dishes to cook, so that I can prepare them in order.

**Preconditions**
1. The user is logged in with role Chef (or Server or Manager, see BR-04).

**Input fields – Adjust an item**

| # | Field | Type | Validation rule | Message when wrong |
|---|---|---|---|---|
| 1 | Quantity | Whole number | Zero or more. Zero removes the item from the order. | "Không thể cập nhật món. Món có thể đã được phục vụ hoặc đã thay đổi." |

Quantity can be changed only while the item is COOKING and the order is OPEN.

**Main Sequence**
1. Chef opens the kitchen screen.
2. The system shows COOKING items of OPEN orders, oldest first.
3. Chef reduces a quantity or removes an item if the kitchen cannot make it.
4. The system updates the item and shows "Đã cập nhật số lượng món." or "Đã hủy món khỏi order."

**Alternative Sequences**
- **A1 – Item already served or changed (step 3):** the change fails with the message in row 1.
- **A2 – Last item removed:** the order is cancelled automatically if it has no items left.

**Post conditions**
1. The kitchen queue shows the current items.

**Business Rules**
- **BR-01 Order of display:** items are shown oldest first, using the time the order was created or approved (`OrderDAO.java:437–490`).
- **BR-02 Station filter:** not implemented. The list shows all stations. Remove station filter from the SRS, or add a station field to items.
- **BR-03 Colour and special notes highlight:** **[Cần kiểm]** not confirmed in this review.
- **BR-04 Access:** the kitchen screen accepts Chef. Server and Manager can also open it. **[Cần chốt]** Keep, or restrict to Chef.

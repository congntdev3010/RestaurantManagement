# Group 4 – Table & Reservation Management

Nguồn đối chiếu: `src/java/controller/reservation/*` (BookingServlet, StaffBookingServlet, ManageReservationsServlet, CheckinServlet, LateArrivalCleaner), `src/java/controller/table/ManageTablesServlet.java`, `src/java/dao/ReservationDAO.java`, `src/java/dao/RestaurantTableDAO.java`, `database/add_reservation_booking_flow(11).sql`, `database/add_restaurant_tables(4).sql`.
Nội dung viết theo hành vi hiện tại của mã. Mục **[Cần chốt]** là chỗ nhóm phải quyết định. Mục **[Cần kiểm]** là chưa xác nhận đủ trong lần đọc này.

## Trạng thái trong code

| Đối tượng | Trạng thái | Ý nghĩa | Nguồn |
|---|---|---|---|
| Reservation | PENDING | Khách đặt online, chờ lễ tân duyệt | `add_reservation_booking_flow(11).sql:82` |
| Reservation | APPROVED | Đã duyệt, đã giữ bàn cho ca | `ManageReservationsServlet.java:221` |
| Reservation | REJECTED | Lễ tân từ chối | `ManageReservationsServlet.java:180` |
| Reservation | CANCELLED | Huỷ (lễ tân hoặc tự động) | `ManageReservationsServlet.java:202` |
| Reservation | COMPLETED | Khách đã đến (check-in) | `CheckinServlet.java:135` |
| Reservation | NO_SHOW | Không đến | `ManageReservationsServlet.java:198` |
| Table | AVAILABLE, RESERVED, OCCUPIED, UNAVAILABLE | Trạng thái bàn | `add_restaurant_tables(4).sql:15` |

Ca phục vụ (`add_reservation_booking_flow(11).sql:30–31`):
- **Ca trưa (LUNCH):** 10:00–14:30
- **Ca tối (DINNER):** 17:00–22:30

---

## UC09 - Make Reservation

**Created by:** Trần Trọng Hiếu
**Primary Actor:** Guest, Customer
**Secondary Actor:** Receptionist (confirms the request, see UC17)
**Description:** As a Guest or Customer, I want to request a table for a date, time and party size, so that a seat is held for me.

**Preconditions**
1. The Reservation page is open. **[Cần kiểm]** whether login is required.

**Input fields**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Full name | Text | Yes | Trimmed; length 2 to 30 | "Họ tên phải từ 2 đến 30 ký tự." |
| 2 | Phone number | Digits, 10 | Yes | Starts with 0; exactly 10 digits | "Số điện thoại không hợp lệ (ví dụ 0901234567)." |
| 3 | Table type | Choice | Yes | Must be a table type that exists | "Loại bàn không hợp lệ." |
| 4 | Party size | Whole number | Yes | 1 to 30 | "Số người phải từ 1 đến 30." |
| 5 | Date | Date | Yes | Must be a valid date | "Vui lòng chọn ngày và giờ đến." |
| 6 | Date | Date | Yes | From today to 7 days ahead | "Chỉ nhận đặt bàn từ hôm nay đến 7 ngày tới." |
| 7 | Time | Time | Yes | Must be a valid time | "Vui lòng chọn ngày và giờ đến." |
| 8 | Time | Time | Yes | Must be inside a serving shift (LUNCH or DINNER) | "Giờ đến phải nằm trong giờ phục vụ: …" (lists the shifts) |
| 9 | Date and time | – | – | At least 60 minutes after now | "Vui lòng đặt trước ít nhất 60 phút." |
| 10 | Note | Text, max 100 | No | Length ≤ 100 | "Ghi chú tối đa 100 ký tự." |
| 11 | Phone | – | – | Same phone may have at most 3 PENDING requests | "Số điện thoại này đang có 3 …" (`BookingServlet.java:83`) |

Phone is saved in the format `0` + 9 digits.

**Main Sequence**
1. Guest or Customer opens the Reservation page.
2. Guest fills in fields 1–10.
3. Guest clicks "Đặt bàn" (Book).
4. The system validates the fields in the order of the table.
5. The system saves the request with status PENDING and shows a reservation code.

**Alternative Sequences**
- **A1 – Validation fails (step 4):** the system shows the message of the first failed rule. Values are kept.
- **A2 – Too many pending requests for this phone (step 4):** the system refuses the request.
- **A3 – No table free (step 5):** **[Cần kiểm]** the current check is at approval time (UC17). The request is still saved.

**Post conditions**
1. A reservation row exists with status PENDING. It does not hold a table until staff approves it.

**Business Rules**
- **BR-01 Lead time:** at least 60 minutes before the requested time. The old SRS said 2 hours. **[Cần chốt]** Keep 60 minutes, or change the code.
- **BR-02 Booking window:** from today to 7 days ahead.
- **BR-03 Pending limit:** 3 pending requests per phone number.
- **BR-04 Party size:** 1 to 30 guests.
- **BR-05 Shift:** only the two serving shifts are accepted.

---

## UC12 - View Table Layout

**Created by:** Vũ Đức Dũng
**Primary Actor:** Server, Receptionist
**Secondary Actor:** Manager
**Description:** As a Server or Receptionist, I want to see the tables and their status, so that I can seat guests.

**Preconditions**
1. The user is logged in with role Server, Receptionist or Manager.

**Input fields (filters)**

| # | Field | Type | Validation rule |
|---|---|---|---|
| 1 | Zone or table type | Choice | Optional. Shows only the chosen group. |

**Main Sequence**
1. User opens "Sơ đồ bàn" (table layout).
2. The system loads all tables with name, type, capacity and status.
3. The system groups tables by type and shows each table with its status.
4. User clicks a table to open its actions (order, status, pay).

**Alternative Sequences**
- **A1 – Load fails:** the system shows an error and a retry option.

**Post conditions**
1. The layout shows the status of every table at the time of loading.

**Business Rules**
- **BR-01 Status values:** AVAILABLE, RESERVED, OCCUPIED, UNAVAILABLE.
- **BR-02 Colour codes and real-time refresh:** **[Cần kiểm]** the SRS requires colours and live update across devices. Not confirmed in this review. Page must be reloaded to see changes.

---

## UC16 - View Reservation List

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** Manager
**Description:** As a Receptionist, I want to see the reservations of a day, so that I can plan seating.

**Input fields (filters)**

| # | Field | Type | Validation rule | Behaviour when invalid |
|---|---|---|---|---|
| 1 | Status | Choice | PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED, NO_SHOW, or all | Empty or unknown: all |
| 2 | Date | Date | Valid date | Invalid: no date filter |
| 3 | Shift | Choice | LUNCH, DINNER | Other: no shift filter |

**Main Sequence**
1. Receptionist opens "Quản lý đặt bàn" (manage reservations).
2. The system shows reservations, filtered by the values chosen.
3. Receptionist selects a reservation to act on (UC17, UC20, or cancel).

**Alternative Sequences**
- **A1 – No reservation matches:** the list is empty.

**Post conditions**
1. The list shows the reservations for the chosen filter.

**Business Rules**
- **BR-01 Default:** **[Cần kiểm]** the SRS says the default is today's reservations, sorted by time. Confirm the default filter in code.
- **BR-02 Highlight for arrivals within 30 minutes:** **[Cần kiểm]** not confirmed in this review.

---

## UC17 - Confirm Reservation Request

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** Customer (contacted by phone when needed)
**Description:** As a Receptionist, I want to approve or reject a pending reservation and hold tables for it, so that the booking is confirmed.

**Preconditions**
1. The reservation has status PENDING.

**Input fields – Approve**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Shift | Choice | Yes | LUNCH or DINNER | "Vui lòng chọn bàn (tối đa 10) và ca (trưa/tối) trước khi duyệt." |
| 2 | Tables | Multi-choice | Yes | 1 to 10 tables | "Vui lòng chọn bàn (tối đa 10) và ca (trưa/tối) trước khi duyệt." |
| 3 | Total capacity | – | – | Sum of capacity of chosen tables ≥ party size | "Tổng sức chứa các bàn đã chọn chưa đủ cho số khách của đơn. Hãy chọn thêm bàn để gộp." |
| 4 | Tables | – | – | Tables still free in that shift | "Có bàn vừa được giữ cho đơn khác trong ca này. Vui lòng chọn bàn khác." |
| 5 | Tables | – | – | Chosen tables must exist | "Bàn đã chọn không tồn tại." |
| 6 | Reservation | – | – | Status must still be PENDING | "Đơn đã được xử lý trước đó." |

**Input fields – Reject**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Reason | Text, max 255 | No | Trimmed; empty is stored as "Lễ tân huỷ theo yêu cầu khách" for cancel | – |

**Input fields – Contacted (optional)**

| # | Field | Type | Required | Validation rule |
|---|---|---|---|---|
| 1 | Staff note | Text, max 500 | No | Trimmed |

**Main Sequence – Approve**
1. Receptionist opens the reservation from the list (UC16).
2. The system shows the request: name, phone, party size, date, time, table type, note.
3. Receptionist checks table availability and phones the customer if needed (contacted action).
4. Receptionist chooses the shift and tables (fields 1–2), then clicks "Duyệt" (Approve).
5. The system validates rows 1–6 of the approve table.
6. The system sets status APPROVED and holds the tables for the shift.
7. The system shows "Đã duyệt đơn và giữ bàn cho khách."

**Main Sequence – Reject**
1. Receptionist opens the request and clicks "Từ chối" (Reject), with an optional reason.
2. The system sets status REJECTED and shows a success message.

**Alternative Sequences**
- **A1 – Validation fails (step 5):** the message from the table is shown. The request stays PENDING.
- **A2 – Conflict (step 5, row 4):** another reservation took a table in the same shift. Receptionist chooses other tables.
- **A3 – Save error:** "Không thể duyệt đơn do lỗi dữ liệu."

**Post conditions**
1. Approved: status APPROVED, tables held for the shift.
2. Rejected: status REJECTED.

**Business Rules**
- **BR-01 Shift and tables:** an approved request holds 1 to 10 tables for one shift.
- **BR-02 Capacity:** total capacity must cover the party size.
- **BR-03 No double booking:** a table cannot be held by two approved requests in the same shift.
- **BR-04 Auto-hold of table status:** **[Cần kiểm]** the SRS says the table turns RESERVED 30 minutes before arrival. Not confirmed in this review.
- **BR-05 Deadline to decide:** **[Cần kiểm]** the SRS says a request must be decided 1 hour before the time. Not enforced in this review.

---

## UC18 - Create Direct Reservation

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** None
**Description:** As a Receptionist, I want to create a reservation for a walk-in or phone caller and hold tables immediately.

**Input fields**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Customer name | Text | Yes | Same as UC09 row 1 (2 to 30) | "Họ tên phải từ 2 đến 30 ký tự." |
| 2 | Customer phone | Digits, 10 | Yes | Same as UC09 row 2 | "Số điện thoại không hợp lệ (ví dụ 0901234567)." |
| 3 | Table type | Choice | Yes | Same as UC09 row 3 | "Loại bàn không hợp lệ." |
| 4 | Party size | Whole number | Yes | 1 to 30 | "Số người phải từ 1 đến 30." |
| 5 | Date | Date | Yes | Same date rule as UC09 | "Chỉ nhận đặt bàn từ hôm nay đến 7 ngày tới." |
| 6 | Time | Time | Yes | Must be inside a serving shift | "Giờ đến phải nằm trong giờ phục vụ: …" |
| 7 | Tables | Multi-choice | Yes | 1 to 10 tables | "Vui lòng chọn từ 1 đến 10 bàn." |
| 8 | Total capacity | – | – | Sum of capacity ≥ party size | "Tổng sức chứa các bàn đã chọn chưa đủ cho số khách. Hãy chọn thêm bàn để gộp." |
| 9 | Tables | – | – | Free in that shift | "Có bàn vừa được giữ cho đơn khác trong ca này. Vui lòng chọn lại bàn." |
| 10 | Note | Text, max 100 | No | Length ≤ 100 | "Ghi chú tối đa 100 ký tự." |

Difference from UC09: the 60-minute lead time does **not** apply to staff bookings **[Cần kiểm]**.

**Main Sequence**
1. Receptionist opens "Đặt bàn trực tiếp" (direct booking).
2. Receptionist fills in fields 1–10 and selects tables. The system can show the free tables first.
3. Receptionist clicks save.
4. The system validates the fields and saves the reservation with status APPROVED, holding the tables.
5. The system shows a success message.

**Alternative Sequences**
- **A1 – Validation fails:** the message from the table is shown. Values are kept.
- **A2 – Table conflict:** the system asks for other tables.

**Post conditions**
1. A reservation with status APPROVED exists and the tables are held.

**Business Rules**
- **BR-01 No second approval:** a direct booking is approved at once.
- **BR-02 Terminology:** the code uses APPROVED, not "Confirmed". Use the code term in the SRS.

---

## UC19 - Search Reservation

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** None
**Description:** As a Receptionist, I want to search reservations by customer name or phone, so that I can find a booking at check-in.

**Input fields**

| # | Field | Type | Required | Validation rule | Behaviour when invalid |
|---|---|---|---|---|---|
| 1 | Search text | Text, max 100 | No | Trimmed. Longer than 100 characters is cut to 100. Matches part of the customer name or phone. | Cut silently |

Special characters `[`, `%` and `_` are escaped, so they are matched as normal text.

**Main Sequence**
1. Receptionist opens the check-in screen.
2. Receptionist types part of a name or phone number in the search box.
3. The system shows matching reservations for today.
4. Receptionist selects one (UC20).

**Alternative Sequences**
- **A1 – No match:** the list is empty.

**Post conditions**
1. The list shows the matching reservations.

**Business Rules**
- **BR-01 Partial match:** name or phone.
- **BR-02 Search by reservation ID:** not implemented. Search by ID is not in the code.
- **BR-03 Date filter and paging:** **[Cần kiểm]** the SRS asks for a date filter and paging above 20 records. Not confirmed.

---

## UC20 - Process Customer Check-in

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** None
**Description:** As a Receptionist, I want to check in a booked guest, so that the table is marked occupied.

**Preconditions**
1. The reservation has status APPROVED and a table.
2. The reservation date is today.

**Input fields:** none. Action is a button on the reservation.

**Main Sequence**
1. Receptionist finds the reservation (UC19).
2. Receptionist clicks "Khách đã đến" (guest arrived).
3. The system checks the rules below.
4. The system sets status COMPLETED, records the arrival time, and sets the table to OCCUPIED.
5. The system shows "Khách đã đến: bàn đã chuyển sang đang phục vụ và đơn đặt bàn đã hoàn thành."

**Alternative Sequences**
- **A1 – Already checked in:** "Đơn này đã được check-in trước đó."
- **A2 – Not today:** "Chỉ check-in được đơn có ngày đến là hôm nay."
- **A3 – Table busy:** "Bàn của đơn này đang có khách. Kiểm tra sơ đồ bàn hoặc đổi bàn cho khách."
- **A4 – No table assigned:** "Đơn chưa được gán bàn."
- **A5 – Not approved:** "Đơn không còn ở trạng thái đã duyệt."
- **A6 – Late arrival:** a reservation not checked in within 30 minutes of its time is cancelled automatically (see BR-02).

**Post conditions**
1. Status COMPLETED with arrival time.
2. Table status OCCUPIED.

**Business Rules**
- **BR-01 Walk-in tab:** the check-in screen also has a "walk-in" tab. **[Cần kiểm]** describe its flow before writing it.
- **BR-02 Grace period:** 30 minutes. The job runs every minute and cancels APPROVED reservations not checked in with reason "Tự động huỷ: khách đến muộn quá 30 phút" (`LateArrivalCleaner.java:15`, `ReservationDAO.java:515–524`). The old SRS said 15 minutes. **[Cần chốt]** Keep 30, or change the code.
- **BR-03 No-show:** the Receptionist can mark a reservation NO_SHOW manually (`ManageReservationsServlet.java:198`).

---

## UC21 - Update Table Status

**Created by:** Vũ Đức Dũng
**Primary Actor:** Manager
**Secondary Actor:** Receptionist (see BR-01)
**Description:** As a staff member, I want to set a table's status, so that the floor status is accurate.

**Input fields**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Table | – | Yes | Must exist | "Bàn cần cập nhật không hợp lệ." |
| 2 | New status | Choice | Yes | AVAILABLE, RESERVED, OCCUPIED or UNAVAILABLE, and the change must be allowed | "Không thể cập nhật trạng thái bàn; trạng thái chuyển đổi không hợp lệ hoặc bàn không còn tồn tại." |

**Main Sequence**
1. Manager opens the table layout (UC12).
2. Manager chooses a table and "Đổi trạng thái" (change status).
3. Manager chooses the new status and saves.
4. The system checks that the change is allowed and saves it.
5. The system shows "Đã cập nhật trạng thái bàn."

**Alternative Sequences**
- **A1 – Release table with an open order:** "Không thể bật lại bàn trống: bàn không ở trạng thái đang phục vụ hoặc vẫn còn order."
- **A2 – Invalid change:** the message in row 2.

**Post conditions**
1. The table status is saved.

**Business Rules**
- **BR-01 Who can change:** only Manager can change status (`ManageTablesServlet.java:165`). The code comment on the line above says staff can change status. **[Cần chốt]** Decide: allow Receptionist (as UC21 says), or keep Manager only and update UC21.
- **BR-02 Release rule:** a table with an open order cannot be set to free. Use the release action, which checks there is no open order.
- **BR-03 Paid table:** payment releases the table automatically (UC23).

---

## UC32 - Manage Tables

**Created by:** Nguyễn Thành Công
**Primary Actor:** Manager
**Secondary Actor:** None
**Description:** As a Manager, I want to add and edit tables, so that the layout and capacity are correct.

**Input fields – Table form**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Table name | Text, max 50 | Yes | Not empty; length ≤ 50 | "Tên bàn là bắt buộc và tối đa 50 ký tự." |
| 2 | Table type | Text, max 50 | Yes | Not empty; length ≤ 50 | "Loại bàn là bắt buộc và tối đa 50 ký tự." |
| 3 | Capacity | Whole number | Yes | 1 to 30 | "Sức chứa phải từ 1 đến 30 khách." |
| 4 | Note | Text, max 255 | No | Length ≤ 255 | "Ghi chú tối đa 255 ký tự." |
| 5 | Status | Choice | Yes | AVAILABLE, RESERVED, OCCUPIED, UNAVAILABLE | "Trạng thái bàn không hợp lệ." |
| 6 | Table name | – | – | Must be unique in the database | "Không thể lưu bàn. Mã bàn có thể bị trùng hoặc trạng thái chuyển đổi không hợp lệ." |

**Main Sequence – Add**
1. Manager opens "Quản lý bàn" (manage tables).
2. Manager clicks "Thêm bàn" (add table).
3. Manager fills in fields 1–5 and saves.
4. The system validates in the order of the table and saves.
5. The system shows the success message and refreshes the list.

**Main Sequence – Edit**
1. Manager clicks a table in the list.
2. The system shows the form filled with current values.
3. Manager changes fields and saves. Steps 4–5 as above.

**Alternative Sequences**
- **A1 – Validation fails:** the message from the table is shown.
- **A2 – Table not found:** "Bàn cần cập nhật không hợp lệ."

**Post conditions**
1. The table is saved and the layout shows it.

**Business Rules**
- **BR-01 Name uniqueness:** table names must be unique.
- **BR-02 Delete:** **not implemented.** The SRS says Manager can delete or deactivate a table. **[Cần chốt]** Remove delete from the SRS, or use status UNAVAILABLE as "deactivated" and say so in the text.
- **BR-03 Table in use:** **[Cần kiểm]** the SRS says an occupied or reserved table cannot be deleted. Not relevant until delete exists.

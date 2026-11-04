# Group 2 – Menu & Dishes

Nguồn đối chiếu: `src/java/controller/menu/MenuServlet.java`, `ManageDishesServlet.java`, `src/java/dao/DishDAO.java`, `src/java/utils/SearchUtils.java`, `src/java/utils/DishImageStore.java`.
Nội dung viết theo hành vi hiện tại của mã. Mục **[Cần chốt]** là chỗ nhóm phải quyết định.

---

## UC05 - View Menu

**Created by:** Trần Trọng Hiếu
**Primary Actor:** Guest, Customer
**Secondary Actor:** None
**Description:** As a Guest or Customer, I want to view the restaurant menu so that I can see the dishes, categories and prices.

**Preconditions**
1. The menu has at least one visible dish (status active).

**Input fields (URL filters, all optional)**

| # | Field | Type | Validation rule | Behaviour when invalid |
|---|---|---|---|---|
| 1 | Keyword | Text | Trimmed. Longer than 50 characters is cut to 50. Empty means no keyword. | Cut silently |
| 2 | Category | Number | Must be a number. Non-number is ignored (all categories). | Ignored silently |
| 3 | Sort by price | Choice | `asc`, `desc`, or none (newest first) | Other values: default order |
| 4 | Page | Number | Must be a number. Non-number is ignored (page 1). | Ignored silently |

**Main Sequence**
1. User opens the menu page.
2. The system loads the list of active categories.
3. The system loads visible dishes (status active, category active), applies the filters, and shows one page.
4. The system shows each dish with name, category, price, unit, description and image.
5. User selects a category, sorts, searches or changes page.

**Alternative Sequences**
- **A1 – No dish matches:** the list is empty. **[Cần chốt]** The current view does not show a special message. Decide the text to show, or write "empty list" in the SRS.
- **A2 – Hidden dish:** dishes set to hidden do not appear for Guest or Customer.

**Post conditions**
1. The user sees only visible dishes and active categories.

**Business Rules**
- **BR-01 Visibility:** a dish is visible when its status is active and its category is active.
- **BR-02 Out of stock:** not implemented. The system has no stock or "sold out" flag. Remove "out of stock disabled" from the SRS, or add a new flag as a future item.
- **BR-03 Member discount on menu:** not implemented in the menu. Member points are applied at checkout (see UC26).

---

## UC06 - Search Dishes

**Created by:** Trần Trọng Hiếu
**Primary Actor:** Guest, Customer
**Secondary Actor:** None
**Description:** As a Guest or Customer, I want to search for dishes by name so that I can find them quickly.

**Preconditions**
1. The menu has visible dishes.

**Input fields**

| # | Field | Type | Validation rule | Behaviour when invalid |
|---|---|---|---|---|
| 1 | Keyword | Text, max 50 | Trimmed. Longer than 50 characters is cut to 50. Empty means no search. | Cut silently |
| 2 | Category | Choice | Optional. Must be an existing category. | Ignored if not a number |
| 3 | Sort by price | Choice | Optional: low to high, or high to low | Other values: default order |

**Matching rule**
- The keyword is matched against the **dish name only**. Description and tags are not searched.
- Accents are ignored (for example "bo" matches "bò"), and the letter "đ" is treated as "d".
- Every word in the keyword must appear in the dish name. The order of words does not matter.

**Main Sequence**
1. User types a keyword in the search box on the menu page.
2. User optionally chooses a category or a price sort.
3. User submits the search.
4. The system returns the visible dishes whose name matches all words of the keyword.

**Alternative Sequences**
- **A1 – No match:** the result list is empty. **[Cần chốt]** The current view has no "no dishes found" text and no suggestion list. Add them to the UI, or write "empty list" in the SRS.
- **A2 – Keyword only spaces:** treated as no keyword. The full menu is shown.

**Post conditions**
1. The result list shows only visible dishes matching the keyword and filters.

**Business Rules**
- **BR-01 Special characters:** the keyword is used as a parameter in the database query (no SQL built from raw text). Search is done in memory after the query.
- **BR-02 Partial match:** a word matches if it appears anywhere in the dish name (for example "ga" matches "gà rán").

---

## UC31 - Manage Menu

**Created by:** Nguyễn Thành Công
**Primary Actor:** Manager
**Secondary Actor:** None
**Description:** As a Manager, I want to add, edit, hide and delete dishes so that the menu is up to date.

**Preconditions**
1. The Manager is logged in.
2. The Manager opens the "Manage menu" page.

**Input fields – Dish form**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Dish name | Text, max 150 | Yes | Trimmed; not empty; length ≤ 150 | "Tên món là bắt buộc." / "Tên món không được quá 150 ký tự." |
| 2 | Category | Choice | Yes | Must select a category | "Vui lòng chọn danh mục." |
| 3 | Price | Number (VND) | Yes | Must be a number | "Giá không hợp lệ (chỉ nhập số, VD: 145000)." |
| 4 | Price | Number (VND) | Yes | Greater than 0 | "Giá phải lớn hơn 0." |
| 5 | Price | Number (VND) | Yes | At most 999,999,999 | "Giá quá lớn." |
| 6 | Unit | Text, max 50 | No | Length ≤ 50 (for example: phần, đĩa, tô) | "Đơn vị tính không được quá 50 ký tự." |
| 7 | Description | Text, max 2000 | No | Length ≤ 2000 | "Mô tả không được quá 2000 ký tự." |
| 8 | Image | File | Yes on create; optional on edit | Type: JPG, PNG, WEBP or GIF (checked from file content). Size ≤ 5 MB. | "Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF." / "Ảnh không được lớn hơn 5 MB." |
| 9 | Image (create) | – | – | An image must be chosen when creating | "Vui lòng chọn ảnh cho món ăn." |
| 10 | Featured | Checkbox | No | Yes or no | – |
| 11 | Status | Choice | No | Visible (default) or Hidden | – |

On edit, if no new image is chosen, the old image is kept. The Manager can also tick "remove image".

**Main Sequence – Add a dish**
1. Manager clicks "Thêm món" (Add dish).
2. The system shows the empty form (fields 1–11).
3. Manager fills in the form and clicks save.
4. The system validates in the order of the table.
5. The system saves the dish and shows "Đã thêm món "<name>" vào thực đơn."

**Main Sequence – Edit a dish**
1. Manager clicks a dish in the list.
2. The system shows the form filled with current values.
3. Manager changes fields and clicks save.
4. The system validates and saves. Shows "Đã cập nhật món "<name>"."

**Main Sequence – Hide or show a dish**
1. Manager clicks the status toggle for a dish.
2. The system changes the status and shows "Món đã hiển thị trên thực đơn." or "Đã ẩn món khỏi thực đơn."

**Main Sequence – Delete a dish**
1. Manager clicks "Xoá" (Delete) and confirms.
2. If the dish has never been ordered, the system deletes it and shows "Đã xoá món khỏi thực đơn."
3. If the dish is in an old order, the system hides it instead and shows "Món này đã có trong đơn hàng cũ nên không xoá hẳn được — đã chuyển sang ẨN (khách không còn thấy)."

**Alternative Sequences**
- **A1 – Validation fails:** the system shows the first failed message, keeps the entered values, and waits for correction.
- **A2 – Save error:** "Có lỗi khi lưu dữ liệu. Vui lòng thử lại."
- **A3 – Session token expired:** "Phiên làm việc đã hết hạn, vui lòng tải lại trang." The form is not saved.
- **A4 – Dish not found (toggle or delete):** "Không tìm thấy món." / "Không tìm thấy món cần xoá."
- **A5 – Cancel:** the Manager leaves the form without saving. Nothing changes.

**Post conditions**
1. The dish list shows the new or changed dish.
2. A hidden dish no longer appears on the customer menu (UC05).

**Business Rules**
- **BR-01 Price:** whole number, greater than 0, at most 999,999,999 VND.
- **BR-02 Name uniqueness:** **[Cần chốt]** The code does not check for duplicate dish names. The SRS currently says "unique within the same category". Choose: add the check, or change the SRS.
- **BR-03 Delete vs hide:** a dish that appears in any old order is hidden, not deleted. This keeps sales history.
- **BR-04 Images:** stored on the server. The file type is checked from its content (not only the name). Maximum 5 MB.
- **BR-05 Access:** only Manager can change the menu. Other roles get a 403 error.
- **BR-06 Out of stock:** not implemented (see UC05 BR-02).

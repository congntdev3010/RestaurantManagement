# Group 1 – Authentication & Account Management

Nguồn đối chiếu: `src/java/controller/user/*`, `src/java/dao/UserDAO.java`, `web/WEB-INF/views/auth/register.jsp`.
Nội dung UC viết theo hành vi hiện tại của mã. Mục đánh dấu **[Cần chốt]** là chỗ nhóm phải quyết định.

---

## UC01 - Register Account

**Created by:** Trương Khánh Đông
**Primary Actor:** Guest
**Secondary Actor:** Email service (SMTP) – sends the OTP email
**Description:** As a Guest, I want to register a new account so that I can access member features, earn loyalty points, and make reservations.

**Preconditions**
1. The user is not logged in.
2. The registration page is accessible.

**Input fields**

| # | Field | Type | Required | Validation rule (checked in this order) | Error message (exact text) |
|---|---|---|---|---|---|
| 1 | Full Name | Text, max 30 | Yes | Trimmed; not empty; length ≤ 30 | "Họ và tên không được để trống và tối đa 30 ký tự!" |
| 2 | Username | Text, max 30 | Yes | Trimmed; not empty; length ≤ 30 | "Tên tài khoản không được để trống và tối đa 30 ký tự!" |
| 3 | Username | – | – | Must not already exist | "Tên tài khoản đã tồn tại, vui lòng chọn tên khác!" |
| 4 | Email | Email, max 50 | Yes | Trimmed; matches `name@domain.tld`; length ≤ 50 | "Email không hợp lệ hoặc dài quá 50 ký tự!" |
| 5 | Email | – | – | Must not already exist | "Email này đã được đăng ký!" |
| 6 | Phone Number | Digits, 10 | Yes | Trimmed; exactly 10 digits; starts with 0 | "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số!" |
| 7 | Password | Password, 8–30 | Yes | Length 8 to 30 characters; not trimmed | "Mật khẩu phải từ 8 đến 30 ký tự!" |
| 8 | Confirm Password | Password | Yes | Must equal Password exactly | "Mật khẩu nhập lại không khớp!" |

Notes:
- Browser checks run first: required fields, Phone pattern `0[0-9]{9}`, and password min/max length. The server re-checks everything.
- If a field is empty on the server, the message for that field is shown (for example, an empty Email shows the Email message).
- On any error the form is shown again with one error message at the top. Full Name, Username, Email and Phone keep their values. Password and Confirm Password are cleared.

**Main Sequence**
1. Guest clicks "Register" in the header.
2. The system displays the registration form with fields 1–8.
3. Guest fills in the fields.
4. Guest clicks "Tạo tài khoản" (Create account).
5. The system validates the fields in the order listed above.
6. If all checks pass, the system generates a 6-digit OTP and emails it to the entered address.
7. The system keeps the pending registration in the user's session (not in the database) and redirects Guest to the OTP page.
8. Guest enters the OTP and submits.
9. The system compares the entered OTP with the one stored in the session.
10. If the OTP matches, the system saves the account to the database with role Customer and status Active, then redirects Guest to the Login page.

**Alternative Sequences**
- **A1 – Validation fails (step 5):** the system shows the message of the first failed rule, keeps the form, and waits for correction.
- **A2 – Email sending fails (step 6):** the system shows "Gửi mã OTP thất bại, vui lòng kiểm tra lại Email!" and keeps the form.
- **A3 – Wrong OTP (step 9):** the system shows "Mã OTP không chính xác!" on the OTP page. Guest can retry or resend.
- **A4 – Resend OTP:** the system generates a new OTP, replaces the old one in the session, and emails it. If sending fails: "Gửi email thất bại. Vui lòng thử lại sau!" If it succeeds: "Mã OTP mới đã được gửi đến email của bạn!"
- **A5 – Pending data missing from session (step 9):** the system shows "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại!" and returns Guest to the registration form.
- **A6 – Database error when saving (step 10):** the system shows "Đã có lỗi xảy ra khi tạo tài khoản, vui lòng thử lại!" on the OTP page. The pending data stays in the session so Guest can retry.

**Post conditions**
1. A new row exists in the Users table with role Customer and status Active. It is created only after a correct OTP.
2. The new Customer can log in on the Login page with username or email and password. Login by phone number is not supported.

**Business Rules**
- **BR-01 Required fields:** Full Name, Username, Email, Phone Number, Password and Confirm Password are required.
- **BR-02 Trimming:** Full Name, Username, Email and Phone Number are trimmed before validation. Password fields are not trimmed.
- **BR-03 Username uniqueness:** Username must be unique across all accounts.
- **BR-04 Email uniqueness and format:** Email must match the format in row 4 and be unique across all accounts.
- **BR-05 Phone format:** Phone Number must be exactly 10 digits and start with 0. **[Cần chốt]** The current code does not check whether a phone number is already used, and the Users table has no unique constraint on phone. Choose one: (a) keep as is and state "phone numbers may repeat", or (b) add a duplicate check.
- **BR-06 Password length:** Password must be 8 to 30 characters. There is no complexity rule.
- **BR-07 OTP verification:** The system sends a 6-digit OTP to the entered email. The account is created only after the OTP matches. **[Cần chốt]** The code does not check the 5-minute validity of the OTP (the timestamp is stored but not read). Current behaviour: the OTP is valid until the session expires. Choose one: (a) add a 5-minute check, or (b) write "valid until the session expires".
- **BR-08 Default role and status:** Accounts registered from the public site get role Customer and status Active.
- **BR-09 Password storage:** Out of scope for this release. Passwords are stored as entered. **[Cần chốt]** Keep this line as a scope note, or delete it.

---

## UC02 - Log into System

**Created by:** Trương Khánh Đông
**Primary Actor:** System User (Customer, Server, Receptionist, Chef, Manager, Admin)
**Secondary Actor:** None
**Description:** As a System User, I want to log into the system so that I can access the features of my role.

**Preconditions**
1. The user has an existing account.
2. The user is not logged in.

**Input fields**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Account | Text | Yes (browser `required`) | Username **or** email. Phone number is not accepted. | Same generic message as row 2 |
| 2 | Password | Password | Yes (browser `required`) | Compared exactly with the stored password | "Tài khoản/Mật khẩu không đúng hoặc chưa được kích hoạt!" |

The server does not check the format of the account field. The database lookup decides.

**Main Sequence**
1. User clicks "Login" in the header.
2. The system displays the login form.
3. User enters Account and Password.
4. User clicks the login button.
5. The system looks up an account whose username or email matches Account, the password matches, and the account is active and not deleted.
6. If found, the system stores the user in the session and redirects to `/home`.

**Alternative Sequences**
- **A1 – Wrong account or password (step 5):** the system shows "Tài khoản/Mật khẩu không đúng hoặc chưa được kích hoạt!" and keeps the form.
- **A2 – Account is locked or deleted (step 5):** if the user is redirected from another page, the system shows "Tài khoản đã bị khóa hoặc xóa. Vui lòng liên hệ quản trị viên." (Shown when the session user is found inactive on the next request.)
- **A3 – Forgot password:** see EX-01.
- **A4 – Google login:** see EX-02.

**Post conditions**
1. The session holds the logged-in user.
2. Every user is redirected to `/home` after login. Role-based landing pages are not implemented in the current version. **[Cần chốt]** Keep as "all roles go to /home" or implement role routing.

**Business Rules**
- **BR-01 Authentication fields:** username or email with password. Phone is not supported.
- **BR-02 Generic failure message:** the same message is shown for an unknown account and a wrong password.
- **BR-03 Active accounts only:** only accounts with active status and not deleted can log in.
- **BR-04 Failed-attempt lockout:** not implemented. No counter for failed attempts and no temporary lock. Remove any reference to it from the SRS, or mark it as a future item.
- **BR-05 Session:** the login state is kept in a server-side session. JWT is not used. The session lifetime is the server default **[Cần kiểm tra trong web.xml]**.

---

## UC03 - Log out of System

**Created by:** Trương Khánh Đông
**Primary Actor:** System User (Customer, Server, Receptionist, Chef, Manager, Admin)
**Secondary Actor:** None
**Description:** As a System User, I want to log out so that I end my session and protect my account.

**Preconditions**
1. The user is logged in.

**Main Sequence**
1. User clicks "Logout" in the navigation bar or profile menu.
2. The system ends the session on the server.
3. The system redirects the user to `/home`.

**Alternative Sequences**
- **A1 – Session already expired:** the system still redirects to `/home`. No error is shown.

**Post conditions**
1. The session no longer holds a user.
2. Pages that require login redirect to the Login page.

**Business Rules**
- **BR-01 Session end:** logout ends the server session.
- **BR-02 Redirect target:** all roles are redirected to `/home`. Internal roles are not redirected to Login. **[Cần chốt]** Keep or change.
- **BR-03 Cache headers:** not set by the logout page. Some protected pages set `Cache-Control: no-store`. Do not claim full back-button protection in the SRS.

---

## UC04 - Manage Profile

**Created by:** Trương Khánh Đông
**Primary Actor:** System User (Customer, Server, Receptionist, Chef, Manager, Admin)
**Secondary Actor:** None
**Description:** As a System User, I want to view and update my profile so that my contact details stay accurate.

**Preconditions**
1. The user is logged in.

**Input fields – Profile information**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Full Name | Text, max 30 | Yes | Trimmed; not empty; length ≤ 30 | "Họ và tên là bắt buộc và tối đa 30 ký tự." |
| 2 | Phone Number | Digits, 10 | Yes | Trimmed; exactly 10 digits; starts with 0 | "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số." |

Email, username and role are shown but cannot be changed by this page.

**Input fields – Change password**

| # | Field | Type | Required | Validation rule (checked in this order) | Message when wrong |
|---|---|---|---|---|---|
| 1 | Current Password | Password | Yes | Not empty | "Vui lòng nhập mật khẩu hiện tại." |
| 2 | New Password | Password | Yes | Length 8 to 30 | "Mật khẩu mới phải có từ 8 đến 30 ký tự." |
| 3 | Confirm Password | Password | Yes | Equal to New Password | "Mật khẩu xác nhận không khớp." |
| 4 | New Password | – | – | Must differ from Current Password | "Mật khẩu mới phải khác mật khẩu hiện tại." |
| 5 | Current Password | – | – | Must match the stored password | "Mật khẩu hiện tại không đúng hoặc không thể cập nhật." |

On success: "Đã đổi mật khẩu thành công."

**Main Sequence – Update profile**
1. User opens "Profile" from the user menu.
2. The system shows the current Full Name and Phone Number.
3. User edits Full Name and/or Phone Number and clicks save.
4. The system validates rows 1–2 of the profile table.
5. The system updates the database and updates the name and phone in the session.
6. The system shows "Đã cập nhật thông tin cá nhân."

**Main Sequence – Change password**
1. User opens the password section of the profile page.
2. User fills in the five fields and clicks save.
3. The system validates in the order of the password table.
4. The system updates the password and shows "Đã đổi mật khẩu thành công."

**Alternative Sequences**
- **A1 – Profile validation fails:** the system shows the message and keeps the form.
- **A2 – Database update fails:** "Không thể cập nhật thông tin. Vui lòng thử lại."
- **A3 – Password validation fails:** the system shows the message for the failed row and keeps the form.

**Post conditions**
1. The database row shows the new name and phone (profile) or the new password (password change).
2. The header and session show the new name and phone without logging in again.

**Business Rules**
- **BR-01 Read-only fields:** email, username and role cannot be changed on this page.
- **BR-02 Avatar:** not implemented. Do not include an avatar field in the SRS for this release.
- **BR-03 Phone:** the same format as UC01 row 6. Duplicate phone numbers are not checked (see UC01 BR-05).
- **BR-04 Password change:** requires the current password. The new password must differ from the current one.

---

## UC22 - Create Customer Account (Receptionist)

**Created by:** Vũ Đức Dũng
**Primary Actor:** Receptionist
**Secondary Actor:** Customer
**Status in current code:** **Not implemented.** No screen or action lets a Receptionist create a Customer account. Receptionists have no access to the user-management screen (see UC44 and UC36).

**Description:** As a Receptionist, I want to register a customer at the front desk so that walk-in guests can join the membership programme.

**Decision needed [Cần chốt]:**
- (a) Remove UC22 from this release, or
- (b) Implement it. The rules would be the same as UC01 (rows 1, 4, 6), with no password and no OTP, and status Active. Accounts created this way would have role Customer.

---

## EX-01 - Reset Forgotten Password (not in the original 46 UC)

**Primary Actor:** Guest
**Description:** As a Guest who has forgotten the password, I want to receive a new password by email.
**Location:** a modal on the Login page.

**Input fields**

| # | Field | Type | Required | Validation rule | Message when wrong |
|---|---|---|---|---|---|
| 1 | Email | Email | Yes | Trimmed; must exist in the system | "Email này chưa được đăng ký trong hệ thống!" |

**Main Sequence**
1. Guest clicks "Quên mật khẩu" on the Login page.
2. Guest enters the email and submits.
3. The system checks that the email exists.
4. The system generates a random 8-character password, replaces the stored password, and emails the new password.
5. The Login page shows "Hệ thống đã gửi mật khẩu mới qua email của bạn!"

**Alternative Sequences**
- **A1 – Email not found (step 3):** the modal stays open with the message from row 1.
- **A2 – Database update fails (step 4):** "Đã có lỗi xảy ra, vui lòng thử lại sau!" and the modal stays open.

**Business Rules**
- **BR-01:** the old password stops working as soon as the database is updated.
- **BR-02:** the system does not check whether the email was delivered. **[Cần chốt]** Note this as a known limit.

---

## EX-02 - Login with Google (not in the original 46 UC)

**Primary Actor:** Guest
**Description:** As a Guest, I want to sign in with my Google account.

**Main Sequence**
1. Guest clicks the Google sign-in button on the Login page.
2. The system sends Guest to Google and receives the result.
3. The system reads the email and name from Google.
4. If Google does not confirm the email, the system shows "Email Google chưa được xác minh."
5. If the email has no account, the system creates a Customer account from the Google email and name.
6. If the account is inactive, the system shows "Tài khoản đã bị khóa hoặc chưa được kích hoạt. Vui lòng liên hệ quản trị viên."
7. Otherwise the system logs Guest in and redirects to `/home`.

**Alternative Sequences**
- **A1 – Google not reachable:** "Không kết nối được tới Google. Vui lòng thử lại."
- **A2 – Account cannot be created:** "Không tạo được tài khoản. Vui lòng thử lại sau."

**Business Rules**
- **BR-01:** only Google accounts whose email is verified by Google are accepted.
- **BR-02:** the session ID is renewed at login to prevent session fixation.
- **BR-03 [Chưa đọc hết mã]:** the Customer account created from Google has no password. Confirm how a Google-only Customer changes password (UC04 requires a current password).

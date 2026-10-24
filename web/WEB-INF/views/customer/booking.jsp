<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đặt bàn - Việt À la carte</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/booking.css?v=1.0">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

        <main class="booking-page">
            <section class="booking-hero">
                <div class="booking-hero__content">
                    <p class="booking-eyebrow">RESERVATION</p>
                    <h1>Đặt bàn theo phong cách của bạn</h1>
                    <p class="booking-intro">Từ những buổi hẹn hò lãng mạn, tiệc gia đình, cho đến các sự kiện doanh nghiệp, chúng tôi luôn sẵn sàng mang đến trải nghiệm ẩm thực trọn vẹn.</p>
                </div>
                <div class="booking-hero__summary">
                    <span class="chip chip--gold"><i class="fa-solid fa-circle-check"></i> Đặt trước 24h</span>
                    <div class="summary-card">
                        <strong>18</strong>
                        <span>Bàn có sẵn hôm nay</span>
                    </div>
                </div>
            </section>

            <div class="booking-layout">
                <form class="booking-form" method="post" action="${pageContext.request.contextPath}/booking">
                    <c:if test="${not empty bookingError}">
                        <div class="booking-alert booking-alert--error" role="alert">
                            <i class="fa-solid fa-circle-exclamation"></i>
                            <span><c:out value="${bookingError}" /></span>
                        </div>
                    </c:if>

                    <c:if test="${not empty bookingMessage}">
                        <div class="booking-alert booking-alert--success" role="status">
                            <i class="fa-solid fa-circle-check"></i>
                            <span><c:out value="${bookingMessage}" /></span>
                        </div>
                    </c:if>

                    <div class="form-header">
                        <h2>Thông tin đặt bàn</h2>
                        <span>Nhân viên xác nhận nhanh chóng</span>
                    </div>

                    <div class="form-grid">
                        <label>
                            <span>Họ và tên</span>
                            <input type="text" name="fullName" value="${bookingFullName}" placeholder="Nguyễn Văn A" required>
                        </label>
                        <label>
                            <span>Số điện thoại</span>
                            <input type="tel" name="phone" value="${bookingPhone}" placeholder="0901 234 567" required>
                        </label>
                        <label>
                            <span>Email</span>
                            <input type="email" name="email" value="${bookingEmail}" placeholder="example@email.com">
                        </label>
                        <label>
                            <span>Số lượng khách</span>
                            <select name="guests" required>
                                <option value="">Chọn số lượng</option>
                                <option value="2" ${bookingGuests == '2' ? 'selected' : ''}>2 người</option>
                                <option value="4" ${bookingGuests == '4' ? 'selected' : ''}>4 người</option>
                                <option value="6" ${bookingGuests == '6' ? 'selected' : ''}>6 người</option>
                                <option value="8" ${bookingGuests == '8' ? 'selected' : ''}>8 người</option>
                                <option value="10" ${bookingGuests == '10' ? 'selected' : ''}>10 người</option>
                                <option value="12+" ${bookingGuests == '12+' ? 'selected' : ''}>12 người trở lên</option>
                            </select>
                        </label>
                        <label>
                            <span>Ngày</span>
                            <input type="date" name="date" value="${bookingDate}" required>
                        </label>
                        <label>
                            <span>Giờ</span>
                            <select name="time" required>
                                <option value="">Chọn giờ</option>
                                <option value="11:30" ${bookingTime == '11:30' ? 'selected' : ''}>11:30</option>
                                <option value="12:00" ${bookingTime == '12:00' ? 'selected' : ''}>12:00</option>
                                <option value="18:00" ${bookingTime == '18:00' ? 'selected' : ''}>18:00</option>
                                <option value="18:30" ${bookingTime == '18:30' ? 'selected' : ''}>18:30</option>
                                <option value="19:00" ${bookingTime == '19:00' ? 'selected' : ''}>19:00</option>
                                <option value="20:00" ${bookingTime == '20:00' ? 'selected' : ''}>20:00</option>
                            </select>
                        </label>
                        <label>
                            <span>Khu vực</span>
                            <select name="area">
                                <option value="">Mọi khu vực</option>
                                <option value="Cửa sổ" ${bookingArea == 'Cửa sổ' ? 'selected' : ''}>Cửa sổ</option>
                                <option value="Trong nhà" ${bookingArea == 'Trong nhà' ? 'selected' : ''}>Trong nhà</option>
                                <option value="Sân vườn" ${bookingArea == 'Sân vườn' ? 'selected' : ''}>Sân vườn</option>
                                <option value="Phòng riêng" ${bookingArea == 'Phòng riêng' ? 'selected' : ''}>Phòng riêng</option>
                            </select>
                        </label>
                    </div>

                    <label class="notes-field">
                        <span>Ghi chú</span>
                        <textarea name="notes" rows="4" placeholder="Ví dụ: Muốn bàn gần cửa sổ, sinh nhật, dịp kỷ niệm...">${bookingNotes}</textarea>
                    </label>

                    <div class="form-actions">
                        <button type="submit" class="primary-button">
                            <i class="fa-solid fa-calendar-check"></i>
                            Xác nhận đặt bàn
                        </button>
                        <button type="reset" class="secondary-button">Làm mới</button>
                    </div>
                </form>

                <aside class="booking-side-panel">
                    <div class="panel-box panel-box--highlight">
                        <p class="panel-label">Mã ưu đãi</p>
                        <h3>Early dinner set</h3>
                        <p>Giảm 10% cho đặt bàn trước 17:00, áp dụng cho nhóm từ 4 người trở lên.</p>
                    </div>

                    <div class="panel-box">
                        <h3>Điều kiện & dịch vụ</h3>
                        <ul>
                            <li><i class="fa-solid fa-check"></i> Đặt bàn tối thiểu 30 phút trước giờ tới</li>
                            <li><i class="fa-solid fa-check"></i> Hỗ trợ chỗ ngồi riêng cho trẻ em và người cao tuổi</li>
                            <li><i class="fa-solid fa-check"></i> Miễn phí đổi bàn trong 30 phút đầu</li>
                        </ul>
                    </div>

                    <div class="panel-box">
                        <h3>Phòng & khu vực</h3>
                        <div class="table-legend">
                            <span><i class="fa-solid fa-square" style="color:#38b26d"></i> Còn trống</span>
                            <span><i class="fa-solid fa-square" style="color:#f5b942"></i> Sắp đầy</span>
                            <span><i class="fa-solid fa-square" style="color:#d94b4b"></i> Đã đặt</span>
                        </div>
                        <div class="table-grid">
                            <div class="table-card table-card--available">
                                <strong>Bàn 01</strong>
                                <span>2 người</span>
                            </div>
                            <div class="table-card table-card--available">
                                <strong>Bàn 04</strong>
                                <span>4 người</span>
                            </div>
                            <div class="table-card table-card--warning">
                                <strong>Bàn 06</strong>
                                <span>6 người</span>
                            </div>
                            <div class="table-card table-card--booked">
                                <strong>Bàn 08</strong>
                                <span>8 người</span>
                            </div>
                            <div class="table-card table-card--available">
                                <strong>Bàn 09</strong>
                                <span>4 người</span>
                            </div>
                            <div class="table-card table-card--available">
                                <strong>Bàn 12</strong>
                                <span>10 người</span>
                            </div>
                        </div>
                    </div>
                </aside>
            </div>
        </main>
    </body>
</html>

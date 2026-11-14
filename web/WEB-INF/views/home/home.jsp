<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
    String roleName = user == null ? "" : user.getRoleName();
    boolean isRestaurantStaff = "Admin".equalsIgnoreCase(roleName)
        || "Manager".equalsIgnoreCase(roleName)
        || "Server".equalsIgnoreCase(roleName)
        || "Receptionist".equalsIgnoreCase(roleName)
        || "Chef".equalsIgnoreCase(roleName);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trang chủ - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/home.css?v=1.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        /* ===== Michelin ===== */
        .michelin-section { background: radial-gradient(circle at 15% 20%, #4a1620 0%, #2a1a16 55%, #1d110e 100%); color: #f3e9dc; padding: 88px 6vw; }
        .michelin-wrap { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: 1.15fr 1fr; gap: 64px; align-items: center; }
        .michelin-media { margin: 0; border-radius: 22px; overflow: hidden; border: 1px solid rgba(232, 184, 74, .35); box-shadow: 0 26px 56px rgba(0, 0, 0, .45); }
        .michelin-media img { display: block; width: 100%; height: auto; transition: transform .6s ease; }
        .michelin-media:hover img { transform: scale(1.03); }
        .michelin-text .eyebrow { margin: 0 0 12px; letter-spacing: .2em; text-transform: uppercase; font-size: 13px; color: #d9b37a; }
        .michelin-text h2 { margin: 0 0 18px; font-size: clamp(28px, 3.6vw, 44px); line-height: 1.15; color: #fff; }
        .michelin-stars { display: flex; gap: 8px; margin-bottom: 22px; }
        .michelin-stars i { color: #e8b84a; font-size: 32px; filter: drop-shadow(0 2px 8px rgba(232, 184, 74, .45)); }
        .michelin-text p { margin: 0 0 16px; line-height: 1.7; color: #d8c8b8; font-size: 16px; }
        .michelin-badge { display: inline-flex; align-items: center; gap: 10px; margin-top: 8px; padding: 10px 18px; border-radius: 999px; background: rgba(232, 184, 74, .12); border: 1px solid rgba(232, 184, 74, .45); color: #f0d59a; font-weight: 600; font-size: 14px; }
        @media (max-width: 860px) {
            .michelin-wrap { grid-template-columns: 1fr; gap: 36px; }
            .michelin-text { text-align: center; }
            .michelin-stars { justify-content: center; }
            .michelin-badge { margin-left: auto; margin-right: auto; }
        }
        .home-review { padding: 36px 6vw; background: #f4ebe0; text-align: center; }
        .home-review a { display: inline-flex; align-items: center; gap: 12px; padding: 15px 24px; border: 1px solid #d9b37a; border-radius: 999px; background: #fffdf9; color: #4a1620; text-decoration: none; font-weight: 700; box-shadow: 0 10px 28px rgba(74,22,32,.1); transition: transform .2s ease; }
        .home-review a:hover { transform: translateY(-2px); }
        .home-review .home-review-stars { color: #bd8424; letter-spacing: .08em; }

        /* ===== Không gian ===== */
        .space-section { padding: 88px 6vw; background: linear-gradient(180deg, #fbf6f0 0%, #f4ebe0 100%); }
        .space-head { text-align: center; margin: 0 auto 44px; max-width: 720px; }
        .space-head .eyebrow { margin: 0 0 10px; letter-spacing: .2em; text-transform: uppercase; font-size: 13px; color: #9c6720; }
        .space-head h2 { margin: 0 0 12px; font-size: clamp(28px, 3.4vw, 40px); color: #4a1620; }
        .space-head p { margin: 0; color: #7a6a64; line-height: 1.6; }
        .space-grid { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: 1.35fr 1fr; grid-template-rows: 1fr 1fr; gap: 18px; }
        .space-item { margin: 0; position: relative; border-radius: 20px; overflow: hidden; background: #3a2620; box-shadow: 0 14px 34px rgba(74, 22, 32, .14); min-height: 240px; }
        .space-item:first-child { grid-row: 1 / span 2; min-height: 480px; }
        .space-item img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .6s ease; }
        .space-item:hover img { transform: scale(1.06); }
        .space-item::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0, 0, 0, 0) 55%, rgba(0, 0, 0, .35) 100%); pointer-events: none; }
        @media (max-width: 860px) {
            .space-grid { grid-template-columns: 1fr; grid-template-rows: none; }
            .space-item:first-child { grid-row: auto; min-height: 300px; }
            .space-item { min-height: 240px; }
        }

        /* ===== Footer ===== */
        .site-footer { background: #64463d; color: #ffffff; padding: 48px 6vw 26px; font-size: 15px; line-height: 1.7; }
        .footer-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 36px; max-width: 1200px; margin: 0 auto; }
        .footer-brand { margin: 0 0 14px; font-size: clamp(22px, 2.6vw, 30px); font-weight: 800; color: #e6cba3; text-transform: uppercase; letter-spacing: .01em; }
        .footer-col h3 { margin: 0 0 12px; font-size: 22px; font-weight: 700; color: #e6cba3; }
        .footer-col p { margin: 0; }
        .footer-col strong { font-weight: 700; }
        .footer-divider { width: 40px; height: 2px; background: #ffffff; margin: 14px 0; }
        .footer-social { display: flex; gap: 12px; flex-wrap: wrap; }
        .footer-social a { width: 48px; height: 48px; border-radius: 50%; background: #ffffff; color: #64463d; display: inline-flex; align-items: center; justify-content: center; font-size: 22px; text-decoration: none; transition: transform .2s ease, background .2s ease; }
        .footer-social a:hover { transform: translateY(-2px); background: #e6cba3; }
        .footer-copy { max-width: 1200px; margin: 30px auto 0; padding-top: 18px; border-top: 1px solid rgba(255, 255, 255, .18); font-size: 13px; color: #d9c7b8; text-align: center; }

        @media (max-width: 760px) {
            .footer-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>

    <!-- Nhúng Header -->
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

    <!-- Hero Banner -->
    <section class="hero-section">
        <div class="hero-content">
            <% if (user != null) { %>
                <div class="welcome-badge">
                    <i class="fa-solid fa-crown"></i>
                    <span>Xin chào quý khách, <strong><%= (user.getFullName() != null && !user.getFullName().trim().isEmpty()) ? user.getFullName() : user.getUsername() %></strong>!</span>
                </div>
            <% } %>

            <p class="hero-subtitle">Mỹ vị Tinh hoa Việt</p>
            <h1 class="hero-title">Trải Nghiệm Ẩm Thực Á la carte Đẳng Cấp</h1>
            <p class="hero-desc">
                Hương vị truyền thống ba miền hòa quyện cùng nghệ thuật chế biến hiện đại. 
                Thưởng thức những món ăn được phục vụ tận tâm trong không gian ấm cúng.
            </p>
            
            <% if (!isRestaurantStaff) { %>
            <div class="hero-buttons">
                <a href="${pageContext.request.contextPath}/booking" class="btn-hero-primary">
                    <i class="fa-regular fa-calendar-check"></i> Đặt bàn ngay
                </a>
                <a href="${pageContext.request.contextPath}/menu" class="btn-hero-secondary">
                    <i class="fa-solid fa-utensils"></i> Xem Thực Đơn
                </a>
            </div>
            <% } %>
        </div>
    </section>

    <section class="home-review">
        <a href="${pageContext.request.contextPath}/review"><i class="fa-regular fa-comment-dots" aria-hidden="true"></i> Đánh giá nhà hàng <span class="home-review-stars">★</span> ${empty averageRating ? '0.0' : averageRating} · ${reviewCount} nhận xét</a>
    </section>

    <!-- Chứng nhận Michelin -->
    <section class="michelin-section">
        <div class="michelin-wrap">
            <figure class="michelin-media">
                <img src="${pageContext.request.contextPath}/assets/images/michelin.png"
                     alt="Việt À la carte vinh dự nhận 3 sao Michelin" loading="lazy">
            </figure>
            <div class="michelin-text">
                <p class="eyebrow">Chứng nhận ẩm thực</p>
                <h2>Nhà hàng đạt 3 sao Michelin</h2>
                <div class="michelin-stars" aria-label="3 sao Michelin">
                    <i class="fa-solid fa-star" aria-hidden="true"></i><i class="fa-solid fa-star" aria-hidden="true"></i><i class="fa-solid fa-star" aria-hidden="true"></i>
                </div>
                <p>Sự công nhận cao nhất dành cho những nhà hàng giữ được chất lượng món ăn nhất quán, đáng để thực khách đi xa tìm đến.</p>
                <p>Mỗi món ăn tại Việt À la carte được chuẩn bị tỉ mỉ từ nguyên liệu chọn lọc, giữ trọn hương vị truyền thống ba miền.</p>
                <span class="michelin-badge"><i class="fa-solid fa-award" aria-hidden="true"></i> Michelin Guide · 3 sao</span>
            </div>
        </div>
    </section>

    <!-- Không gian nhà hàng -->
    <section class="space-section">
        <div class="space-head">
            <p class="eyebrow">Không gian</p>
            <h2>Không gian nhà hàng</h2>
            <p>Ấm cúng, tinh tế và được chăm chút từng chi tiết để bạn thoải mái thưởng thức.</p>
        </div>
        <div class="space-grid">
            <figure class="space-item">
                <img src="${pageContext.request.contextPath}/assets/images/space/space-1.jpg" alt="Không gian nhà hàng 1" loading="lazy">
            </figure>
            <figure class="space-item">
                <img src="${pageContext.request.contextPath}/assets/images/space/space-2.jpg" alt="Không gian nhà hàng 2" loading="lazy">
            </figure>
            <figure class="space-item">
                <img src="${pageContext.request.contextPath}/assets/images/space/space-3.jpg" alt="Không gian nhà hàng 3" loading="lazy">
            </figure>
        </div>
    </section>

    <!-- Footer -->
    <%
        // Thông tin liên hệ: sửa các giá trị dưới đây cho đúng nhà hàng.
        String footerName = "VIỆT À LA CARTE";
        String footerAddress = "65/1 Quang Trung, Phường 9, Thành phố Đà Lạt";
        String footerHotline = "0378 301 004";
        String footerEmail = "vietalacarte.restaurant@gmail.com";
        String footerNote = "(Nhà hàng có chỗ để xe ô tô)";
        String hoursMorning = "Sáng: 10h - 14h30";
        String hoursEvening = "Chiều: 18h - 22h30";
        String hoursAll = "Tất cả các ngày trong tuần";
    %>
    <footer class="site-footer">
        <div class="footer-grid">
            <div class="footer-col">
                <h2 class="footer-brand"><%= footerName %></h2>
                <p>Địa chỉ: <strong><%= footerAddress %></strong></p>
                <p>Hotline: <%= footerHotline %></p>
                <div class="footer-divider"></div>
                <p>Email: <%= footerEmail %></p>
                <p><%= footerNote %></p>
            </div>
            <div class="footer-col">
                <h3>Giờ mở cửa</h3>
                <p><%= hoursMorning %></p>
                <p><%= hoursEvening %></p>
                <p><%= hoursAll %></p>
            </div>
            <div class="footer-col">
                <h3>Mạng xã hội</h3>
                <div class="footer-social">
                    <a href="#" aria-label="Facebook"><i class="fa-brands fa-facebook-f" aria-hidden="true"></i></a>
                    <a href="#" aria-label="YouTube"><i class="fa-brands fa-youtube" aria-hidden="true"></i></a>
                    <a href="#" aria-label="Instagram"><i class="fa-brands fa-instagram" aria-hidden="true"></i></a>
                    <a href="#" aria-label="TikTok"><i class="fa-brands fa-tiktok" aria-hidden="true"></i></a>
                </div>
            </div>
        </div>
        <div class="footer-copy">© <%= java.time.Year.now().getValue() %> Việt À la carte. Bảo lưu mọi quyền.</div>
    </footer>

</body>
</html>

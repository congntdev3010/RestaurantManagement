package controller.user;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Đích của error-page 404 trong web.xml.
 *
 * Mọi địa chỉ không tồn tại (ví dụ /linh-tinh) được đưa về trang chủ bằng một
 * lần CHUYỂN HƯỚNG THẬT (302) — thanh địa chỉ trên trình duyệt đổi thành /home,
 * chứ không giữ nguyên đường dẫn sai như khi dùng forward nội bộ.
 */
@WebServlet(name = "NotFoundServlet", urlPatterns = {"/error-404"})
public class NotFoundServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        redirectHome(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        redirectHome(request, response);
    }

    private void redirectHome(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Không chuyển hướng được nếu phần đầu của phản hồi đã gửi đi (thường là chưa).
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.sendRedirect(request.getContextPath() + "/home");
    }
}

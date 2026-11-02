package controller.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.DishImageStore;

/** Phục vụ ảnh món ăn đã upload: GET /uploads/dishes/<tên-file>. Ai cũng xem được (dùng ở trang Thực đơn). */
@WebServlet(name = "DishImageServlet", urlPatterns = {"/uploads/dishes/*"})
public class DishImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String info = request.getPathInfo();
        Path file = info == null ? null : DishImageStore.resolve(info.startsWith("/") ? info.substring(1) : info);
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String name = file.getFileName().toString();
        String type = name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp"
                : name.endsWith(".gif") ? "image/gif" : "image/jpeg";
        response.setContentType(type);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=604800"); // tên file ngẫu nhiên, không đổi nội dung
        response.setContentLengthLong(Files.size(file));
        Files.copy(file, response.getOutputStream());
    }
}

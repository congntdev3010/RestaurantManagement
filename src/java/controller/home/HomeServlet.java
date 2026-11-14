package controller.home;

import java.io.IOException;
import dao.ReviewDAO;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "HomeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (ReviewDAO dao = new ReviewDAO()) {
            request.setAttribute("averageRating", dao.averageRating());
            request.setAttribute("reviewCount", dao.approvedCount());
        } catch (SQLException ex) {
            getServletContext().log("Could not load restaurant rating", ex);
        }
        request.getRequestDispatcher("/WEB-INF/views/home/home.jsp").forward(request, response);
    }
}

package controller.customer;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "BookingServlet", urlPatterns = {"/booking"})
public class BookingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/customer/booking.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String fullName = trimToEmpty(request.getParameter("fullName"));
        String phone = trimToEmpty(request.getParameter("phone"));
        String email = trimToEmpty(request.getParameter("email"));
        String date = trimToEmpty(request.getParameter("date"));
        String time = trimToEmpty(request.getParameter("time"));
        String guests = trimToEmpty(request.getParameter("guests"));
        String area = trimToEmpty(request.getParameter("area"));
        String notes = trimToEmpty(request.getParameter("notes"));

        boolean hasError = fullName.isEmpty() || phone.isEmpty() || date.isEmpty() || time.isEmpty() || guests.isEmpty();

        if (hasError) {
            request.setAttribute("bookingError", "Vui lòng điền đầy đủ thông tin bắt buộc để xác nhận đặt bàn.");
            request.setAttribute("bookingFullName", fullName);
            request.setAttribute("bookingPhone", phone);
            request.setAttribute("bookingEmail", email);
            request.setAttribute("bookingDate", date);
            request.setAttribute("bookingTime", time);
            request.setAttribute("bookingGuests", guests);
            request.setAttribute("bookingArea", area);
            request.setAttribute("bookingNotes", notes);
            request.getRequestDispatcher("/WEB-INF/views/customer/booking.jsp").forward(request, response);
            return;
        }

        request.setAttribute("bookingMessage", "Yêu cầu đặt bàn của quý khách đã được ghi nhận. Nhân viên nhà hàng sẽ liên hệ xác nhận trong thời gian sớm nhất.");
        request.setAttribute("bookingFullName", fullName);
        request.setAttribute("bookingPhone", phone);
        request.setAttribute("bookingEmail", email);
        request.setAttribute("bookingDate", date);
        request.setAttribute("bookingTime", time);
        request.setAttribute("bookingGuests", guests);
        request.setAttribute("bookingArea", area);
        request.setAttribute("bookingNotes", notes);
        request.getRequestDispatcher("/WEB-INF/views/customer/booking.jsp").forward(request, response);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

package filter;

import feature.user.dao.UserDAO;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import model.User;

@WebFilter(filterName = "AccountStatusFilter", urlPatterns = "/*")
public class AccountStatusFilter implements Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (isStaticResource(httpRequest.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)
                || new UserDAO().isUserActive(((User) sessionUser).getUserId())) {
            chain.doFilter(request, response);
            return;
        }

        session.invalidate();
        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?reason=inactive");
    }

    private boolean isStaticResource(String path) {
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        return normalizedPath.startsWith("/assets/")
                || normalizedPath.startsWith("/css/")
                || normalizedPath.startsWith("/js/")
                || normalizedPath.startsWith("/images/")
                || normalizedPath.startsWith("/fonts/")
                || normalizedPath.endsWith(".css")
                || normalizedPath.endsWith(".js")
                || normalizedPath.endsWith(".png")
                || normalizedPath.endsWith(".jpg")
                || normalizedPath.endsWith(".jpeg")
                || normalizedPath.endsWith(".webp")
                || normalizedPath.endsWith(".svg")
                || normalizedPath.endsWith(".ico")
                || normalizedPath.endsWith(".woff")
                || normalizedPath.endsWith(".woff2")
                || normalizedPath.endsWith(".ttf");
    }
}
package controller.authen;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.User;

/**
 * Đăng nhập bằng tài khoản Google (OAuth 2.0 authorization code).
 * - /login/google: chuyển người dùng sang trang đăng nhập của Google.
 * - /login/google/callback: Google trả về "code"; đổi lấy access token, đọc email đã xác minh,
 *   rồi đăng nhập vào tài khoản có email đó (chưa có thì tạo tài khoản Customer).
 *
 * Cấu hình (biến môi trường hoặc -D): GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET,
 * GOOGLE_REDIRECT_URI (tuỳ chọn; mặc định http(s)://host/ctx/login/google/callback).
 */
@WebServlet(name = "GoogleLoginServlet", urlPatterns = {"/login/google", "/login/google/callback"})
public class GoogleLoginServlet extends HttpServlet {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String STATE_KEY = "googleOAuthState";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String clientId = config("GOOGLE_CLIENT_ID");
        String clientSecret = config("GOOGLE_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            fail(request, response, "Chưa cấu hình đăng nhập Google (thiếu GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET).");
            return;
        }
        if (request.getServletPath().endsWith("/callback")) {
            handleCallback(request, response, clientId, clientSecret);
        } else {
            redirectToGoogle(request, response, clientId);
        }
    }

    private void redirectToGoogle(HttpServletRequest request, HttpServletResponse response, String clientId)
            throws IOException {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession().setAttribute(STATE_KEY, state); // chống CSRF ở bước callback
        String url = AUTH_URL + "?response_type=code"
                + "&client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri(request))
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state)
                + "&prompt=select_account";
        response.sendRedirect(url);
    }

    private void handleCallback(HttpServletRequest request, HttpServletResponse response,
            String clientId, String clientSecret) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(STATE_KEY);
        if (session != null) session.removeAttribute(STATE_KEY);
        String state = request.getParameter("state");
        String code = request.getParameter("code");
        if (request.getParameter("error") != null || code == null || expected == null || !expected.equals(state)) {
            fail(request, response, "Đăng nhập Google bị hủy hoặc không hợp lệ. Vui lòng thử lại.");
            return;
        }

        String email;
        String name;
        try {
            String tokenBody = "code=" + enc(code) + "&client_id=" + enc(clientId)
                    + "&client_secret=" + enc(clientSecret)
                    + "&redirect_uri=" + enc(redirectUri(request)) + "&grant_type=authorization_code";
            String tokenJson = send(HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody)).timeout(Duration.ofSeconds(15)).build());
            String accessToken = jsonString(tokenJson, "access_token");
            if (accessToken == null) {
                fail(request, response, "Google không cấp quyền đăng nhập. Vui lòng thử lại.");
                return;
            }
            String info = send(HttpRequest.newBuilder(URI.create(USERINFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().timeout(Duration.ofSeconds(15)).build());
            email = jsonString(info, "email");
            name = jsonString(info, "name");
            // Chỉ tin email đã được Google xác minh.
            if (email == null || !info.matches("(?s).*\"email_verified\"\\s*:\\s*true.*")) {
                fail(request, response, "Email Google chưa được xác minh.");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(request, response, "Không kết nối được tới Google. Vui lòng thử lại.");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            fail(request, response, "Không kết nối được tới Google. Vui lòng thử lại.");
            return;
        }

        User user;
        try (UserDAO dao = new UserDAO()) {
            user = dao.findByEmail(email);
            if (user == null) {
                if (!dao.createGoogleCustomer(email, name)) {
                    fail(request, response, "Không tạo được tài khoản. Vui lòng thử lại sau.");
                    return;
                }
                user = dao.findByEmail(email);
            }
        }
        if (user == null || !user.isIsActive()) {
            fail(request, response, "Tài khoản đã bị khóa hoặc chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
            return;
        }
        request.changeSessionId(); // tránh session fixation
        request.getSession().setAttribute("user", user);
        response.sendRedirect(request.getContextPath() + "/home");
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/WEB-INF/views/authen/login.jsp").forward(request, response);
    }

    private String send(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) {
            throw new IOException("Google trả về HTTP " + res.statusCode());
        }
        return res.body();
    }

    /** Lấy giá trị chuỗi của một khóa trong JSON phẳng (đủ dùng cho phản hồi của Google). */
    static String jsonString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) return null;
        String raw = m.group(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                sb.append(c);
                continue;
            }
            char n = raw.charAt(++i);
            switch (n) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (i + 4 < raw.length()) {
                        sb.append((char) Integer.parseInt(raw.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    break;
                default: sb.append(n);
            }
        }
        return sb.toString();
    }

    private String redirectUri(HttpServletRequest request) {
        String configured = config("GOOGLE_REDIRECT_URI");
        if (configured != null) return configured;
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443);
        return request.getScheme() + "://" + request.getServerName() + (defaultPort ? "" : ":" + port)
                + request.getContextPath() + "/login/google/callback";
    }

    private static String config(String key) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

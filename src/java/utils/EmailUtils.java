package utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;
import java.security.SecureRandom;

public class EmailUtils {

    private static final String SENDER_EMAIL = "dongtkhe181473@fpt.edu.vn";
    private static final String APP_PASSWORD = "cbyr svgq gwnj sxui";

    /**
     * Hàm dùng chung để gửi Email (nhận vào Người nhận, Tiêu đề, Nội dung)
     */
    public static boolean sendEmail(String recipientEmail, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.mime.charset", "UTF-8");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
            );

            message.setSubject(subject, "UTF-8");
            message.setContent(content, "text/plain; charset=UTF-8");

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Sinh mã OTP 6 chữ số
    public static String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Gửi Mail chứa mã OTP
    public static boolean sendOTPEmail(String recipientEmail, String otpCode) {
        String subject = "Mã xác minh OTP - Việt À la carte";
        String content = "Mã OTP kích hoạt tài khoản của bạn là: " + otpCode
                + "\nMã có hiệu lực trong 5 phút.";
        
        return sendEmail(recipientEmail, subject, content);
    }

    // Sinh mật khẩu ngẫu nhiên 8 ký tự
    public static String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    // Gửi Email cấp mật khẩu mới (Chuẩn tiêu đề và nội dung)
    public static boolean sendNewPasswordEmail(String toEmail, String newPassword) {
        String subject = "Cấp lại mật khẩu - Việt À la carte";
        String content = "Xin chào,\n\n"
                + "Hệ thống đã cấp lại mật khẩu mới cho tài khoản của bạn:\n"
                + "Mật khẩu mới: " + newPassword + "\n\n"
                + "Vui lòng đăng nhập và đổi lại mật khẩu ngay để đảm bảo an toàn.\n\n"
                + "Trân trọng,\n"
                + "Nhà hàng Việt À la carte";

        return sendEmail(toEmail, subject, content);
    }
}
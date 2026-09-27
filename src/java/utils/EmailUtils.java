package utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailUtils {

    // Điền Gmail và App Password của bạn tại đây
    private static final String SENDER_EMAIL = "dongtkhe181473@fpt.edu.vn";
    private static final String APP_PASSWORD = "cbyr svgq gwnj sxui"; 

    // Sinh mã OTP 6 chữ sốRandom
    public static String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Gửi Mail chứa mã OTP
    public static boolean sendOTPEmail(String recipientEmail, String otpCode) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Mã xác minh OTP - Á la carte Restaurant");
            message.setText("Mã OTP kích hoạt tài khoản của bạn là: " + otpCode + "\nMã có hiệu lực trong 5 phút.");

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}

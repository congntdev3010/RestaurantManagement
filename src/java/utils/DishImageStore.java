package utils;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lưu ảnh món ăn do Manager tải lên.
 *
 * Ảnh KHÔNG ghi vào thư mục web đã deploy (build/web), vì "Clean and Build" của NetBeans sẽ xoá sạch.
 * Thay vào đó lưu ở thư mục riêng (mặc định: <thư mục người dùng>/RestaurantManagement-uploads/dishes,
 * đổi được bằng -Drestaurant.upload.dir=... hoặc biến môi trường RESTAURANT_UPLOAD_DIR) và phục vụ qua
 * servlet /uploads/dishes/*. Trong CSDL cột image lưu dạng "uploads/dishes/<tên>.jpg".
 */
public final class DishImageStore {

    public static final String URL_PREFIX = "uploads/dishes/";
    public static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Pattern SAFE_NAME = Pattern.compile("[a-f0-9\\-]{36}\\.(jpg|png|webp|gif)");

    private DishImageStore() {
    }

    public static Path directory() throws IOException {
        String base = System.getProperty("restaurant.upload.dir");
        if (base == null || base.isBlank()) base = System.getenv("RESTAURANT_UPLOAD_DIR");
        if (base == null || base.isBlank()) {
            base = Paths.get(System.getProperty("user.home"), "RestaurantManagement-uploads").toString();
        }
        Path dir = Paths.get(base, "dishes").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }

    /** true nếu người dùng thật sự chọn file. */
    public static boolean hasFile(Part part) {
        return part != null && part.getSize() > 0
                && part.getSubmittedFileName() != null && !part.getSubmittedFileName().isBlank();
    }

    /**
     * Kiểm tra và lưu ảnh. Trả về đường dẫn lưu vào CSDL.
     * Ném IllegalArgumentException kèm thông báo tiếng Việt nếu file không hợp lệ.
     */
    public static String save(Part part) throws IOException {
        if (part.getSize() > MAX_BYTES) throw new IllegalArgumentException("Ảnh không được lớn hơn 5 MB.");
        byte[] head = new byte[12];
        int read;
        try (InputStream in = part.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        }
        // Xác định loại ảnh theo NỘI DUNG file, không tin phần đuôi tên file
        String ext = detectExtension(head, read);
        if (ext == null) throw new IllegalArgumentException("Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.");

        String name = UUID.randomUUID() + "." + ext;
        Path target = directory().resolve(name);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return URL_PREFIX + name;
    }

    static String detectExtension(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (n >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    /** Tìm file ảnh theo tên (đã chặn ../ và tên lạ). null nếu không hợp lệ/không có. */
    public static Path resolve(String fileName) throws IOException {
        if (fileName == null || !SAFE_NAME.matcher(fileName).matches()) return null;
        Path file = directory().resolve(fileName);
        return Files.isRegularFile(file) ? file : null;
    }

    /** Xoá file nếu đó là ảnh do hệ thống upload (không đụng ảnh mẫu trong assets). */
    public static void deleteIfUploaded(String imagePath) {
        if (imagePath == null || !imagePath.startsWith(URL_PREFIX)) return;
        try {
            Path file = resolve(imagePath.substring(URL_PREFIX.length()));
            if (file != null) Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}

package utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tiện ích tìm kiếm tiếng Việt không dấu.
 * VD: "canh ga chien" khớp "Cánh gà chiên nước mắm", "dau hu" khớp "Đậu hũ".
 */
public final class SearchUtils {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SearchUtils() {
    }

    /** Bỏ dấu, đổi đ→d, viết thường, gom mọi ký tự không phải chữ/số thành một dấu cách. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFD);
        s = MARKS.matcher(s).replaceAll("");
        s = s.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        s = NON_ALNUM.matcher(s).replaceAll(" ").trim();
        return s;
    }

    /**
     * true nếu MỌI từ trong từ khoá đều khớp với ĐẦU một từ trong văn bản (không phân biệt dấu, hoa thường,
     * thứ tự). VD "ga" khớp "Gà" nhưng không khớp "Ngâm"; "chi" khớp "Chiên" (gõ dở vẫn ra).
     * Từ khoá rỗng ⇒ luôn khớp.
     */
    public static boolean matches(String text, String keyword) {
        String key = normalize(keyword);
        if (key.isEmpty()) {
            return true;
        }
        String target = " " + normalize(text);
        for (String word : key.split(" ")) {
            if (!target.contains(" " + word)) {
                return false;
            }
        }
        return true;
    }
}

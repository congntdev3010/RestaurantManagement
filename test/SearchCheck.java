import utils.SearchUtils;

/** Kiểm nhanh tìm kiếm không dấu: javac -encoding UTF-8 ... rồi java SearchCheck */
public class SearchCheck {
    static int fail = 0;

    static void check(String name, String kw, boolean expected) {
        boolean got = SearchUtils.matches(name, kw);
        System.out.println((got == expected ? "OK   " : "FAIL ") + "[" + kw + "] => " + got);
        if (got != expected) fail++;
    }

    public static void main(String[] args) {
        String dish = "C\u00e1nh g\u00e0 chi\u00ean n\u01b0\u1edbc m\u1eafm";
        check(dish, "canh ga chien", true);
        check(dish, "C\u00e1nh G\u00e0 Chi\u00ean", true);
        check(dish, "CANH GA", true);
        check(dish, "ga chien nuoc mam", true);
        check(dish, "nuoc mam canh ga", true);
        check(dish, "  canh   ga  ", true);
        check(dish, "canh ga nuong", false);
        check(dish, "canh ga chi", true);
        check("N\u01b0\u1edbc S\u1ea5u Ng\u00e2m \u0110\u01b0\u1eddng H\u00e0 N\u1ed9i", "ga", false);
        check("\u0110\u1eadu h\u0169 s\u1ed1t c\u00e0 chua", "dau hu", true);
        check("Ph\u1edf b\u00f2 t\u00e1i", "pho", true);
        check("B\u00fan ch\u1ea3 H\u00e0 N\u1ed9i", "bun cha", true);
        check("B\u00fan ch\u1ea3 H\u00e0 N\u1ed9i", "", true);
        check(dish, java.text.Normalizer.normalize("c\u00e1nh g\u00e0", java.text.Normalizer.Form.NFD), true);
        System.out.println(fail == 0 ? "ALL PASSED" : fail + " FAILED");
        System.exit(fail);
    }
}

package com.teensconf.util;

import java.util.HashMap;
import java.util.Map;

public final class TransliterationUtil {

    private static final Map<Character, String> TRANSLIT_MAP = new HashMap<>();

    static {
        TRANSLIT_MAP.put('а', "a"); TRANSLIT_MAP.put('б', "b"); TRANSLIT_MAP.put('в', "v");
        TRANSLIT_MAP.put('г', "g"); TRANSLIT_MAP.put('д', "d"); TRANSLIT_MAP.put('е', "e");
        TRANSLIT_MAP.put('ё', "e"); TRANSLIT_MAP.put('ж', "zh"); TRANSLIT_MAP.put('з', "z");
        TRANSLIT_MAP.put('и', "i"); TRANSLIT_MAP.put('й', "y"); TRANSLIT_MAP.put('к', "k");
        TRANSLIT_MAP.put('л', "l"); TRANSLIT_MAP.put('м', "m"); TRANSLIT_MAP.put('н', "n");
        TRANSLIT_MAP.put('о', "o"); TRANSLIT_MAP.put('п', "p"); TRANSLIT_MAP.put('р', "r");
        TRANSLIT_MAP.put('с', "s"); TRANSLIT_MAP.put('т', "t"); TRANSLIT_MAP.put('у', "u");
        TRANSLIT_MAP.put('ф', "f"); TRANSLIT_MAP.put('х', "h"); TRANSLIT_MAP.put('ц', "ts");
        TRANSLIT_MAP.put('ч', "ch"); TRANSLIT_MAP.put('ш', "sh"); TRANSLIT_MAP.put('щ', "sch");
        TRANSLIT_MAP.put('ы', "y"); TRANSLIT_MAP.put('э', "e"); TRANSLIT_MAP.put('ю', "yu");
        TRANSLIT_MAP.put('я', "ya");
        TRANSLIT_MAP.put('А', "A"); TRANSLIT_MAP.put('Б', "B"); TRANSLIT_MAP.put('В', "V");
        TRANSLIT_MAP.put('Г', "G"); TRANSLIT_MAP.put('Д', "D"); TRANSLIT_MAP.put('Е', "E");
        TRANSLIT_MAP.put('Ё', "E"); TRANSLIT_MAP.put('Ж', "Zh"); TRANSLIT_MAP.put('З', "Z");
        TRANSLIT_MAP.put('И', "I"); TRANSLIT_MAP.put('Й', "Y"); TRANSLIT_MAP.put('К', "K");
        TRANSLIT_MAP.put('Л', "L"); TRANSLIT_MAP.put('М', "M"); TRANSLIT_MAP.put('Н', "N");
        TRANSLIT_MAP.put('О', "O"); TRANSLIT_MAP.put('П', "P"); TRANSLIT_MAP.put('Р', "R");
        TRANSLIT_MAP.put('С', "S"); TRANSLIT_MAP.put('Т', "T"); TRANSLIT_MAP.put('У', "U");
        TRANSLIT_MAP.put('Ф', "F"); TRANSLIT_MAP.put('Х', "H"); TRANSLIT_MAP.put('Ц', "Ts");
        TRANSLIT_MAP.put('Ч', "Ch"); TRANSLIT_MAP.put('Ш', "Sh"); TRANSLIT_MAP.put('Щ', "Sch");
        TRANSLIT_MAP.put('Ы', "Y"); TRANSLIT_MAP.put('Э', "E"); TRANSLIT_MAP.put('Ю', "Yu");
        TRANSLIT_MAP.put('Я', "Ya");
    }

    private TransliterationUtil() {}

    public static String transliterate(String text) {
        if (text == null) return "";

        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (TRANSLIT_MAP.containsKey(c)) {
                result.append(TRANSLIT_MAP.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else {
                result.append('_');
            }
        }

        return result.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
}

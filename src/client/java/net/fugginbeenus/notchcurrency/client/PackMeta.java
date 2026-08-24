package net.fugginbeenus.notchcurrency.client;

public final class PackMeta {

    private PackMeta() {}

    public static final int FORMAT =
            //? if >=26.2 {
            /*88;
            *///?} elif >=26.1 {
            /*84;
            *///?} elif >=1.21.11 {
            /*75;
            *///?} elif >=1.21 {
            /*34;
            *///?} else {
            15;
            //?}

    public static String json(String description) {
        String fields = FORMAT >= 82
                ? "\"min_format\": " + FORMAT + ",\n    \"max_format\": " + FORMAT
                : FORMAT >= 17
                ? "\"pack_format\": " + FORMAT + ",\n    \"supported_formats\": [" + FORMAT + ", " + FORMAT + "]"
                : "\"pack_format\": " + FORMAT;
        return "{\n  \"pack\": {\n    " + fields
                + ",\n    \"description\": \"" + description + "\"\n  }\n}\n";
    }
}

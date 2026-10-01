package com.orodent.tonv2.core.ui.editor;

/** Encodes Java strings as JavaScript string literals without evaluating user text as code. */
final class JavaScriptStringEncoder {
    private JavaScriptStringEncoder() {
    }

    static String quote(String value) {
        String text = value == null ? "" : value;
        StringBuilder result = new StringBuilder(text.length() + 2).append('"');
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\u2028' -> result.append("\\u2028");
                case '\u2029' -> result.append("\\u2029");
                default -> {
                    if (character < 0x20) {
                        result.append("\\u%04x".formatted((int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}

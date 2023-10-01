package org.openapifactory.api;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StringUtil {
    public static String toUpperCamelCase(String s) {
        return Character.toUpperCase(s.charAt(0)) + replaceUnderscoreWithCamelCase(s).substring(1);
    }

    public static String toLowerCamelCase(String s) {
        return Character.toLowerCase(s.charAt(0)) + replaceUnderscoreWithCamelCase(s).substring(1);
    }

    public static String replaceUnderscoreWithCamelCase(String s) {
        var result = new StringBuilder();
        var underscorePos = 0;
        int oldPos = 0;
        while ((underscorePos = s.indexOf('_', oldPos)) != -1) {
            result.append(s.substring(oldPos, underscorePos));
            result.append(Character.toUpperCase(s.charAt(underscorePos + 1)));
            oldPos = underscorePos + 2;
        }
        result.append(s.substring(oldPos));
        return result.toString();
    }

    public static <T> String lines(Collection<T> list, Function<T, String> fn) {
        return join("\n", list, fn);
    }

    public static <T> String join(Collection<T> list, Function<T, String> fn) {
        return join("", list, fn);
    }

    public static <T> String indent(int indent, Collection<T> list, Function<T, String> fn) {
        return list.stream().map(fn).collect(Collectors.joining("")).indent(indent);
    }

    public static <T> String join(String delimiter, Collection<T> list, Function<T, String> fn) {
        return list.stream().map(fn).collect(Collectors.joining(delimiter));
    }
}

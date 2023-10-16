package org.openapifactory.api;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            result.append(s, oldPos, underscorePos);
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

    public static final StringTemplate.Processor<String, RuntimeException> INDENT = StringUtil::indentProcessor;


    private static String indentProcessor(StringTemplate stringTemplate) {
        var fragments = Objects.requireNonNull(stringTemplate.fragments(), "fragments must not be null");
        var values = Objects.requireNonNull(stringTemplate.values(), "values must not be null");
        if (fragments.size() != values.size() + 1) {
            throw new IllegalArgumentException("fragments must have one more element than values");
        }
        var result = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            var prevFragment = fragments.get(i);

            String indent;
            int previousNewline = prevFragment.lastIndexOf('\n');
            if (previousNewline >= 0 && prevFragment.substring(previousNewline).matches("\\s*")) {
                indent = prevFragment.substring(previousNewline);
                prevFragment = prevFragment.substring(0, previousNewline);
            } else if (i == 0 && prevFragment.matches("^\\s*$")) {
                indent = fragments.get(i);
                result.append(
                        asStream(values.get(i)).flatMap(s -> ((CharSequence)s).toString().lines())
                                .map(s -> indent + s).collect(Collectors.joining("\n"))
                );
                continue;
            } else {
                indent = "";
            }
            result.append(prevFragment);
            asStream(values.get(i))
                    .flatMap(s -> ((String)s).lines())
                    .map(s -> indent + s)
                    .forEach(result::append);
        }
        result.append(fragments.get(fragments.size()-1));
        return result.toString();
    }

    private static Stream<?> asStream(Object o) {
        return switch (o) {
            case Optional<?> opt -> opt.stream();
            case Stream<?> stream -> stream;
            case Collection<?> collection -> collection.stream();
            case null, default -> Optional.ofNullable(o).stream();
        };
    }
}

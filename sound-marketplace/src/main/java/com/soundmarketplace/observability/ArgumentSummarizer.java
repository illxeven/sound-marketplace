package com.soundmarketplace.observability;

import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public final class ArgumentSummarizer {

    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "token", "secret", "clientsecret", "ephemeralkey",
            "authorization", "cardnumber", "cvc", "email"
    );

    private ArgumentSummarizer() {}

    public static boolean isSensitiveName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String sensitive : SENSITIVE_KEYWORDS) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    public static String summarize(Object obj) {
        return summarize(null, obj, 0);
    }

    public static String summarize(String paramName, Object obj) {
        return summarize(paramName, obj, 0);
    }

    private static String summarize(String name, Object obj, int depth) {
        if (isSensitiveName(name)) {
            return "***";
        }
        if (obj == null) {
            return "null";
        }
        if (obj instanceof Number || obj instanceof Boolean || obj.getClass().isEnum()) {
            return obj.toString();
        }
        if (obj instanceof String str) {
            if (str.length() > 64) {
                return str.substring(0, 61) + "...";
            }
            return "\"" + str + "\"";
        }
        if (obj instanceof byte[] bytes) {
            return "<bytes:" + bytes.length + ">";
        }
        if (obj instanceof MultipartFile file) {
            return String.format("<file:%s,%d,%s>", file.getOriginalFilename(), file.getSize(), file.getContentType());
        }
        if (obj instanceof Collection<?> coll) {
            return "<list:" + coll.size() + ">";
        }
        if (obj instanceof Map<?, ?> map) {
            return "<map:" + map.size() + ">";
        }

        if (obj instanceof Optional<?> opt) {
            return opt.map(v -> summarize(name, v, depth)).orElse("empty");
        }

        if (obj instanceof org.springframework.http.ResponseEntity<?> re) {
            return "<ResponseEntity:" + re.getStatusCode().value() + ",body=" + summarize("body", re.getBody(), depth + 1) + ">";
        }

        // Entity check: entity with getId()
        Class<?> clazz = obj.getClass();
        if (clazz.getPackageName().contains(".domain")) {
            try {
                java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                Object id = getIdMethod.invoke(obj);
                return clazz.getSimpleName() + "#" + (id != null ? id : "null");
            } catch (Throwable ignored) {
                return clazz.getSimpleName();
            }
        }

        // DTOs or custom objects: summarize fields at depth < 2 (only for app domain/dto packages)
        if (depth < 2 && clazz.getName().startsWith("com.soundmarketplace.")) {
            String className = clazz.getSimpleName();
            StringJoiner joiner = new StringJoiner(", ", className + "{", "}");
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object val = field.get(obj);
                    joiner.add(fieldName + "=" + summarize(fieldName, val, depth + 1));
                } catch (Throwable e) {
                    joiner.add(field.getName() + "=<error>");
                }
            }
            return joiner.toString();
        }

        try {
            return obj.toString();
        } catch (Throwable t) {
            return clazz.getSimpleName();
        }
    }
}

package org.twelve.gcp.common;

import com.sun.xml.ws.developer.Serialization;
import lombok.SneakyThrows;
import org.twelve.gcp.outline.Outline;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Tool {
    public static boolean hasSerializationAnnotation(Class<?> clazz) {
        Class<? extends Annotation> annotation = Serialization.class;
        while (clazz != null) {
            if (clazz.isAnnotationPresent(annotation)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    public static boolean hasSerializationAnnotation(Method method) {
        if (method.getParameterCount() > 0) return false;
        Class<?> clazz = method.getClass();
        Class<? extends Annotation> annotation = Serialization.class;
        String methodName = method.getName();
        Class<?>[] parameterTypes = null;
        while (clazz != null) {
            try {
                if (parameterTypes == null) {
                    parameterTypes = method.getParameterTypes();
                } else {
                    method = clazz.getDeclaredMethod(methodName, parameterTypes);
                }
                if (method.isAnnotationPresent(annotation)) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                // Continue to the superclass if method is not found
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    public static String manageSerializedName(String methodName) {
        String name = methodName;
        if (name.startsWith("get")) {
            name = name.substring(3);
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        return name;
    }

    /**
     * must have serialization annotation
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> serializeAnnotated(Object obj) {
        Map<String, Object> data = new HashMap<>();
        for (Method method : obj.getClass().getMethods()) {
            if (Tool.hasSerializationAnnotation(method)) {
                data.put(Tool.manageSerializedName(method.getName()), serializeMethod(method, obj));
            }
        }
        return data;
    }

    private static Object serializeNormal(Object obj) {
        if (obj == null) {
            return null;
        }
        // Primitive or Wrapper types, String, BigDecimal, BigInteger, Date/Time
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean
                || obj instanceof BigDecimal || obj instanceof BigInteger
                || obj instanceof LocalDate || obj instanceof LocalDateTime) {
            return obj;
        }
        // Enum
        if (obj instanceof Enum<?>) {
            Enum<?> enumValue = (Enum<?>) obj;
            return enumValue.name();
        }

        // Optional
        if (obj instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) obj;
            if (optional.isPresent()) return serialize(optional.get());
            else return null;
        }

        // Array
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            Object[] objs = new Object[length];
            for (int i = 0; i < length; i++) {
                objs[i] = serialize(Array.get(obj, i));
            }
            return objs;
        }

        // Collection (List, Set, etc.)
        if (obj instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) obj;
            List list = new ArrayList<>();
            for (Object item : collection) {
                list.add(serialize(item));
            }
            return list;
        }

        Map<String, Object> map = new HashMap<>();
        // Map
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> originalMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                map.put(entry.getKey().toString(), serialize(entry.getValue()));
            }
            return map;
        }

        // General Object with Fields
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    map.put(field.getName(), serialize(field.get(obj)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        }

        return map;

    }

    public static Outline getExactNumberOutline(Outline left, Outline right) {
        if(!(left.is(right)||right.is(left))){
            return Outline.Error;
        }
        return left.is(right)?right:left;
    }

    private static Object serialize(Object obj) {
        if (hasSerializationAnnotation(obj.getClass())) {
            return serializeAnnotated(obj);
        } else {
            return serializeNormal(obj);
        }
    }

    @SneakyThrows
    public static Object serializeMethod(Method method, Object obj) {
        method.setAccessible(true);
        return serialize(method.invoke(obj));
    }

    public static <T> T cast(Object object){
        return (T)object;
    }
}

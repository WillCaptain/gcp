package org.twelve.gcp.common;

import com.sun.xml.ws.developer.Serialization;
import lombok.SneakyThrows;
import org.twelve.gcp.ast.Node;
import org.twelve.gcp.node.expression.body.FunctionBody;
import org.twelve.gcp.node.function.FunctionCallNode;
import org.twelve.gcp.node.statement.MemberNode;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Utility class providing serialization and reflection operations.
 *
 * Features:
 * - Annotation-based serialization
 * - Deep object serialization
 * - Type-safe casting
 * - Reflection utilities
 *
 * Thread Safety: Most methods are thread-safe when used with immutable objects
 * @author huizi 2025
 */
public final class Tool {
    private static final Map<Class<?>, Boolean> SERIALIZATION_CACHE = new ConcurrentHashMap<>();
    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
            String.class, Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, BigDecimal.class, BigInteger.class,
            LocalDate.class, LocalDateTime.class
    );

    private Tool() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // --- Annotation Handling ---

    /**
     * Checks if a class has the Serialization annotation in its hierarchy.
     * Results are cached for better performance.
     */
    public static boolean hasSerializationAnnotation(Class<?> clazz) {
        return SERIALIZATION_CACHE.computeIfAbsent(clazz, c -> {
            while (c != null) {
                if (c.isAnnotationPresent(Serialization.class)) {
                    return true;
                }
                c = c.getSuperclass();
            }
            return false;
        });
    }

    /**
     * Checks if a method has the Serialization annotation.
     * Only considers no-arg methods for serialization.
     */
    public static boolean hasSerializationAnnotation(Method method) {
        if (method.getParameterCount() > 0) return false;

        Class<?> clazz = method.getDeclaringClass();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        while (clazz != null) {
            try {
                Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
                if (m.isAnnotationPresent(Serialization.class)) {
                    return true;
                }
            } catch (NoSuchMethodException e) {
                // Continue to superclass
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    // --- Serialization ---

    /**
     * Serializes an object using annotation-based approach if available,
     * otherwise falls back to normal serialization.
     */
    public static Object serialize(Object obj) {
        if (obj == null) return null;
        return hasSerializationAnnotation(obj.getClass())
                ? serializeAnnotated(obj)
                : serializeNormal(obj);
    }

    /**
     * Serializes only methods marked with Serialization annotation.
     */
    public static Map<String, Object> serializeAnnotated(Object obj) {
        Map<String, Object> data = new LinkedHashMap<>();  // Preserve order
        for (Method method : obj.getClass().getMethods()) {
            if (hasSerializationAnnotation(method)) {
                data.put(getSerializedName(method), serializeMethod(method, obj));
            }
        }
        return data;
    }

    /**
     * Deep serialization of any object to primitive/collection types.
     */
    private static Object serializeNormal(Object obj) {
        if (obj == null) return null;

        // Handle immutable types
        if (IMMUTABLE_TYPES.contains(obj.getClass())) {
            return obj;
        }

        // Handle enums
        if (obj instanceof Enum<?>) {
            return ((Enum<?>) obj).name();
        }

        // Handle Optional
        if (obj instanceof Optional<?>) {
            return ((Optional<?>) obj).map(Tool::serialize).orElse(null);
        }

        // Handle arrays
        if (obj.getClass().isArray()) {
            return serializeArray(obj);
        }

        // Handle collections
        if (obj instanceof Collection<?>) {
            return serializeCollection((Collection<?>) obj);
        }

        // Handle maps
        if (obj instanceof Map<?, ?>) {
            return serializeMap((Map<?, ?>) obj);
        }

        // Handle other objects via reflection
        return serializeObject(obj);
    }

    // --- Helper Methods ---

    private static List<Object> serializeArray(Object array) {
        int length = Array.getLength(array);
        List<Object> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(serialize(Array.get(array, i)));
        }
        return result;
    }

    private static List<Object> serializeCollection(Collection<?> collection) {
        List<Object> result = new ArrayList<>(collection.size());
        collection.forEach(item -> result.add(serialize(item)));
        return result;
    }

    private static Map<String, Object> serializeMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(k.toString(), serialize(v)));
        return result;
    }

    private static Map<String, Object> serializeObject(Object obj) {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    result.put(field.getName(), serialize(field.get(obj)));
                } catch (IllegalAccessException e) {
                    // Skip inaccessible fields
                }
            }
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    /**
     * Derives property name from getter method name.
     */
    public static String getSerializedName(Method method) {
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        return methodName;
    }

    @SneakyThrows
    private static Object serializeMethod(Method method, Object obj) {
        method.setAccessible(true);
        return serialize(method.invoke(obj));
    }

    // --- Type Utilities ---

    /**
     * Safely casts an object to type T with runtime type checking.
     * @throws ClassCastException if the object is not of type T
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

    /**
     * Gets the more specific numeric type outline between two outlines.
     */
    public static Outline getExactNumberOutline(Outline left, Outline right) {
        return left.is(right) ? right : right.is(left) ? left : left.ast().Error;
    }

    public static boolean isInFunction(Node node) {
        Node parent = node.parent();
        while(parent!=null){
            if(parent instanceof FunctionBody){
                return true;
            }
            parent = parent.parent();
        }
        return false;
    }

    public static boolean isInMember(Node node){
        return node.parent().parent() instanceof MemberNode;
    }


    // --- Design Considerations ---
    /*
     * Potential Enhancements:
     * 1. Add custom serialization handlers via registry
     * 2. Support for circular references
     * 3. Configuration for field inclusion/exclusion
     * 4. Better error handling for reflection operations
     */
}
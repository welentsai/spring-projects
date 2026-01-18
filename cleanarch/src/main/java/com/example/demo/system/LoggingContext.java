package com.example.demo.system;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

public class LoggingContext {
    private static final ThreadLocal<Map<String, Object>> contextHolder =
            ThreadLocal.withInitial(LinkedHashMap::new);

    // Private constructor to prevent instantiation
    private LoggingContext() {}

    /**
     * Put a key-value pair into the logging context. Also adds it to MDC for automatic inclusion in
     * logs.
     */
    public static Map<String, Object> put(String key, Object value) {
        if (key != null && value != null) {
            contextHolder.get().put(key, value);
            MDC.put(key, value.toString());
        }

        return asMap();
    }

    /** Get a value from the logging context. */
    public static Object get(String key) {
        return contextHolder.get().get(key);
    }

    /** Remove a specific key from the context and MDC. */
    public static void remove(String key) {
        contextHolder.get().remove(key);
        MDC.remove(key);
    }

    /** Get a copy of the entire context as a Map. Preserves insertion order (LinkedHashMap). */
    public static Map<String, Object> asMap() {
        return new LinkedHashMap<>(contextHolder.get());
    }

    /**
     * Clear the entire logging context and MDC. IMPORTANT: Must be called at the end of request
     * processing.
     */
    public static void clear() {
        Map<String, Object> context = contextHolder.get();
        context.keySet().forEach(MDC::remove);
        context.clear();
        contextHolder.remove(); // Remove ThreadLocal to prevent memory leaks
    }
}

package KilimanJARo.P2P.utils;

import java.util.Map;
import java.util.Properties;

public class SmartProperties {
    private final Properties publicProperties;
    private final Properties privateProperties;

    public SmartProperties(Properties publicProperties, Properties privateProperties) {
        this.publicProperties = publicProperties;
        this.privateProperties = privateProperties;
    }

    public String getProperty(String key, Map<String, String> temp_placeholders) {
        if (key.startsWith("private.")) {
            return getPropertyFromPrivate(key.substring(8), temp_placeholders);
        } else {
            return getPropertyFromPublic(key, temp_placeholders);
        }
    }

    public String getProperty(String key) {
        return getProperty(key, Map.of());
    }

    private String getPropertyFromPublic(String key, Map<String, String> temp_placeholders) {
        String value = publicProperties.getProperty(key);
        if (value != null) {
            value = resolvePlaceholders(value, publicProperties, temp_placeholders);
        }
        return value;
    }

    private String getPropertyFromPrivate(String key, Map<String, String> temp_placeholders) {
        String value = privateProperties.getProperty(key);
        if (value != null) {
            value = resolvePlaceholders(value, privateProperties, temp_placeholders);
        }
        return value;
    }

    private String resolvePlaceholders(String value, Properties properties, Map<String, String> temp_placeholders) {
        while (value.contains("${")) {
            int startIndex = value.indexOf("${");
            int endIndex = value.indexOf("}", startIndex);
            if (endIndex == -1) {
                throw new IllegalArgumentException("Incorrect placeholder in property " + value);
            }
            String placeholder = value.substring(startIndex + 2, endIndex);
            String placeholderValue = temp_placeholders.getOrDefault(placeholder, properties.getProperty(placeholder));
            if (placeholderValue != null) {
                value = value.replace("${" + placeholder + "}", placeholderValue);
            } else {
                break;
            }
        }
        return value;
    }
}
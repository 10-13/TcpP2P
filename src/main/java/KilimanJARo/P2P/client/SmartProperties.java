package KilimanJARo.P2P.client;

import java.util.Properties;

public class SmartProperties {
    private final Properties properties;

    public SmartProperties(Properties properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value != null) {
            value = resolvePlaceholders(value);
        }
        return value;
    }

    private String resolvePlaceholders(String value) {
        while (value.contains("${")) {
            int startIndex = value.indexOf("${");
            int endIndex = value.indexOf("}", startIndex);
            if (endIndex == -1) {
                throw new IllegalArgumentException("Incorrect placeholder in property " + value);
            }
            String placeholder = value.substring(startIndex + 2, endIndex);
            String placeholderValue = properties.getProperty(placeholder);
            if (placeholderValue != null) {
                value = value.replace("${" + placeholder + "}", placeholderValue);
            } else {
                break;
            }
        }
        return value;
    }
}
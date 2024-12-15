package KilimanJARo.P2P.client;

import java.util.Properties;

public class SmartProperties {
    private final Properties publicProperties;
    private final Properties privateProperties;

    public SmartProperties(Properties publicProperties, Properties privateProperties) {
        this.publicProperties = publicProperties;
        this.privateProperties = privateProperties;
    }

    public String getProperty(String key) {
        if (key.startsWith("private.")) {
            return getPropertyFromPrivate(key.substring(8));
        } else {
            return getPropertyFromPublic(key);
        }
    }

    private String getPropertyFromPublic(String key) {
        String value = publicProperties.getProperty(key);
        if (value != null) {
            value = resolvePlaceholders(value, publicProperties);
        }
        return value;
    }

    private String getPropertyFromPrivate(String key) {
        String value = privateProperties.getProperty(key);
        if (value != null) {
            value = resolvePlaceholders(value, privateProperties);
        }
        return value;
    }

    private String resolvePlaceholders(String value, Properties properties) {
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
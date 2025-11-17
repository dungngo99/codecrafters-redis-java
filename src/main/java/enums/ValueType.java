package enums;

import java.util.Objects;

public enum ValueType {
    STRING,
    STREAM,
    LIST;

    public static boolean isList(ValueType inputVT) {
        return Objects.equals(inputVT, LIST);
    }
}

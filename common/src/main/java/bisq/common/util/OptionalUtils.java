package bisq.common.util;

import java.util.Arrays;
import java.util.Optional;

public class OptionalUtils {

    public static Optional<String> toOptional(String value) {
        return bisq.common.util.StringUtils.toOptional(value);
    }

    public static Optional<String> normalize(Optional<String> optional) {
        return Optional.ofNullable(optional).orElse(Optional.empty());
    }

    // Using default equals method for an Optional<byte[]> would use comparison by reference not by value.
    public static boolean optionalByteArrayEquals(Optional<byte[]> a, Optional<byte[]> b) {
        return a.isPresent() == b.isPresent() &&
                (a.isEmpty() || Arrays.equals(a.get(), b.get()));
    }
}

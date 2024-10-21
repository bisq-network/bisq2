package bisq.common.util;

import java.util.Optional;

import static bisq.common.util.StringUtils.toNullIfEmpty;

public class OptionalUtils {

    public static Optional<String> toOptional(String value) {
        return bisq.common.util.StringUtils.toOptional(value);
    }

    public static Optional<String> normalize(Optional<String> optional) {
        return Optional.ofNullable(optional).orElse(Optional.empty());
    }
}

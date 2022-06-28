package bisq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@Slf4j
public class StringUtilsTest {

    @Test
    public void deriveWordStartingWith() {
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello jo", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("@jo x", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello@jo", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello @jo!", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Go to#chan", '@'), null);

        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello @jo", '@'), "jo");
        assert Objects.equals(StringUtils.deriveWordStartingWith("@john", '@'), "john");
        assert Objects.equals(StringUtils.deriveWordStartingWith("Go to #chann", '#'), "chann");
        assert Objects.equals(StringUtils.deriveWordStartingWith("#chann", '#'), "chann");
    }
}

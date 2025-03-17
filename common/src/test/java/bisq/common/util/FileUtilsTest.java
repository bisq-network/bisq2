package bisq.common.util;

import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class FileUtilsTest {

    @Test
    public void hasResourceFile() {
        assertTrue(FileUtils.hasResourceFile("logback.xml"));
        assertFalse(FileUtils.hasResourceFile("logback.xml1"));
    }
}

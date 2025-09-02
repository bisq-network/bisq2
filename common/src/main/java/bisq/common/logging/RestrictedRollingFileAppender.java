package bisq.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

public class RestrictedRollingFileAppender extends RollingFileAppender<ILoggingEvent> {
    @Override
    public void openFile(String fileName) throws IOException {
        super.openFile(fileName);
        try {
            Files.setPosixFilePermissions(Paths.get(fileName),
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException e) {
            // For non-posix file systems, we ignore this exception
        }
    }
}

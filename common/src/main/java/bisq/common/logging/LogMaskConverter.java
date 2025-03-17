package bisq.common.logging;

import bisq.common.util.StringUtils;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

public class LogMaskConverter extends CompositeConverter<ILoggingEvent> {
    public String transform(ILoggingEvent event, String message) {
        return StringUtils.maskHomeDirectory(message);
    }
}

package bisq.common.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;

/**
 * Rolling file appender with a predefined configuration.
 * <br/><br/>
 * In order to be instantiated in logback.xml, it only needs the property "file" to be set
 */
public class BisqRollingFileAppender extends RollingFileAppender<ILoggingEvent> {

    @Override
    public void setFile(String file) {
        super.setFile(file);

        // Setting the file also sets the other policies and options, which depend on the filename

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(getContext());
        rollingPolicy.setParent(this);
        rollingPolicy.setFileNamePattern(file + "_%i.log");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(20);
        rollingPolicy.start();

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
        triggeringPolicy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(getContext());
        encoder.setPattern("%d{MMM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{15}: %msg %xEx%n");
        encoder.start();

        setEncoder(encoder);
        setRollingPolicy(rollingPolicy);
        setTriggeringPolicy(triggeringPolicy);
    }
}

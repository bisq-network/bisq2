/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.base.Charsets;
import org.slf4j.LoggerFactory;

public class LogSetup {
    private static final String CONSOLE_APPENDER_NAME = "CONSOLE_APPENDER";
    private static final String STDERR_APPENDER_NAME = "STDERR_APPENDER";
    private static Logger logbackLogger;
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    public static void setLevel(Level logLevel) {
        logbackLogger.setLevel(logLevel);
    }

    public static void setup(String fileName) {
        setup(fileName, 20, "10MB", DEFAULT_LOG_LEVEL);
    }

    public static void setupStderrAppenderOnly() {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            LoggerFactory.getLogger(LogSetup.class).warn("Cannot configure stderr-only logging because logger factory is {}",
                    LoggerFactory.getILoggerFactory().getClass().getName());
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender(CONSOLE_APPENDER_NAME);
        rootLogger.detachAppender(STDERR_APPENDER_NAME);

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName(STDERR_APPENDER_NAME);
        appender.setTarget("System.err");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{MMM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{15}: %mask(%msg) %xEx%n");
        encoder.setCharset(Charsets.UTF_8);
        encoder.start();
        appender.setEncoder(encoder);
        appender.start();

        rootLogger.addAppender(appender);
        rootLogger.setLevel(DEFAULT_LOG_LEVEL);
        logbackLogger = rootLogger;
    }

    public static org.slf4j.Logger setupRawRollingLogger(String loggerName, String fileName) {
        if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
            return LoggerFactory.getLogger(loggerName);
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(loggerName);
        logger.detachAndStopAllAppenders();

        RollingFileAppender<ILoggingEvent> appender = new RestrictedRollingFileAppender();
        appender.setContext(loggerContext);
        appender.setName(loggerName + "_APPENDER");
        appender.setFile(fileName + ".log");

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(fileName + "_%i.log");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(20);
        rollingPolicy.start();

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
        triggeringPolicy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%msg%n");
        encoder.setCharset(Charsets.UTF_8);
        encoder.start();

        appender.setEncoder(encoder);
        appender.setRollingPolicy(rollingPolicy);
        appender.setTriggeringPolicy(triggeringPolicy);
        appender.start();

        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(DEFAULT_LOG_LEVEL);
        return logger;
    }

    public static void setup(String fileName, int rollingPolicyMaxIndex, String maxFileSize, Level logLevel) {
        if (logbackLogger != null && LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        RollingFileAppender<ILoggingEvent> appender = new RestrictedRollingFileAppender();
        appender.setContext(loggerContext);
        appender.setFile(fileName + ".log");

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern(fileName + "_%i.log");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(rollingPolicyMaxIndex);
        rollingPolicy.start();

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        triggeringPolicy.start();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{MMM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{15}: %mask(%msg) %xEx%n");
        encoder.setCharset(Charsets.UTF_8);
        encoder.start();

        appender.setEncoder(encoder);
        appender.setRollingPolicy(rollingPolicy);
        appender.setTriggeringPolicy(triggeringPolicy);
        appender.start();

        logbackLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logbackLogger.addAppender(appender);
        logbackLogger.setLevel(logLevel);
    }
}

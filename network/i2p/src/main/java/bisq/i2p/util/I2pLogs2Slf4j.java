package bisq.i2p.util;

import net.i2p.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class I2pLogs2Slf4j extends Log {

    private static final Map<Integer, Level> I2P_TO_SLF4J = new HashMap<>();
    static {
        I2P_TO_SLF4J.put(Log.DEBUG, Level.DEBUG);
        I2P_TO_SLF4J.put(Log.INFO, Level.INFO);
        I2P_TO_SLF4J.put(Log.WARN, Level.WARN);
        I2P_TO_SLF4J.put(Log.ERROR, Level.ERROR);
        I2P_TO_SLF4J.put(Log.CRIT, Level.ERROR);
    }

    private final Logger delegate;
    private final Level level;

    public I2pLogs2Slf4j(Class<?> cls) {
        super(cls);
        delegate = LoggerFactory.getLogger(cls.getName());
        level = findLevel(delegate);
    }

    public I2pLogs2Slf4j(String name) {
        super(name);
        delegate = LoggerFactory.getLogger(name);
        level = findLevel(delegate);

    }

    private static Level findLevel(Logger log) {
        //Reflection shenanigans to find the level...
        ch.qos.logback.classic.Level lvl;
        ch.qos.logback.classic.Logger parent;
        Field levelField;
        Field parentField;
        try {
            levelField = log.getClass().getDeclaredField("level");
            levelField.setAccessible(true);
            lvl = (ch.qos.logback.classic.Level) levelField.get(log);
            if(lvl == null) {
                parentField = log.getClass().getDeclaredField("parent");
                parentField.setAccessible(true);
                parent = (ch.qos.logback.classic.Logger) parentField.get(log);
                return findLevel(parent);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (lvl == ch.qos.logback.classic.Level.ALL ||
                lvl == ch.qos.logback.classic.Level.DEBUG ||
                lvl == ch.qos.logback.classic.Level.TRACE) {
            return Level.DEBUG;
        } else if (lvl == ch.qos.logback.classic.Level.ERROR) {
            return Level.ERROR;
        } else if (lvl == ch.qos.logback.classic.Level.WARN) {
            return Level.WARN;
        }
        return Level.INFO;
    }

    @Override
    public void log(int priority, String msg) {
        Level translatedPriority = I2P_TO_SLF4J.get(priority);
        if(translatedPriority.toInt() == Level.DEBUG.toInt())
            delegate.debug(msg);
        if(translatedPriority.toInt() == Level.WARN.toInt())
            delegate.warn(msg);
        if(translatedPriority.toInt() == Level.ERROR.toInt())
            delegate.error(msg);
        if(translatedPriority.toInt() == Level.INFO.toInt())
            delegate.info(msg);
    }

    @Override
    public void log(int priority, String msg, Throwable t) {
        Level translatedPriority = I2P_TO_SLF4J.get(priority);
        if(translatedPriority.toInt() == Level.DEBUG.toInt())
            delegate.debug(msg, t);
        if(translatedPriority.toInt() == Level.WARN.toInt())
            delegate.warn(msg, t);
        if(translatedPriority.toInt() == Level.ERROR.toInt())
            delegate.error(msg, t);
        if(translatedPriority.toInt() == Level.INFO.toInt())
            delegate.info(msg, t);
    }

    @Override
    public boolean shouldLog(int priority) {
        Level translatedPriority = I2P_TO_SLF4J.get(priority);
        if(translatedPriority.toInt() == Level.DEBUG.toInt())
            return delegate.isDebugEnabled();
        if(translatedPriority.toInt() == Level.WARN.toInt())
            return delegate.isWarnEnabled();
        if(translatedPriority.toInt() == Level.ERROR.toInt())
            return delegate.isErrorEnabled();
        if(translatedPriority.toInt() == Level.INFO.toInt())
            return delegate.isInfoEnabled();
        return false;
    }

    @Override
    public boolean shouldDebug() {
        return level.toInt() <= Level.DEBUG.toInt();
    }

    @Override
    public boolean shouldInfo() {
        return level.toInt() <= Level.INFO.toInt();
    }

    @Override
    public boolean shouldWarn() {
        return level.toInt() <= Level.WARN.toInt();
    }

    @Override
    public boolean shouldError() {
        return level.toInt() <= Level.ERROR.toInt();
    }

}

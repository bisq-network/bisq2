package bisq.i2p.util;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;
import net.i2p.util.LogManager;

import java.util.HashMap;
import java.util.Map;

public class I2PLogManager extends LogManager {

    private static final Map<Class<?>, Log> classLogs = new HashMap<>();
    private static final Map<String, Log> stringLogs = new HashMap<>();

    public I2PLogManager() {
        super(I2PAppContext.getGlobalContext());
    }

    @Override
    public synchronized Log getLog(Class<?> cls, String name) {
        Log log;
        if (cls != null) {
            log = classLogs.get(cls);
            if (log == null) {
                log = new I2pLogs2Slf4j(cls);
                classLogs.put(cls, log);
            }
        }
        else {
            log = stringLogs.get(name);
            if (log == null) {
                log = new I2pLogs2Slf4j(name);
                stringLogs.put(name, log);
            }
        }
        return log;
    }
}
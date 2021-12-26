package network.misq.common;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TypesafeConfigTest {

    @Test
    public void testNetworkPoolConfig() {
        Config config = MisqConfig.getConfig(MisqConfig.NETWORK_IO_POOL_CONFIG_PATH);
        assertNotNull(config);

        Assertions.assertEquals("NETWORK_IO_POOL", config.getString("name"));
        Assertions.assertEquals(1, config.getInt("corePoolSize"));
        Assertions.assertEquals(5000, config.getInt("maximumPoolSize"));
        Assertions.assertEquals(10, config.getLong("keepAliveTimeInSec"));
    }
}

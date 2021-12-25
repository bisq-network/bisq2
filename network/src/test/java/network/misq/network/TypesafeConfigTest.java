package network.misq.network;

import com.typesafe.config.Config;
import network.misq.common.configuration.MisqConfig;
import org.junit.jupiter.api.Test;

import static network.misq.common.configuration.MisqConfig.NETWORK_IO_POOL_CONFIG_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TypesafeConfigTest {

    @Test
    public void testNetworkPoolConfig() {
        Config config = MisqConfig.getConfig(NETWORK_IO_POOL_CONFIG_PATH);
        assertNotNull(config);

        assertEquals("NETWORK_IO_POOL", config.getString("name"));
        assertEquals(1, config.getInt("corePoolSize"));
        assertEquals(5000, config.getInt("maximumPoolSize"));
        assertEquals(10, config.getLong("keepAliveTimeInSec"));
    }
}

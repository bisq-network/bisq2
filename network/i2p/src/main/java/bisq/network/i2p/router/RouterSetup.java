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

package bisq.network.i2p.router;

import bisq.common.file.FileUtils;
import bisq.common.file.PropertiesReader;
import bisq.common.logging.LogSetup;
import bisq.network.i2p.router.utils.I2PLogLevel;
import bisq.network.i2p.router.utils.RouterCertificateUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PClient;
import net.i2p.data.DataHelper;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.util.OrderedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class RouterSetup {
    public static final int DEFAULT_I2CP_PORT = 7654;
    public static final int DEFAULT_BI2P_GRPC_PORT = 6159;

    private final String i2cpHost;
    private final int i2cpPort;
    @Getter
    private final I2PLogLevel i2pLogLevel;
    private final boolean isEmbedded;
    private final int inboundKBytesPerSecond;
    private final int outboundKBytesPerSecond;
    private final int bandwidthSharePercentage;
    private final File i2pDir;

    RouterSetup(Path i2pDirPath,
                String i2cpHost,
                int i2cpPort,
                I2PLogLevel i2pLogLevel,
                boolean isEmbedded,
                int inboundKBytesPerSecond,
                int outboundKBytesPerSecond,
                int bandwidthSharePercentage) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.i2pLogLevel = i2pLogLevel;
        this.isEmbedded = isEmbedded;
        this.inboundKBytesPerSecond = inboundKBytesPerSecond;
        this.outboundKBytesPerSecond = outboundKBytesPerSecond;
        this.bandwidthSharePercentage = bandwidthSharePercentage;

        i2pDir = i2pDirPath.toFile();
        setupDirectories();
        setupLogging();

        setupSystemProperties();
    }

    void initialize() {
        try {
            setupProperties();
            RouterCertificateUtil.copyCertificatesFromResources(i2pDir);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void setI2pLogLevel(Router router) {
        RouterContext routerContext = router.getContext();
        routerContext.logManager().setDefaultLimit(i2pLogLevel.name());
    }

    private void setupLogging() {
        String fileName = i2pDir.toPath().resolve("bi2p").toString();
        LogSetup.setup(fileName);
        log.info("I2P router app logging to {}", fileName);
    }

    private void setupDirectories() {
        try {
            createDirectoryAndSetProperty("", "i2p.dir.base");
            createDirectoryAndSetProperty("router", "i2p.dir.router");
            createDirectoryAndSetProperty("pid", "i2p.dir.pid");
        } catch (IOException e) {
            log.error("setupDirectories failed", e);
            throw new RuntimeException(e);
        }
    }

    private void setupSystemProperties() {
        // Must be set before I2P router is created as otherwise log outputs are routed by I2P log system
        System.setProperty("I2P_DISABLE_OUTPUT_OVERRIDE", "true");

        // Having IPv6 enabled can cause problems with certain configurations.
        // System.setProperty("java.net.preferIPv4Stack", "true");

        // System.setProperty("i2cp.host", i2cpHost);
        //   System.setProperty("i2cp.port", String.valueOf(i2cpPort));

        if (isEmbedded) {
            // When embedded, I2P uses in-process communication instead of TCP
            System.setProperty(I2PClient.PROP_TCP_HOST, "internal");
            System.setProperty(I2PClient.PROP_TCP_PORT, "internal");
            System.setProperty("i2cp.host", "internal");
            System.setProperty("i2cp.port", "internal");

            // We can disable the interface
            System.setProperty("i2cp.disableInterface", "true");
        } else {
            // When started as separate process
            System.setProperty(I2PClient.PROP_TCP_HOST, i2cpHost);
            System.setProperty(I2PClient.PROP_TCP_PORT, String.valueOf(i2cpPort));
            System.setProperty("i2cp.host", i2cpHost);
            System.setProperty("i2cp.port", String.valueOf(i2cpPort));

            // We need to have the interface enabled
            System.setProperty("i2cp.disableInterface", "false");
        }
    }

    private void setupProperties() {
        Properties properties = new Properties();
        if (isEmbedded) {
            // When embedded, I2P uses in-process communication instead of TCP
            properties.put(I2PClient.PROP_TCP_HOST, "internal");
            properties.put(I2PClient.PROP_TCP_PORT, "internal");

            // We can disable the interface
            properties.put("i2cp.disableInterface", "true");
        } else {
            // When started as separate process
            properties.put(I2PClient.PROP_TCP_HOST, i2cpHost);
            properties.put(I2PClient.PROP_TCP_PORT, String.valueOf(i2cpPort));

            // We need to have the interface enabled
            properties.put("i2cp.disableInterface", "false");
        }

        properties.put("i2np.bandwidth.inboundKBytesPerSecond", String.valueOf(inboundKBytesPerSecond));
        properties.put("i2np.bandwidth.inboundBurstKBytesPerSecond", String.valueOf(inboundKBytesPerSecond));
        properties.put("i2np.bandwidth.outboundKBytesPerSecond", String.valueOf(outboundKBytesPerSecond));
        properties.put("i2np.bandwidth.outboundBurstKBytesPerSecond", String.valueOf(outboundKBytesPerSecond));
        properties.put("router.sharePercentage", String.valueOf(bandwidthSharePercentage)); // default 80%

        mergeAndStoreRouterConfig(properties);
    }

    private void mergeAndStoreRouterConfig(Properties overrides) {
        File configFile = new File(i2pDir, "router.config");
        Properties properties = new OrderedProperties();
        properties.putAll(getPropertiesFromResources());
        properties.putAll(getPropertiesI2pDir(configFile));
        properties.putAll(overrides);
        try {
            DataHelper.storeProps(properties, configFile);
        } catch (IOException e) {
            log.warn("Could not store router.config file in i2p data directory {}", configFile.getAbsolutePath(), e);
        }
    }

    private Properties getPropertiesFromResources() {
        Properties properties = new Properties();
        try {
            properties = PropertiesReader.getProperties("router.config");
        } catch (IOException e) {
            log.warn("Could not load router.config file from resources", e);
        }
        return properties;
    }

    private Properties getPropertiesI2pDir(File configFile) {
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                DataHelper.loadProps(properties, inputStream);
            } catch (IOException e) {
                log.warn("Could not load router.config file from i2p data directory {}", configFile.getAbsolutePath(), e);
            }
        } else {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                log.warn("Could not create router.config file in i2p data directory {}", configFile.getAbsolutePath(), e);
            }
        }
        return properties;
    }

    private void createDirectoryAndSetProperty(String dirName, String propertyName) throws IOException {
        File dir = createDirectory(dirName);
        System.setProperty(propertyName, dir.getAbsolutePath());
    }

    private File createDirectory(String dirName) throws IOException {
        return createDirectory(i2pDir, dirName);
    }

    private File createDirectory(File parent, String child) throws IOException {
        File dir = new File(parent, child);
        FileUtils.makeDirIfNotExists(dir);
        return dir;
    }
}

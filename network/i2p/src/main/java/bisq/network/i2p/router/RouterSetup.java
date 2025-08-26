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
import bisq.network.i2p.router.log.I2pLogLevel;
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
class RouterSetup {

    private final String i2cpHost;
    private final int i2cpPort;
    @Getter
    private final I2pLogLevel i2pLogLevel;
    private final boolean isInProcess;
    private final int inboundKBytesPerSecond;
    private final int outboundKBytesPerSecond;
    private final int bandwidthSharePercentage;
    private final File i2pDir;

    RouterSetup(Path i2pDirPath,
                String i2cpHost,
                int i2cpPort,
                I2pLogLevel i2pLogLevel,
                boolean isInProcess,
                int inboundKBytesPerSecond,
                int outboundKBytesPerSecond,
                int bandwidthSharePercentage) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.i2pLogLevel = i2pLogLevel;
        this.isInProcess = isInProcess;
        this.inboundKBytesPerSecond = inboundKBytesPerSecond;
        this.outboundKBytesPerSecond = outboundKBytesPerSecond;
        this.bandwidthSharePercentage = bandwidthSharePercentage;

        i2pDir = i2pDirPath.toFile();
        setupLogging();

        // Must be set before I2P router is created as otherwise log outputs are routed by I2P log system
        System.setProperty("I2P_DISABLE_OUTPUT_OVERRIDE", "true");

        // Having IPv6 enabled can cause problems with certain configurations.
        System.setProperty("java.net.preferIPv4Stack", "true");

        System.setProperty("i2cp.host", i2cpHost);
        System.setProperty("i2cp.port", String.valueOf(i2cpPort));
        //todo
        // System.setProperty("router.reseedDisable", "true");
    }

    void initialize() {
        try {
            setupDirectories();
            setupProperties();
            setupCertificatesDirectories();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void setI2pLogLevel(Router router) {
        RouterContext routerContext = router.getContext();
        routerContext.logManager().setDefaultLimit(i2pLogLevel.name());
    }

    private void setupLogging() {
        String fileName = i2pDir.toPath().resolve("i2p_router").toString();
        LogSetup.setup(fileName);
        log.info("I2P router app logging to {}", fileName);
    }

    private void setupDirectories() throws IOException {
        createDirectoryAndSetProperty("", "i2p.dir.base");
        createDirectoryAndSetProperty("router", "i2p.dir.router");
        createDirectoryAndSetProperty("pid", "i2p.dir.pid");
    }

    private void setupProperties() {
        Properties properties = new Properties();
        if (isInProcess) {
            // When used as embedded router I2P uses in-process communication instead of TPC
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

    private void setupCertificatesDirectories() throws IOException, URISyntaxException {
        File certDir = createDirectory("certificates");
        File seedDir = createDirectory(certDir, "reseed");
        File sslDir = createDirectory(certDir, "ssl");
        FileUtils.copyResourceDirectory("certificates/reseed/", seedDir.toPath());
        FileUtils.copyResourceDirectory("certificates/ssl/", sslDir.toPath());
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

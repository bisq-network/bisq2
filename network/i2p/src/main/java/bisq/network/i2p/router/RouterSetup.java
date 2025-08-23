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
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PClient;
import net.i2p.data.DataHelper;
import net.i2p.util.OrderedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

@Slf4j
class RouterSetup {
    private final boolean isEmbedded;
    private final int inboundKBytesPerSecond;
    private final int outboundKBytesPerSecond;
    private final int bandwidthSharePercentage;
    private final File i2pDir;

    RouterSetup(String i2pDirPath,
                boolean isEmbedded,
                int inboundKBytesPerSecond,
                int outboundKBytesPerSecond,
                int bandwidthSharePercentage) {
        this.isEmbedded = isEmbedded;
        this.inboundKBytesPerSecond = inboundKBytesPerSecond;
        this.outboundKBytesPerSecond = outboundKBytesPerSecond;
        this.bandwidthSharePercentage = bandwidthSharePercentage;

        i2pDir = new File(i2pDirPath);
    }

    void initialize() throws IOException, URISyntaxException {
        setupDirectories();
        setupProperties();
        setupCertificatesDirectories();
    }

    private void setupDirectories() throws IOException {
        createDirectoryAndSetProperty("", "i2p.dir.base");
        createDirectoryAndSetProperty("config", "i2p.dir.config");
        createDirectoryAndSetProperty("router", "i2p.dir.router");
        createDirectoryAndSetProperty("pid", "i2p.dir.pid");
        createDirectoryAndSetProperty("logs", "i2p.dir.log");
        createDirectoryAndSetProperty("app", "i2p.dir.app");
    }

    private void setupProperties() {
        Properties properties = new Properties();
        if (isEmbedded) {
            // When used as embedded router I2P uses in-process communication instead of TPC
            properties.put(I2PClient.PROP_TCP_HOST, "internal");
            properties.put(I2PClient.PROP_TCP_PORT, "internal");

            // We can disable the interface
            properties.put("i2cp.disableInterface", "true");
        } else {
            // When started as separate process
            properties.put(I2PClient.PROP_TCP_HOST, "127.0.0.1");
            properties.put(I2PClient.PROP_TCP_PORT, "7654");

            // We need to have the interface enables
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

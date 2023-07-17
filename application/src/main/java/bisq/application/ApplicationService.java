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

package bisq.application;

import bisq.common.application.DevMode;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.logging.LogSetup;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.i18n.Res;
import bisq.persistence.PersistenceService;
import ch.qos.logback.classic.Level;
import com.typesafe.config.ConfigFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class ApplicationService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config {
        private static Config from(com.typesafe.config.Config config, String[] args) {
            String appName = "Bisq2";
            if (config.hasPath("appName")) {
                appName = config.getString("appName");
            }

            boolean devMode = config.getBoolean("devMode");

            String dataDir = null;
            for (String arg : args) {
                if (arg.startsWith("--appName")) {
                    appName = arg.split("=")[1];
                }

                if (arg.startsWith("--data-dir")) {
                    dataDir = arg.split("=")[1];
                }
            }

            String appDir = dataDir == null ? OsUtils.getUserDataDir() + File.separator + appName : dataDir;
            log.info("Use application directory {}", appDir);

            String version = config.getString("version");

            return new Config(appDir, appName, version, devMode);
        }

        private final String baseDir;
        private final String appName;
        private final String version;
        private final boolean devMode;

        public Config(String baseDir,
                      String appName,
                      String version,
                      boolean devMode) {
            this.baseDir = baseDir;
            this.appName = appName;
            this.version = version;
            this.devMode = devMode;
        }
    }

    private final com.typesafe.config.Config typesafeAppConfig;
    @Getter
    protected final Config config;
    @Getter
    protected final PersistenceService persistenceService;

    private FileLock instanceLock;

    public ApplicationService(String configFileName, String[] args) {
        com.typesafe.config.Config typesafeConfig = ConfigFactory.load(configFileName);
        typesafeConfig.checkValid(ConfigFactory.defaultReference(), configFileName);

        typesafeAppConfig = typesafeConfig.getConfig("application");
        config = Config.from(typesafeAppConfig, args);

        try {
            FileUtils.makeDirs(config.getBaseDir());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkInstanceLock();

        LogSetup.setup(Paths.get(config.getBaseDir(), "bisq").toString());
        LogSetup.setLevel(Level.INFO);

        DevMode.setDevMode(config.isDevMode());

        Locale locale = LocaleRepository.getDefaultLocale();
        CountryRepository.setLocale(locale);
        LanguageRepository.setLocale(locale);
        FiatCurrencyRepository.setLocale(locale);
        Res.setLocale(locale);
        ResolverConfig.config();

        persistenceService = new PersistenceService(config.getBaseDir());
    }

    private void checkInstanceLock() {
        // Acquire exclusive lock on file basedir/lock, throw if locks fails
        // to avoid running multiple instances using the same basedir
        try (FileOutputStream fileOutputStream = new FileOutputStream(Paths.get(config.getBaseDir(), "lock").toString())) {
            instanceLock = fileOutputStream.getChannel().tryLock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (instanceLock == null) {
            throw new NoFileLockException("Another instance might be running", new Throwable("Unable to acquire lock file lock"));
        }
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    public abstract CompletableFuture<Boolean> initialize();

    public abstract CompletableFuture<Boolean> shutdown();

    protected com.typesafe.config.Config getConfig(String path) {
        return typesafeAppConfig.getConfig(path);
    }

    protected boolean hasConfig(String path) {
        return typesafeAppConfig.hasPath(path);
    }
}

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
import bisq.common.application.Service;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.logging.LogSetup;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.common.util.Version;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class ApplicationService implements Service {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config {
        private static Config from(com.typesafe.config.Config config, String[] args) {
            String appName = "Bisq2";
            if (config.hasPath("appName")) {
                appName = config.getString("appName");
            }

            Optional<Path> dataDir = Optional.empty();
            for (String arg : args) {
                if (arg.startsWith("--appName")) {
                    appName = arg.split("=")[1];
                }

                if (arg.startsWith("--data-dir")) {
                    dataDir = Optional.of(
                            Paths.get(arg.split("=")[1])
                    );
                }
            }

            Path appDataDir = dataDir.orElse(
                    OsUtils.getUserDataDir().resolve(appName)
            );
            log.info("Use application directory {}", appDataDir);

            return new Config(appDataDir,
                    appName,
                    config.getString("version"),
                    config.getBoolean("devMode"),
                    config.getString("keyIds"),
                    config.getBoolean("ignoreSigningKeyInResourcesCheck"),
                    config.getBoolean("ignoreSignatureVerification"));
        }

        private final Path baseDir;
        private final String appName;
        private final Version version;
        private final boolean devMode;
        private final List<String> keyIds;
        private final boolean ignoreSigningKeyInResourcesCheck;
        private final boolean ignoreSignatureVerification;

        public Config(Path baseDir,
                      String appName,
                      String version,
                      boolean devMode,
                      String keyIds,
                      boolean ignoreSigningKeyInResourcesCheck,
                      boolean ignoreSignatureVerification) {
            this.baseDir = baseDir;
            this.appName = appName;
            this.version = new Version(version);
            this.devMode = devMode;
            // We want to use the keyIds at the DesktopApplicationLauncher as a simple format. 
            // Using the typesafe format with indexes would require a more complicate parsing as we do not use 
            // typesafe at the DesktopApplicationLauncher class. Thus, we use a simple comma separated list instead and treat it as sting in typesafe.
            this.keyIds = List.of(keyIds.split(","));
            this.ignoreSigningKeyInResourcesCheck = ignoreSigningKeyInResourcesCheck;
            this.ignoreSignatureVerification = ignoreSignatureVerification;
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

        Path dataDir = config.getBaseDir();
        try {
            FileUtils.makeDirs(dataDir.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        checkInstanceLock();

        LogSetup.setup(dataDir.resolve("bisq").toString());
        LogSetup.setLevel(Level.INFO);

        DevMode.setDevMode(config.isDevMode());

        Locale locale = LocaleRepository.getDefaultLocale();
        CountryRepository.applyDefaultLocale(locale);
        LanguageRepository.setDefaultLanguage(locale.getLanguage());
        FiatCurrencyRepository.setLocale(locale);
        Res.setLanguage(LanguageRepository.getDefaultLanguage());
        ResolverConfig.config();

        String absoluteDataDirPath = dataDir.toAbsolutePath().toString();
        persistenceService = new PersistenceService(absoluteDataDirPath);
    }

    private void checkInstanceLock() {
        // Acquire exclusive lock on file basedir/lock, throw if locks fails
        // to avoid running multiple instances using the same basedir
        File lockFilePath = config.getBaseDir()
                .resolve("lock")
                .toFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(lockFilePath)) {
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

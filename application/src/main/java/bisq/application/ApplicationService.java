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

import bisq.application.migration.MigrationService;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.file.FileUtils;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.logging.AsciiLogo;
import bisq.common.logging.LogSetup;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.persistence.PersistenceService;
import ch.qos.logback.classic.Level;
import com.typesafe.config.ConfigFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class ApplicationService implements Service {
    public static final String CUSTOM_CONFIG_FILE_NAME = "bisq.conf";

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config {
        private static Config from(com.typesafe.config.Config rootConfig,
                                   com.typesafe.config.Config config,
                                   Path baseDir) {
            return new Config(rootConfig,
                    baseDir,
                    config.getString("appName"),
                    config.getBoolean("devMode"),
                    config.getLong("devModeReputationScore"),
                    config.getBoolean("devModeWalletSetup"),
                    config.getString("keyIds"),
                    config.getBoolean("ignoreSigningKeyInResourcesCheck"),
                    config.getBoolean("ignoreSignatureVerification"),
                    config.getInt("memoryReportIntervalSec"),
                    config.getBoolean("includeThreadListInMemoryReport"),
                    config.getBoolean("checkInstanceLock"));
        }

        private final com.typesafe.config.Config rootConfig;
        private final Path baseDir;
        private final String appName;
        private final boolean devMode;
        private final long devModeReputationScore;
        private final boolean devModeWalletSetup;
        private final List<String> keyIds;
        private final boolean ignoreSigningKeyInResourcesCheck;
        private final boolean ignoreSignatureVerification;
        private final int memoryReportIntervalSec;
        private final boolean includeThreadListInMemoryReport;
        private final boolean checkInstanceLock;

        public Config(com.typesafe.config.Config rootConfig,
                      Path baseDir,
                      String appName,
                      boolean devMode,
                      long devModeReputationScore,
                      boolean devModeWalletSetup,
                      String keyIds,
                      boolean ignoreSigningKeyInResourcesCheck,
                      boolean ignoreSignatureVerification,
                      int memoryReportIntervalSec,
                      boolean includeThreadListInMemoryReport,
                      boolean checkInstanceLock) {
            this.rootConfig = rootConfig;
            this.baseDir = baseDir;
            this.appName = appName;
            this.devMode = devMode;
            this.devModeReputationScore = devModeReputationScore;
            this.devModeWalletSetup = devModeWalletSetup;
            // We want to use the keyIds at the DesktopApplicationLauncher as a simple format. 
            // Using the typesafe format with indexes would require a more complicate parsing as we do not use 
            // typesafe at the DesktopApplicationLauncher class. Thus, we use a simple comma separated list instead and treat it as sting in typesafe.
            this.keyIds = Arrays.stream(keyIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            this.ignoreSigningKeyInResourcesCheck = ignoreSigningKeyInResourcesCheck;
            this.ignoreSignatureVerification = ignoreSignatureVerification;
            this.memoryReportIntervalSec = memoryReportIntervalSec;
            this.includeThreadListInMemoryReport = includeThreadListInMemoryReport;
            this.checkInstanceLock = checkInstanceLock;
        }
    }

    protected final com.typesafe.config.Config jvmConfig;
    protected final com.typesafe.config.Config programArgConfig;
    protected final com.typesafe.config.Config resourceConfig;
    protected final com.typesafe.config.Config customConfig;
    protected com.typesafe.config.Config rootConfig;

    protected final com.typesafe.config.Config applicationConfig;

    @Getter
    protected final Config config;
    @Getter
    protected final PersistenceService persistenceService;
    private final MigrationService migrationService;
    private InstanceLockManager instanceLockManager;
    @Getter
    protected final Observable<State> state = new Observable<>(State.INITIALIZE_APP);

    public ApplicationService(String configFileName, String[] args, Path userDataDir) {
        programArgConfig = TypesafeConfigUtils.parseArgsToConfig(args);
        jvmConfig = TypesafeConfigUtils.resolveFilteredJvmOptions();
        resourceConfig = ConfigFactory.parseResources(configFileName + ".conf").resolve();
        // We do not check validity yet, as we do not have a reference config. Typesafe use then the existing config
        // which is the preliminary config from program args and jvm args but that is not a useful reference.

        // Precedence Order: Program Arguments > JVM options > Resource config
        rootConfig = programArgConfig
                .withFallback(jvmConfig)
                .withFallback(resourceConfig)
                .resolve();

        String appName = rootConfig.getString("application.appName");
        Path baseDir = rootConfig.hasPath("application.baseDir")
                ? Path.of(rootConfig.getString("application.baseDir"))
                : userDataDir.resolve(appName);
        try {
            FileUtils.makeDirs(baseDir.toFile());
        } catch (IOException e) {
            log.error("Could not create data directory {}", baseDir, e);
            throw new RuntimeException(e);
        }

        setupLogging(baseDir);

        log.info(AsciiLogo.getAsciiLogo());
        log.info("Data directory: {}", baseDir);
        log.info("Version: v{} / Commit hash: {}", ApplicationVersion.getVersion().getVersionAsString(), ApplicationVersion.getBuildCommitShortHash());
        log.info("Tor Version: v{}", ApplicationVersion.getTorVersionString());

        customConfig = TypesafeConfigUtils.resolveCustomConfig(baseDir).orElse(ConfigFactory.empty());

        // Precedence Order: Program Arguments > JVM options > Custom config > Resource config
        rootConfig = programArgConfig
                .withFallback(jvmConfig)
                .withFallback(customConfig)
                .withFallback(resourceConfig)
                .resolve();

        applicationConfig = rootConfig.getConfig("application");
        config = Config.from(rootConfig, applicationConfig, baseDir);

        DevMode.setDevMode(config.isDevMode());
        if (config.isDevMode()) {
            DevMode.setDevModeReputationScore(config.getDevModeReputationScore());
            DevMode.setDevModeWalletSetup(config.isDevModeWalletSetup());
        }

        if (config.isCheckInstanceLock()) {
            checkInstanceLock();
        }

        Locale locale = LocaleRepository.getDefaultLocale();
        CountryRepository.applyDefaultLocale(locale);
        LanguageRepository.setDefaultLanguage(locale.getLanguage());
        FiatCurrencyRepository.setLocale(locale);
        Res.setAndApplyLanguage(LanguageRepository.getDefaultLanguage());
        ResolverConfig.config();

        String absoluteDataDirPath = baseDir.toAbsolutePath().toString();
        persistenceService = new PersistenceService(absoluteDataDirPath);
        migrationService = new MigrationService(baseDir);
    }

    public CompletableFuture<Void> pruneAllBackups() {
        return persistenceService.pruneAllBackups();
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return migrationService.initialize();
    }

    @Override
    public abstract CompletableFuture<Boolean> shutdown();

    protected com.typesafe.config.Config getConfig(String path) {
        return applicationConfig.getConfig(path);
    }

    protected boolean hasConfig(String path) {
        return applicationConfig.hasPath(path);
    }

    protected void setState(State newState) {
        checkArgument(state.get().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state.get());
        state.set(newState);
        log.info("New state {}", newState);
    }

    protected void setupLogging(Path dataDir) {
        com.typesafe.config.Config loggingConfig = getConfig("logging");
        int rollingPolicyMaxIndex = loggingConfig.getInt("rollingPolicyMaxIndex");
        String maxFileSize = loggingConfig.getString("maxFileSize");
        Level logLevel = Level.toLevel(loggingConfig.getString("logLevel"));
        LogSetup.setup(dataDir.resolve("bisq").toString(), rollingPolicyMaxIndex, maxFileSize, logLevel);
    }

    protected void checkInstanceLock() {
        // Create a quasi-unique port per data directory
        // Dynamic/private ports: 49152 – 65535
        int lowestPort = 49152;
        int highestPort = 65535;
        int port = lowestPort + Math.abs(config.getBaseDir().hashCode() % (highestPort - lowestPort));
        instanceLockManager = new InstanceLockManager();
        instanceLockManager.acquireLock(port);

        // We release the instance lock with a shutdown hook to ensure it gets release in all cases.
        // If we throw the NoFileLockException we are still in constructor call and the ApplicationService instance is
        // not created, thus shutdown would not be called. Therefor the shutdown hook is a more reliable solution.
        // Usually we try to avoid adding multiple shutdownHooks as the order of their execution is not
        // defined. In that case the order from other hooks has no impact.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("InstanceLockManager.releaseLock");
            instanceLockManager.releaseLock();
        }));
    }
}

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

import bisq.account.accountage.AccountAgeWitnessData;
import bisq.common.application.DevMode;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.logging.LogSetup;
import bisq.common.util.OsUtils;
import bisq.i18n.Res;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.services.data.storage.DistributedDataResolver;
import bisq.offer.Offer;
import bisq.oracle.daobridge.model.AuthorizedProofOfBurnData;
import bisq.persistence.PersistenceService;
import bisq.chat.message.ChatMessage;
import bisq.user.profile.UserProfile;
import ch.qos.logback.classic.Level;
import com.typesafe.config.ConfigFactory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

            // todo @alvasw can we use the typesafeConfig instead?
            String dataDir = null;
            boolean isBitcoindRegtest = false;
            boolean isElementsdRegtest = false;
            for (String arg : args) {
                if (arg.startsWith("--appName")) {
                    appName = arg.split("=")[1];
                }

                if (arg.startsWith("--data-dir")) {
                    dataDir = arg.split("=")[1];
                }

                if (arg.startsWith("--regtest-bitcoind")) {
                    isBitcoindRegtest = true;
                }

                if (arg.startsWith("--regtest-elementsd")) {
                    isElementsdRegtest = true;
                }
            }

            String appDir = dataDir == null ? OsUtils.getUserDataDir() + File.separator + appName : dataDir;
            log.info("Use application directory {}", appDir);

            String version = config.getString("version");

            return new Config(appDir, appName, version, devMode, isBitcoindRegtest, isElementsdRegtest);
        }

        private final String baseDir;
        private final String appName;
        private final String version;
        private final boolean devMode;
        private final boolean isBitcoindRegtest;
        private final boolean isElementsdRegtest;

        public Config(String baseDir,
                      String appName,
                      String version,
                      boolean devMode,
                      boolean isBitcoindRegtest,
                      boolean isElementsdRegtest) {
            this.baseDir = baseDir;
            this.appName = appName;
            this.version = version;
            this.devMode = devMode;
            this.isBitcoindRegtest = isBitcoindRegtest;
            this.isElementsdRegtest = isElementsdRegtest;
        }
    }

    private final com.typesafe.config.Config typesafeAppConfig;
    @Getter
    protected final Config config;
    @Getter
    protected final PersistenceService persistenceService;

    public ApplicationService(String configFileName, String[] args) {
        com.typesafe.config.Config typesafeConfig = ConfigFactory.load(configFileName);
        typesafeConfig.checkValid(ConfigFactory.defaultReference(), configFileName);

        typesafeAppConfig = typesafeConfig.getConfig("application");
        config = Config.from(typesafeAppConfig, args);

        LogSetup.setup(Paths.get(config.getBaseDir(), "bisq").toString());
        LogSetup.setLevel(Level.INFO);

        DevMode.setDevMode(config.isDevMode());

        Locale locale = LocaleRepository.getDefaultLocale();
        CountryRepository.setLocale(locale);
        LanguageRepository.setLocale(locale);
        FiatCurrencyRepository.setLocale(locale);
        Res.setLocale(locale);

        // Register resolvers for distributedData 
        DistributedDataResolver.addResolver("chat.ChatMessage", ChatMessage.getDistributedDataResolver());
        DistributedDataResolver.addResolver("user.UserProfile", UserProfile.getResolver());
        DistributedDataResolver.addResolver("offer.Offer", Offer.getResolver());
        DistributedDataResolver.addResolver("oracle.AuthorizedProofOfBurnData", AuthorizedProofOfBurnData.getResolver());
        DistributedDataResolver.addResolver("account.AccountAgeWitnessData", AccountAgeWitnessData.getResolver());

        // Register resolvers for networkMessages 
        NetworkMessageResolver.addResolver("chat.ChatMessage", ChatMessage.getNetworkMessageResolver());

        persistenceService = new PersistenceService(config.getBaseDir());
    }

    public CompletableFuture<Boolean> readAllPersisted() {
        return persistenceService.readAllPersisted();
    }

    public abstract CompletableFuture<Boolean> initialize();

    public abstract CompletableFuture<Boolean> shutdown();

    protected com.typesafe.config.Config getConfig(String path) {
        return typesafeAppConfig.getConfig(path);
    }
}

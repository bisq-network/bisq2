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

package bisq.trade;

import bisq.account.AccountService;
import bisq.bonded_roles.BondedRolesService;
import bisq.burningman.BurningmanService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.contract.ContractService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.offer.OfferService;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.mu_sig.DelayedPayoutTxReceiverService;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class TradeService implements Service, ServiceProvider {
    @Getter
    public static class Config {
        private final com.typesafe.config.Config muSigConfig;

        public Config(com.typesafe.config.Config muSigConfig) {
            this.muSigConfig = muSigConfig;
        }

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getConfig("muSig"));
        }
    }

    private final BisqEasyTradeService bisqEasyTradeService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final PersistenceService persistenceService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final SupportService supportService;
    private final ChatService chatService;
    private final BondedRolesService bondedRolesService;
    private final UserService userService;
    private final SettingsService settingsService;
    private final AccountService accountService;
    private final MuSigTradeService muSigTradeService;
    private final DelayedPayoutTxReceiverService delayedPayoutTxReceiverService;
    private final BurningmanService burningmanService;

    public TradeService(Config config,
                        NetworkService networkService,
                        IdentityService identityService,
                        PersistenceService persistenceService,
                        OfferService offerService,
                        ContractService contractService,
                        SupportService supportService,
                        ChatService chatService,
                        BondedRolesService bondedRolesService,
                        UserService userService,
                        SettingsService settingsService,
                        AccountService accountService,
                        BurningmanService burningmanService) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.persistenceService = persistenceService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.supportService = supportService;
        this.chatService = chatService;
        this.bondedRolesService = bondedRolesService;
        this.userService = userService;
        this.settingsService = settingsService;
        this.accountService = accountService;
        this.burningmanService = burningmanService;

        delayedPayoutTxReceiverService = new DelayedPayoutTxReceiverService(burningmanService);

        bisqEasyTradeService = new BisqEasyTradeService(this);
        muSigTradeService = new MuSigTradeService(MuSigTradeService.Config.from(config.getMuSigConfig()),
                this);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return delayedPayoutTxReceiverService.initialize()
                .thenCompose(result -> bisqEasyTradeService.initialize());

    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return delayedPayoutTxReceiverService.shutdown()
                .thenCompose(result -> bisqEasyTradeService.shutdown());
    }

    public CompletableFuture<Boolean> initializeMuSigTradeService() {
        log.info("initialize MuSigTradeService");
        return muSigTradeService.initialize();
    }

    public CompletableFuture<Boolean> shutdownMuSigTradeService() {
        log.info("shutdown MuSigTradeService");
        return muSigTradeService.shutdown();
    }
}
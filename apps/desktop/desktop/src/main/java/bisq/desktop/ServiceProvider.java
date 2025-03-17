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

package bisq.desktop;

import bisq.account.AccountService;
import bisq.application.ApplicationService;
import bisq.application.ShutDownHandler;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.ChatService;
import bisq.contract.ContractService;
import bisq.desktop.webcam.WebcamAppService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.offer.OfferService;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.security.SecurityService;
import bisq.settings.DontShowAgainService;
import bisq.settings.FavouriteMarketsService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.evolution.updater.UpdaterService;
import bisq.user.UserService;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class ServiceProvider {
    private final ShutDownHandler shutDownHandler;
    private final ApplicationService.Config config;
    private final PersistenceService persistenceService;
    private final SecurityService securityService;
    private final Optional<WalletService> walletService;
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final BondedRolesService bondedRolesService;
    private final AccountService accountService;
    private final OfferService offerService;
    private final ContractService contractService;
    private final UserService userService;
    private final ChatService chatService;
    private final SettingsService settingsService;
    private final SupportService supportService;
    private final SystemNotificationService systemNotificationService;
    private final TradeService tradeService;
    private final UpdaterService updaterService;
    private final BisqEasyService bisqEasyService;
    private final AlertNotificationsService alertNotificationsService;
    private final FavouriteMarketsService favouriteMarketsService;
    private final DontShowAgainService dontShowAgainService;
    private final WebcamAppService webcamAppService;

    public ServiceProvider(ShutDownHandler shutDownHandler,
                           ApplicationService.Config config,
                           PersistenceService persistenceService,
                           SecurityService securityService,
                           Optional<WalletService> walletService,
                           NetworkService networkService,
                           IdentityService identityService,
                           BondedRolesService bondedRolesService,
                           AccountService accountService,
                           OfferService offerService,
                           ContractService contractService,
                           UserService userService,
                           ChatService chatService,
                           SettingsService settingsService,
                           SupportService supportService,
                           SystemNotificationService systemNotificationService,
                           TradeService tradeService,
                           UpdaterService updaterService,
                           BisqEasyService bisqEasyService,
                           AlertNotificationsService alertNotificationsService,
                           FavouriteMarketsService favouriteMarketsService,
                           DontShowAgainService dontShowAgainService,
                           WebcamAppService webcamAppService) {
        this.shutDownHandler = shutDownHandler;
        this.config = config;
        this.persistenceService = persistenceService;
        this.securityService = securityService;
        this.walletService = walletService;
        this.networkService = networkService;
        this.identityService = identityService;
        this.bondedRolesService = bondedRolesService;
        this.accountService = accountService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.userService = userService;
        this.chatService = chatService;
        this.settingsService = settingsService;
        this.supportService = supportService;
        this.systemNotificationService = systemNotificationService;
        this.tradeService = tradeService;
        this.updaterService = updaterService;
        this.bisqEasyService = bisqEasyService;
        this.alertNotificationsService = alertNotificationsService;
        this.favouriteMarketsService = favouriteMarketsService;
        this.dontShowAgainService = dontShowAgainService;
        this.webcamAppService = webcamAppService;
    }
}

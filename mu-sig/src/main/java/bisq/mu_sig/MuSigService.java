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

package bisq.mu_sig;

import bisq.account.AccountService;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.chat.ChatService;
import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.currency.Market;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.contract.ContractService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.OfferService;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.price.spec.PriceSpec;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class MuSigService implements Service {
    private final PersistenceService persistenceService;
    private final SecurityService securityService;
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
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final AlertService alertService;

    private final Observable<Boolean> muSigActivated = new Observable<>(false);
    private Pin cookieChangedPin;

    public MuSigService(PersistenceService persistenceService,
                        SecurityService securityService,
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
                        TradeService tradeService) {
        this.persistenceService = persistenceService;
        this.securityService = securityService;
        this.networkService = networkService;
        this.identityService = identityService;
        this.bondedRolesService = bondedRolesService;
        marketPriceService = bondedRolesService.getMarketPriceService();
        this.accountService = accountService;
        this.offerService = offerService;
        this.contractService = contractService;
        this.userService = userService;
        this.chatService = chatService;
        this.settingsService = settingsService;
        this.supportService = supportService;
        this.systemNotificationService = systemNotificationService;
        this.tradeService = tradeService;
        userIdentityService = userService.getUserIdentityService();
        alertService = bondedRolesService.getAlertService();
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        cookieChangedPin = settingsService.getMuSigActivated().addObserver(activated -> {
            muSigActivated.set(DevMode.isDevMode() && activated);
            if (muSigActivated.get()) {
                activate();
            } else {
                deactivate();
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (cookieChangedPin != null) {
            cookieChangedPin.unbind();
            cookieChangedPin = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    private void activate() {
        log.info("activate");
        tradeService.initializeMuSigTradeService();
    }

    private void deactivate() {
        log.info("deactivate");
        tradeService.shutdownMuSigTradeService();
    }

    public MuSigOffer createAndGetMuSigOffer(Direction direction,
                                             Market market,
                                             AmountSpec amountSpec,
                                             PriceSpec priceSpec,
                                             List<FiatPaymentMethod> fiatPaymentMethods,
                                             List<OfferOption> offerOptions) {
        NetworkId makerNetworkId = userIdentityService.getSelectedUserIdentity().getUserProfile().getNetworkId();
        return new MuSigOffer(makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                fiatPaymentMethods,
                offerOptions,
                MuSigProtocol.VERSION);
    }
}

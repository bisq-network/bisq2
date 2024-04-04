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

package bisq.bisq_easy;

import bisq.account.AccountService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.contract.ContractService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.offer.OfferService;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SendNotificationService;
import bisq.security.SecurityService;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class BisqEasyService implements Service {
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
    private final SendNotificationService sendNotificationService;
    private final TradeService tradeService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final Observable<Long> minRequiredReputationScore = new Observable<>();
    private Pin difficultyAdjustmentFactorPin, ignoreDiffAdjustmentFromSecManagerPin,
            mostRecentDiffAdjustmentValueOrDefaultPin, minRequiredReputationScorePin,
            ignoreMinRequiredReputationScoreFromSecManagerPin, mostRecentMinRequiredReputationScoreOrDefaultPin;

    public BisqEasyService(PersistenceService persistenceService,
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
                           SendNotificationService sendNotificationService,
                           TradeService tradeService) {
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
        this.sendNotificationService = sendNotificationService;
        this.tradeService = tradeService;
        userIdentityService = userService.getUserIdentityService();

        bisqEasyNotificationsService = new BisqEasyNotificationsService(chatService.getChatNotificationService(),
                supportService.getMediatorService());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        difficultyAdjustmentFactorPin = settingsService.getDifficultyAdjustmentFactor().addObserver(e -> applyDifficultyAdjustmentFactor());
        ignoreDiffAdjustmentFromSecManagerPin = settingsService.getIgnoreDiffAdjustmentFromSecManager().addObserver(e -> applyDifficultyAdjustmentFactor());
        mostRecentDiffAdjustmentValueOrDefaultPin = bondedRolesService.getDifficultyAdjustmentService().getMostRecentValueOrDefault().addObserver(e -> applyDifficultyAdjustmentFactor());

        minRequiredReputationScorePin = settingsService.getMinRequiredReputationScore().addObserver(e -> applyMinRequiredReputationScore());
        ignoreMinRequiredReputationScoreFromSecManagerPin = settingsService.getIgnoreMinRequiredReputationScoreFromSecManager().addObserver(e -> applyMinRequiredReputationScore());
        mostRecentMinRequiredReputationScoreOrDefaultPin = bondedRolesService.getMinRequiredReputationScoreService().getMostRecentValueOrDefault().addObserver(e -> applyMinRequiredReputationScore());

        return bisqEasyNotificationsService.initialize();
    }


    public CompletableFuture<Boolean> shutdown() {
        if (difficultyAdjustmentFactorPin != null) {
            difficultyAdjustmentFactorPin.unbind();
            ignoreDiffAdjustmentFromSecManagerPin.unbind();
            mostRecentDiffAdjustmentValueOrDefaultPin.unbind();
            minRequiredReputationScorePin.unbind();
            ignoreMinRequiredReputationScoreFromSecManagerPin.unbind();
            mostRecentMinRequiredReputationScoreOrDefaultPin.unbind();
        }
        return bisqEasyNotificationsService.shutdown();
    }

    public boolean isDeleteUserIdentityProhibited(UserIdentity userIdentity) {
        return chatService.isIdentityUsed(userIdentity) ||
                !userIdentityService.hasMultipleUserIdentities();
    }

    public CompletableFuture<BroadcastResult> deleteUserIdentity(UserIdentity userIdentity) {
        if (isDeleteUserIdentityProhibited(userIdentity)) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted"));
        }
        return userIdentityService.deleteUserIdentity(userIdentity);
    }

    private void applyDifficultyAdjustmentFactor() {
        networkService.getNetworkLoadService().ifPresent(service -> {
            if (settingsService.getIgnoreDiffAdjustmentFromSecManager().get()) {
                service.setDifficultyAdjustmentFactor(settingsService.getDifficultyAdjustmentFactor().get());
            } else {
                service.setDifficultyAdjustmentFactor(bondedRolesService.getDifficultyAdjustmentService().getMostRecentValueOrDefault().get());
            }
        });
    }

    private void applyMinRequiredReputationScore() {
        if (settingsService.getIgnoreMinRequiredReputationScoreFromSecManager().get()) {
            minRequiredReputationScore.set(settingsService.getMinRequiredReputationScore().get());
        } else {
            minRequiredReputationScore.set(bondedRolesService.getMinRequiredReputationScoreService().getMostRecentValueOrDefault().get());
        }
    }
}

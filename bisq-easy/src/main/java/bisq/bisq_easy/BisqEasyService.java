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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.common.application.Service;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.util.CompletableFutureUtils;
import bisq.contract.ContractService;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.resend.ResendMessageData;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.offer.OfferService;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.security.SecurityService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.support.SupportService;
import bisq.trade.TradeService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    private final SystemNotificationService systemNotificationService;
    private final TradeService tradeService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final MarketPriceService marketPriceService;
    private Pin difficultyAdjustmentFactorPin, ignoreDiffAdjustmentFromSecManagerPin,
            mostRecentDiffAdjustmentValueOrDefaultPin, selectedMarketPin;

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
                           SystemNotificationService systemNotificationService,
                           TradeService tradeService) {
        this.persistenceService = persistenceService;
        this.securityService = securityService;
        this.walletService = walletService;
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

        bisqEasyNotificationsService = new BisqEasyNotificationsService(chatService.getChatNotificationService(),
                supportService.getMediatorService(),
                chatService.getBisqEasyOfferbookChannelService(),
                settingsService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        difficultyAdjustmentFactorPin = settingsService.getDifficultyAdjustmentFactor().addObserver(e -> applyDifficultyAdjustmentFactor());
        ignoreDiffAdjustmentFromSecManagerPin = settingsService.getIgnoreDiffAdjustmentFromSecManager().addObserver(e -> applyDifficultyAdjustmentFactor());
        mostRecentDiffAdjustmentValueOrDefaultPin = bondedRolesService.getDifficultyAdjustmentService().getMostRecentValueOrDefault().addObserver(e -> applyDifficultyAdjustmentFactor());

        settingsService.getCookie().asString(CookieKey.SELECTED_MARKET_CODES)
                .flatMap(MarketRepository::findAnyFiatMarketByMarketCodes)
                .ifPresentOrElse(marketPriceService::setSelectedMarket,
                        () -> marketPriceService.setSelectedMarket(MarketRepository.getDefault()));

        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(market -> {
            if (market != null) {
                settingsService.setCookie(CookieKey.SELECTED_MARKET_CODES, market.getMarketCodes());
            }
        });

        return bisqEasyNotificationsService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (difficultyAdjustmentFactorPin != null) {
            difficultyAdjustmentFactorPin.unbind();
            ignoreDiffAdjustmentFromSecManagerPin.unbind();
            mostRecentDiffAdjustmentValueOrDefaultPin.unbind();
            selectedMarketPin.unbind();
        }

        return getStorePendingMessagesInMailboxFuture()
                .thenCompose(e -> bisqEasyNotificationsService.shutdown());
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


    // At shutdown, we store all pending messages (CONNECT, SENT or TRY_ADD_TO_MAILBOX state) in the mailbox.
    // We wait until all broadcast futures are completed or timeout if it takes longer as expected.
    private CompletableFuture<Boolean> getStorePendingMessagesInMailboxFuture() {
        return CompletableFutureUtils.allOf(getStorePendingMessagesInMailboxFutures())
                .orTimeout(20, TimeUnit.SECONDS)
                .handle((broadcastResultList, throwable) -> {
                    if (throwable != null) {
                        log.warn("flushPendingMessagesToMailboxAtShutdown failed", throwable);
                        return false;
                    } else {
                        log.info("All broadcast futures at getStorePendingMessagesInMailboxFuture completed. broadcastResultList={}", broadcastResultList);
                        try {
                            // We delay a bit before continuing shutdown process. Usually we only have 1 pending message...
                            Thread.sleep(100 + broadcastResultList.size() * 500L);
                        } catch (InterruptedException ignore) {
                        }
                        return true;
                    }
                });
    }

    private Stream<CompletableFuture<bisq.network.p2p.services.data.broadcast.BroadcastResult>> getStorePendingMessagesInMailboxFutures() {
        Set<ResendMessageData> pendingResendMessageDataSet = networkService.getPendingResendMessageDataSet();
        return networkService.getConfidentialMessageServices().stream()
                .flatMap(confidentialMessageService ->
                        pendingResendMessageDataSet.stream()
                                .flatMap(resendMessageData ->
                                        userIdentityService.findUserIdentity(resendMessageData.getSenderNetworkId().getId())
                                                .map(userIdentity -> userIdentity.getNetworkIdWithKeyPair().getKeyPair())
                                                .flatMap(senderKeyPair -> confidentialMessageService.flushPendingMessagesToMailboxAtShutdown(resendMessageData, senderKeyPair)).stream()
                                )
                )
                .flatMap(sendConfidentialMessageResult -> sendConfidentialMessageResult.getMailboxFuture().stream())
                .flatMap(Collection::stream);
    }

    private void applyDifficultyAdjustmentFactor() {
        networkService.getNetworkLoadServices().forEach(networkLoadService -> {
            if (settingsService.getIgnoreDiffAdjustmentFromSecManager().get()) {
                networkLoadService.setDifficultyAdjustmentFactor(settingsService.getDifficultyAdjustmentFactor().get());
            } else {
                networkLoadService.setDifficultyAdjustmentFactor(bondedRolesService.getDifficultyAdjustmentService().getMostRecentValueOrDefault().get());
            }
        });
    }
}

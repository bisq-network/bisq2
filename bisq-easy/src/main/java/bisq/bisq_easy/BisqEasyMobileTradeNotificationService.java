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

import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.i18n.Res;
import bisq.notifications.NotificationService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emits mobile-push notifications for the trade-state transitions that the user actually
 * needs to act on, mirroring the Android nodeApp's {@code OpenTradesNotificationService}
 * which has proven sound at scale.
 * <p>
 * Why this exists: the {@link bisq.chat.notifications.ChatNotificationService} push path
 * was suppressing {@code PROTOCOL_LOG_MESSAGE} chat events for mobile (issue
 * {@code bisq-network/bisq-mobile#1450} — they fired 6–10+ times per active trade and
 * spammed users). That filter killed the noise AND the few signal events. This service
 * re-introduces the useful signal events on the mobile relay path, observing
 * {@link BisqEasyTrade#tradeStateObservable()} directly with a tight whitelist of states.
 * <p>
 * Dispatch is mobile-only (via {@link NotificationService#dispatchMobileOnlyNotification})
 * because the desktop already surfaces these events through the in-app trade chat's
 * protocol log and the trade detail header — we don't want desktop users to see a
 * duplicate OS-level toast.
 * <p>
 * Per-trade {@code notifiedPaymentInfo} dedup mirrors the nodeApp guard documented in
 * {@code OpenTradesNotificationService}: the offer-response state AND the payment-data
 * observer can each fire for the same logical "payment account info exchanged" event,
 * so we collapse them to one push per trade.
 * <p>
 * Whitelist of states (in order):
 * <ul>
 *   <li>{@code BUYER_SENT_FIAT_SENT_CONFIRMATION} — buyer-side push: "you sent fiat";
 *       seller-side push: "peer sent fiat".</li>
 *   <li>{@code SELLER_RECEIVED_FIAT_SENT_CONFIRMATION} — seller-side: "you received
 *       the fiat-sent confirmation"; buyer-side: "you sent fiat".</li>
 *   <li>{@code BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION} /
 *       {@code SELLER_CONFIRMED_FIAT_RECEIPT} — buyer-side: "peer confirmed receipt";
 *       seller-side: "you confirmed receipt".</li>
 *   <li>{@code SELLER_SENT_BTC_SENT_CONFIRMATION} — seller-side: "you sent BTC";
 *       buyer-side: "peer sent BTC".</li>
 *   <li>{@code BUYER_RECEIVED_BTC_SENT_CONFIRMATION} — buyer-side: "you received BTC
 *       confirmation"; seller-side: "you sent BTC".</li>
 *   <li>{@code TAKER_SENT_TAKE_OFFER_REQUEST} — maker-side only: "your offer was
 *       taken" (the taker initiated, no push needed there).</li>
 *   <li>{@code MAKER_SENT_TAKE_OFFER_RESPONSE__*} (2 variants) — "offer taken"
 *       observed from the response side.</li>
 *   <li>{@code TAKER_RECEIVED_TAKE_OFFER_RESPONSE__*} (3 variants) — payment-info
 *       exchanged; gated by per-trade dedup so the user gets ONE "payment info
 *       sent/received" push regardless of which observer fires first.</li>
 *   <li>Terminal states ({@code BTC_CONFIRMED}, {@code REJECTED}, {@code PEER_REJECTED},
 *       {@code CANCELLED}, {@code PEER_CANCELLED}, {@code FAILED}, {@code FAILED_AT_PEER}).
 * </ul>
 * <p>
 * All other states are intentionally silent — they're intermediate protocol bookkeeping
 * the user doesn't need a push for. Same set as Android nodeApp.
 */
@Slf4j
public class BisqEasyMobileTradeNotificationService implements Service {
    private final BisqEasyTradeService bisqEasyTradeService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    /** Per-trade state-observer Pins so we can unbind on trade removal / shutdown. */
    private final Map<String, Pin> tradeStatePins = new ConcurrentHashMap<>();

    /**
     * Trade IDs whose state observer has fired at least once. {@code Observable#addObserver}
     * fires synchronously with the current value when subscribed — we treat that first
     * emission as "we just started watching" rather than a real transition, so existing
     * trades don't re-notify on service startup. Subsequent emissions are real transitions.
     */
    private final Set<String> tradesWithInitializedState = ConcurrentHashMap.newKeySet();

    /**
     * Trades for which we've already emitted a "payment account info exchanged" push.
     * Mirrors {@code OpenTradesNotificationService.notifiedPaymentInfo} on Android
     * nodeApp.
     */
    private final Set<String> notifiedPaymentInfo = ConcurrentHashMap.newKeySet();

    @Nullable
    private Pin tradesPin;

    public BisqEasyMobileTradeNotificationService(BisqEasyTradeService bisqEasyTradeService,
                                                  UserProfileService userProfileService,
                                                  NotificationService notificationService) {
        this.bisqEasyTradeService = bisqEasyTradeService;
        this.userProfileService = userProfileService;
        this.notificationService = notificationService;
    }


    /* --------------------------------------------------------------------- */
    // Service lifecycle
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<BisqEasyTrade>() {
            @Override
            public void onAdded(BisqEasyTrade trade) {
                subscribeToTrade(trade);
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof BisqEasyTrade trade) {
                    unsubscribeFromTrade(trade);
                }
            }

            @Override
            public void onCleared() {
                clearAllState();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (tradesPin != null) {
            tradesPin.unbind();
            tradesPin = null;
        }
        clearAllState();
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // Per-trade subscription
    /* --------------------------------------------------------------------- */

    private void subscribeToTrade(BisqEasyTrade trade) {
        String tradeId = trade.getId();
        Pin previous = tradeStatePins.remove(tradeId);
        if (previous != null) {
            previous.unbind();
        }
        Pin pin = trade.tradeStateObservable().addObserver(state -> onStateEmitted(trade, state));
        tradeStatePins.put(tradeId, pin);
    }

    private void unsubscribeFromTrade(BisqEasyTrade trade) {
        Pin pin = tradeStatePins.remove(trade.getId());
        if (pin != null) {
            pin.unbind();
        }
        tradesWithInitializedState.remove(trade.getId());
        notifiedPaymentInfo.remove(trade.getId());
    }

    private void clearAllState() {
        tradeStatePins.values().forEach(Pin::unbind);
        tradeStatePins.clear();
        tradesWithInitializedState.clear();
        notifiedPaymentInfo.clear();
    }

    private void onStateEmitted(BisqEasyTrade trade, BisqEasyTradeState state) {
        if (state == null) {
            return;
        }
        String tradeId = trade.getId();
        boolean isFirstEmission = tradesWithInitializedState.add(tradeId);
        if (isFirstEmission) {
            log.debug("Skipping initial state observation for trade {} (state={})", tradeId, state);
            return;
        }
        try {
            handleStateChange(trade, state);
        } catch (Exception e) {
            log.error("Failed to handle mobile-push trade state change for trade {} state {}", tradeId, state, e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Whitelist + dispatch
    /* --------------------------------------------------------------------- */

    private void handleStateChange(BisqEasyTrade trade, BisqEasyTradeState state) {
        switch (state) {
            case BUYER_SENT_FIAT_SENT_CONFIRMATION -> {
                if (trade.isBuyer()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.youSentFiat.title",
                            "bisqEasy.mobileNotifications.youSentFiat.message");
                } else {
                    dispatch(trade, "bisqEasy.mobileNotifications.peerSentFiat.title",
                            "bisqEasy.mobileNotifications.peerSentFiat.message");
                }
            }

            case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION -> {
                if (trade.isSeller()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.youReceivedFiatConfirmation.title",
                            "bisqEasy.mobileNotifications.youReceivedFiatConfirmation.message");
                } else {
                    dispatch(trade, "bisqEasy.mobileNotifications.youSentFiat.title",
                            "bisqEasy.mobileNotifications.youSentFiat.message");
                }
            }

            case BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
                 SELLER_CONFIRMED_FIAT_RECEIPT -> {
                if (trade.isBuyer()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.peerReceivedFiat.title",
                            "bisqEasy.mobileNotifications.peerReceivedFiat.message");
                } else {
                    dispatch(trade, "bisqEasy.mobileNotifications.youReceivedFiat.title",
                            "bisqEasy.mobileNotifications.youReceivedFiat.message");
                }
            }

            case SELLER_SENT_BTC_SENT_CONFIRMATION -> {
                if (trade.isSeller()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.youSentBtc.title",
                            "bisqEasy.mobileNotifications.youSentBtc.message");
                } else {
                    dispatch(trade, "bisqEasy.mobileNotifications.peerSentBtc.title",
                            "bisqEasy.mobileNotifications.peerSentBtc.message");
                }
            }

            case BUYER_RECEIVED_BTC_SENT_CONFIRMATION -> {
                if (trade.isBuyer()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.youReceivedBtc.title",
                            "bisqEasy.mobileNotifications.youReceivedBtc.message");
                } else {
                    dispatch(trade, "bisqEasy.mobileNotifications.youSentBtc.title",
                            "bisqEasy.mobileNotifications.youSentBtc.message");
                }
            }

            case TAKER_SENT_TAKE_OFFER_REQUEST -> {
                // Notify the maker only — the taker initiated and already knows.
                if (trade.isMaker()) {
                    dispatch(trade, "bisqEasy.mobileNotifications.offerTaken.title",
                            "bisqEasy.mobileNotifications.offerTaken.message");
                }
            }

            case MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                 MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA ->
                    dispatch(trade, "bisqEasy.mobileNotifications.offerTaken.title",
                            "bisqEasy.mobileNotifications.offerTaken.message");

            case TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                 TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
                 MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ -> {
                // Payment-info exchange — the offer-response state AND the payment-data
                // change observer can both fire here, so dedup to one push per trade.
                if (notifiedPaymentInfo.add(trade.getId())) {
                    if (trade.isSeller()) {
                        dispatch(trade, "bisqEasy.mobileNotifications.paymentInfoSent.title",
                                "bisqEasy.mobileNotifications.paymentInfoSent.message");
                    } else {
                        dispatch(trade, "bisqEasy.mobileNotifications.paymentInfoReceived.title",
                                "bisqEasy.mobileNotifications.paymentInfoReceived.message");
                    }
                }
            }

            default -> {
                if (state.isFinalState()) {
                    String terminalKey = terminalI18nKey(state);
                    if (terminalKey != null) {
                        dispatchTerminal(trade, terminalKey);
                    }
                }
                // Other non-whitelist, non-terminal states are intentionally silent.
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // Helpers
    /* --------------------------------------------------------------------- */

    private void dispatch(BisqEasyTrade trade, String titleKey, String messageKey) {
        String shortTradeId = shortTradeId(trade);
        String peerName = peerNameFor(trade);
        String title = Res.get(titleKey, shortTradeId);
        String message = Res.get(messageKey, peerName);
        String notificationId = notificationIdFor(trade);
        notificationService.dispatchMobileOnlyNotification(new MobileTradeNotification(notificationId, title, message));
    }

    private void dispatchTerminal(BisqEasyTrade trade, String terminalI18nKey) {
        String shortTradeId = shortTradeId(trade);
        String peerName = peerNameFor(trade);
        String terminalLabel = Res.get(terminalI18nKey);
        String title = Res.get("bisqEasy.mobileNotifications.tradeCompleted.title", shortTradeId);
        String message = Res.get("bisqEasy.mobileNotifications.tradeCompleted.message", peerName, terminalLabel);
        notificationService.dispatchMobileOnlyNotification(
                new MobileTradeNotification(notificationIdFor(trade), title, message));
    }

    @Nullable
    static String terminalI18nKey(BisqEasyTradeState state) {
        return switch (state) {
            case BTC_CONFIRMED -> "bisqEasy.mobileNotifications.terminal.completed";
            case REJECTED -> "bisqEasy.mobileNotifications.terminal.rejected";
            case PEER_REJECTED -> "bisqEasy.mobileNotifications.terminal.peerRejected";
            case CANCELLED -> "bisqEasy.mobileNotifications.terminal.cancelled";
            case PEER_CANCELLED -> "bisqEasy.mobileNotifications.terminal.peerCancelled";
            case FAILED -> "bisqEasy.mobileNotifications.terminal.failed";
            case FAILED_AT_PEER -> "bisqEasy.mobileNotifications.terminal.failedAtPeer";
            default -> null;
        };
    }

    /**
     * Whitelist check — exposed package-private so callers and tests can reason about
     * whether a given state will produce a push without depending on the full handler.
     */
    static boolean isWhitelistedState(BisqEasyTradeState state) {
        return switch (state) {
            case BUYER_SENT_FIAT_SENT_CONFIRMATION,
                 SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
                 BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION,
                 SELLER_CONFIRMED_FIAT_RECEIPT,
                 SELLER_SENT_BTC_SENT_CONFIRMATION,
                 BUYER_RECEIVED_BTC_SENT_CONFIRMATION,
                 TAKER_SENT_TAKE_OFFER_REQUEST,
                 MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                 MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                 TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                 TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_,
                 MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_ -> true;
            default -> state.isFinalState();
        };
    }

    private static String shortTradeId(BisqEasyTrade trade) {
        String id = trade.getId();
        return id.substring(0, Math.min(8, id.length()));
    }

    private String peerNameFor(BisqEasyTrade trade) {
        return userProfileService.findUserProfile(trade.getPeer().getNetworkId().getId())
                .map(UserProfile::getUserName)
                .orElse("?");
    }

    /**
     * Stable per-trade notification id so that subsequent state changes for the same trade
     * REPLACE the previous push on the device rather than stack up another row in the
     * notification tray (this matches Android nodeApp's {@code NotificationIds.getTradeStateUpdatedId(...)}).
     */
    private static String notificationIdFor(BisqEasyTrade trade) {
        return "bisq-easy-mobile-trade-" + shortTradeId(trade);
    }
}

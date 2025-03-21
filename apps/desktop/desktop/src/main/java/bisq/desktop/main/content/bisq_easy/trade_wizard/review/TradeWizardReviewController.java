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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.BisqEasyService;
import bisq.bisq_easy.BisqEasyServiceUtil;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.DevMode;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.ReviewDataDisplay;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.ChatMessageType;
import bisq.settings.SettingsService;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TradeWizardReviewController implements Controller {
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String DASH_SYMBOL = "\u2013"; // Unicode for "–"

    private final TradeWizardReviewModel model;
    @Getter
    private final TradeWizardReviewView view;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final ChatService chatService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BannedUserService bannedUserService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final SettingsService settingsService;
    private final ReviewDataDisplay reviewDataDisplay;
    private final MediationRequestService mediationRequestService;
    private final BisqEasyService bisqEasyService;
    private Pin errorMessagePin, peersErrorMessagePin;
    private UIScheduler timeoutScheduler;

    public TradeWizardReviewController(ServiceProvider serviceProvider,
                                       Consumer<Boolean> mainButtonsVisibleHandler,
                                       Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        settingsService = serviceProvider.getSettingsService();
        bisqEasyService = serviceProvider.getBisqEasyService();
        mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());
        reviewDataDisplay = new ReviewDataDisplay();
        model = new TradeWizardReviewModel();
        view = new TradeWizardReviewView(model, this, reviewDataDisplay.getRoot());
    }

    public void setBitcoinPaymentMethods(List<BitcoinPaymentMethod> bitcoinPaymentMethods) {
        if (bitcoinPaymentMethods != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setSelectedBisqEasyOffer(BisqEasyOffer selectedBisqEasyOffer) {
        if (selectedBisqEasyOffer != null) {
            resetSelectedPaymentMethod();
        }
    }

    public void setDataForCreateOffer(Direction direction,
                                      Market market,
                                      List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                                      List<FiatPaymentMethod> fiatPaymentMethods,
                                      AmountSpec amountSpec,
                                      PriceSpec priceSpec) {
        model.setCreateOfferMode(true);
        checkArgument(!bitcoinPaymentMethods.isEmpty(), "bitcoinPaymentMethods must not be empty");
        checkArgument(!fiatPaymentMethods.isEmpty(), "fiatPaymentMethods must not be empty");
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        String chatMessageText = BisqEasyServiceUtil.createOfferBookMessageFromPeerPerspective(userIdentity.getNickName(),
                marketPriceService,
                direction,
                market,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                amountSpec,
                priceSpec);
        BisqEasyOffer bisqEasyOffer = new BisqEasyOffer(
                userIdentity.getUserProfile().getNetworkId(),
                direction,
                market,
                amountSpec,
                priceSpec,
                new ArrayList<>(bitcoinPaymentMethods),
                new ArrayList<>(fiatPaymentMethods),
                userIdentity.getUserProfile().getTerms(),
                new ArrayList<>(settingsService.getSupportedLanguageCodes()));
        model.setBisqEasyOffer(bisqEasyOffer);

        Optional<BisqEasyOfferbookChannel> optionalChannel = bisqEasyOfferbookChannelService.findChannel(market);
        if (optionalChannel.isPresent()) {
            BisqEasyOfferbookChannel channel = optionalChannel.get();
            model.setSelectedChannel(channel);

            BisqEasyOfferbookMessage myOfferMessage = new BisqEasyOfferbookMessage(channel.getId(),
                    userIdentity.getUserProfile().getId(),
                    Optional.of(bisqEasyOffer),
                    Optional.of(chatMessageText),
                    Optional.empty(),
                    new Date().getTime(),
                    false);

            model.setMyOfferMessage(myOfferMessage);
        } else {
            log.warn("optionalChannel not present");
        }

        applyData(direction,
                market,
                bitcoinPaymentMethods,
                fiatPaymentMethods,
                amountSpec,
                priceSpec);
    }

    public void setDataForTakeOffer(BisqEasyOffer bisqEasyOffer,
                                    AmountSpec amountSpec,
                                    List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                                    List<FiatPaymentMethod> fiatPaymentMethods) {
        if (bisqEasyOffer == null) {
            return;
        }

        model.setCreateOfferMode(false);
        model.setBisqEasyOffer(bisqEasyOffer);
        Direction direction = bisqEasyOffer.getTakersDirection();
        Market market = bisqEasyOffer.getMarket();
        PriceSpec price = bisqEasyOffer.getPriceSpec();

        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = bisqEasyOffer.getBaseSidePaymentMethodSpecs();
        Set<BitcoinPaymentMethod> takersBitcoinPaymentMethodSet = new HashSet<>(bitcoinPaymentMethods);
        List<BitcoinPaymentMethod> bitcoinPaymentMethodsToUse = baseSidePaymentMethodSpecs.stream()
                .filter(e -> takersBitcoinPaymentMethodSet.contains(e.getPaymentMethod()))
                .map(PaymentMethodSpec::getPaymentMethod)
                .collect(Collectors.toList());

        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        Set<FiatPaymentMethod> takersFiatPaymentMethodSet = new HashSet<>(fiatPaymentMethods);
        List<FiatPaymentMethod> fiatPaymentMethodsToUse = quoteSidePaymentMethodSpecs.stream()
                .filter(e -> takersFiatPaymentMethodSet.contains(e.getPaymentMethod()))
                .map(PaymentMethodSpec::getPaymentMethod)
                .collect(Collectors.toList());

        AmountSpec amountSpecToUse = bisqEasyOffer.getAmountSpec() instanceof FixedAmountSpec
                ? bisqEasyOffer.getAmountSpec()
                : amountSpec;

        applyData(direction,
                market,
                bitcoinPaymentMethodsToUse,
                fiatPaymentMethodsToUse,
                amountSpecToUse,
                price);
    }

    // direction is from user perspective not offer direction
    private void applyData(Direction direction,
                           Market market,
                           List<BitcoinPaymentMethod> bitcoinPaymentMethods,
                           List<FiatPaymentMethod> fiatPaymentMethods,
                           AmountSpec amountSpec,
                           PriceSpec priceSpec) {
        boolean isCreateOfferMode = model.isCreateOfferMode();

        String marketCodes = market.getMarketCodes();

        model.setBitcoinPaymentMethods(bitcoinPaymentMethods);
        model.setFiatPaymentMethods(fiatPaymentMethods);
        model.setPriceSpec(priceSpec);
        priceInput.setMarket(market);
        priceInput.setDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.taker", marketCodes));

        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        priceQuote.ifPresent(priceInput::setQuote);
        String formattedPrice = priceQuote
                .map(PriceFormatter::format)
                .orElse("");
        String codes = priceQuote.map(e -> e.getMarket().getMarketCodes()).orElse("");
        model.setPrice(Res.get("bisqEasy.tradeWizard.review.price", formattedPrice, codes));

        applyPriceDetails(model.getPriceSpec(), market);

        String toSendAmountDescription, toSendAmount, toSendCode, toReceiveAmountDescription, toReceiveAmount, toReceiveCode;
        boolean isRangeAmount = amountSpec instanceof RangeAmountSpec;
        if (isRangeAmount) {
            Monetary minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinBaseSideAmount(minBaseSideAmount);
            Monetary maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxBaseSideAmount(maxBaseSideAmount);

            Monetary minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinQuoteSideAmount(minQuoteSideAmount);
            Monetary maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxQuoteSideAmount(maxQuoteSideAmount);

            String formattedMinQuoteAmount = AmountFormatter.formatQuoteAmount(minQuoteSideAmount);
            String formattedMinBaseAmount = AmountFormatter.formatBaseAmount(minBaseSideAmount);
            String formattedMaxQuoteAmount = AmountFormatter.formatQuoteAmount(maxQuoteSideAmount);
            String formattedMaxBaseAmount = AmountFormatter.formatBaseAmount(maxBaseSideAmount);
            if (isCreateOfferMode && direction.isSell()) {
                toSendAmount = formattedMinBaseAmount + " " + DASH_SYMBOL + " " + formattedMaxBaseAmount;
                toSendCode = maxBaseSideAmount.getCode();
                toReceiveAmount = formattedMinQuoteAmount + " " + DASH_SYMBOL + " " + formattedMaxQuoteAmount;
                toReceiveCode = maxQuoteSideAmount.getCode();
            } else {
                toSendAmount = formattedMinQuoteAmount + " " + DASH_SYMBOL + " " + formattedMaxQuoteAmount;
                toSendCode = maxQuoteSideAmount.getCode();
                toReceiveAmount = formattedMinBaseAmount + " " + DASH_SYMBOL + " " + formattedMaxBaseAmount;
                toReceiveCode = maxBaseSideAmount.getCode();
            }
        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixBaseSideAmount(fixBaseSideAmount);
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(fixBaseSideAmount);

            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixQuoteSideAmount(fixQuoteSideAmount);
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(fixQuoteSideAmount);

            if (isCreateOfferMode && direction.isSell()) {
                toSendAmount = formattedBaseAmount;
                toSendCode = fixBaseSideAmount.getCode();
                toReceiveAmount = formattedQuoteAmount;
                toReceiveCode = fixQuoteSideAmount.getCode();
            } else {
                toSendAmount = formattedQuoteAmount;
                toSendCode = fixQuoteSideAmount.getCode();
                toReceiveAmount = formattedBaseAmount;
                toReceiveCode = fixBaseSideAmount.getCode();
            }
        }

        if (isCreateOfferMode) {
            model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.maker"));
            model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.maker").toUpperCase());
            model.setBitcoinPaymentMethodDescription(
                    bitcoinPaymentMethods.size() == 1
                            ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.btc")
                            : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.btc.maker")
            );
            model.setFiatPaymentMethodDescription(
                    fiatPaymentMethods.size() == 1
                            ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.fiat")
                            : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.fiat.maker")
            );
            model.setBitcoinPaymentMethod(PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods));
            model.setFiatPaymentMethod(PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods));
            model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.maker"));
        } else {
            model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.taker"));
            model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.taker").toUpperCase());
            model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.taker"));
            model.getTakersBitcoinPaymentMethods().setAll(bitcoinPaymentMethods);
            model.getTakersFiatPaymentMethods().setAll(fiatPaymentMethods);
            if (model.getTakersSelectedBitcoinPaymentMethod() == null) {
                model.setTakersSelectedBitcoinPaymentMethod(bitcoinPaymentMethods.get(0));
            }
            if (model.getTakersSelectedFiatPaymentMethod() == null) {
                model.setTakersSelectedFiatPaymentMethod(fiatPaymentMethods.get(0));
            }
            model.setBitcoinPaymentMethodDescription(
                    bitcoinPaymentMethods.size() == 1
                            ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.btc")
                            : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.btc.taker")
            );
            model.setFiatPaymentMethodDescription(
                    fiatPaymentMethods.size() == 1
                            ? Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription.fiat")
                            : Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.fiat.taker")
            );
            model.setBitcoinPaymentMethod(model.getTakersSelectedBitcoinPaymentMethod().getDisplayString());
            model.setFiatPaymentMethod(model.getTakersSelectedFiatPaymentMethod().getDisplayString());

        }
        if (direction.isSell()) {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toSend");
        } else {
            toSendAmountDescription = Res.get("bisqEasy.tradeWizard.review.toPay");
        }
        toReceiveAmountDescription = Res.get("bisqEasy.tradeWizard.review.toReceive");

        applyHeaderBitcoinPaymentMethod();
        applyHeaderFiatPaymentMethod();

        model.setRangeAmount(isRangeAmount);
        reviewDataDisplay.setRangeAmount(isRangeAmount);
        reviewDataDisplay.setDirection(Res.get("bisqEasy.tradeWizard.review.direction", Res.get(direction.isSell() ? "offer.sell" : "offer.buy").toUpperCase()));
        reviewDataDisplay.setToSendAmountDescription(toSendAmountDescription.toUpperCase());
        reviewDataDisplay.setToSendAmount(toSendAmount);
        reviewDataDisplay.setToSendCode(toSendCode);
        reviewDataDisplay.setToReceiveAmountDescription(toReceiveAmountDescription.toUpperCase());
        reviewDataDisplay.setToReceiveAmount(toReceiveAmount);
        reviewDataDisplay.setToReceiveCode(toReceiveCode);
        reviewDataDisplay.setBitcoinPaymentMethodDescription(model.getBitcoinPaymentMethodDescription().toUpperCase());
        reviewDataDisplay.setFiatPaymentMethodDescription(model.getFiatPaymentMethodDescription().toUpperCase());
    }

    public void reset() {
        model.reset();
    }

    public void publishOffer() {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();

        String dontShowAgainId = "sendOfferMsgTextOnlyWarn";
        boolean hasShowOnlyTextFilter = settingsService.getBisqEasyOfferbookMessageTypeFilter().get() == ChatMessageType.TEXT;
        if (hasShowOnlyTextFilter) {
            new Popup().information(Res.get("chat.message.send.textMsgOnly.warn"))
                    .actionButtonText(Res.get("confirmation.yes"))
                    .onAction(() -> settingsService.setBisqEasyOfferbookMessageTypeFilter(ChatMessageType.ALL))
                    .closeButtonText(Res.get("confirmation.no"))
                    .dontShowAgainId(dontShowAgainId)
                    .show();
        }
        bisqEasyOfferbookChannelService.publishChatMessage(model.getMyOfferMessage(), userIdentity)
                .thenAccept(result -> UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
                }));
    }

    public void takeOffer() {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        if (bannedUserService.isNetworkIdBanned(bisqEasyOffer.getMakerNetworkId())) {
            new Popup().warning(Res.get("bisqEasy.takeOffer.makerBanned.warning")).show();
            return;
        }
        UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
        if (bannedUserService.isUserProfileBanned(takerIdentity.getUserProfile())) {
            // If taker is banned we don't need to show them a popup
            return;
        }
        Optional<UserProfile> mediator = mediationRequestService.selectMediator(bisqEasyOffer.getMakersUserProfileId(),
                takerIdentity.getId(),
                bisqEasyOffer.getId());
        if (!DevMode.isDevMode() && mediator.isEmpty()) {
            new Popup().warning(Res.get("bisqEasy.takeOffer.noMediatorAvailable.warning"))
                    .closeButtonText(Res.get("action.cancel"))
                    .actionButtonText(Res.get("confirmation.ok"))
                    .onAction(() -> doTakeOffer(bisqEasyOffer, takerIdentity, mediator))
                    .show();
        } else {
            doTakeOffer(bisqEasyOffer, takerIdentity, mediator);
        }
    }

    private void doTakeOffer(BisqEasyOffer bisqEasyOffer, UserIdentity takerIdentity, Optional<UserProfile> mediator) {
        Monetary takersBaseSideAmount = model.getFixBaseSideAmount();
        Monetary takersQuoteSideAmount = model.getFixQuoteSideAmount();
        BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec = new BitcoinPaymentMethodSpec(model.getTakersSelectedBitcoinPaymentMethod());
        FiatPaymentMethodSpec fiatPaymentMethodSpec = new FiatPaymentMethodSpec(model.getTakersSelectedFiatPaymentMethod());
        PriceSpec sellersPriceSpec = model.getPriceSpec();
        long marketPrice = model.getMarketPrice();
        BisqEasyProtocol bisqEasyProtocol = bisqEasyTradeService.createBisqEasyProtocol(takerIdentity.getIdentity(),
                bisqEasyOffer,
                takersBaseSideAmount,
                takersQuoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                sellersPriceSpec,
                marketPrice);
        BisqEasyTrade bisqEasyTrade = bisqEasyProtocol.getModel();
        log.info("Selected mediator for trade {}: {}", bisqEasyTrade.getShortId(), mediator.map(UserProfile::getUserName).orElse("N/A"));
        model.setBisqEasyTrade(bisqEasyTrade);
        errorMessagePin = bisqEasyTrade.errorMessageObservable().addObserver(errorMessage -> {
                    if (errorMessage != null) {
                        UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failed.popup",
                                        errorMessage,
                                        StringUtils.truncate(bisqEasyTrade.getErrorStackTrace(), 2000)))
                                .show());
                    }
                }
        );
        peersErrorMessagePin = bisqEasyTrade.peersErrorMessageObservable().addObserver(peersErrorMessage -> {
                    if (peersErrorMessage != null) {
                        UIThread.run(() -> new Popup().error(Res.get("bisqEasy.openTrades.failedAtPeer.popup",
                                        peersErrorMessage,
                                        StringUtils.truncate(bisqEasyTrade.getPeersErrorStackTrace(), 2000)))
                                .show());
                    }
                }
        );

        bisqEasyTradeService.takeOffer(bisqEasyTrade);
        model.getTakeOfferStatus().set(TradeWizardReviewModel.TakeOfferStatus.SENT);

        BisqEasyContract contract = bisqEasyTrade.getContract();

        mainButtonsVisibleHandler.accept(false);
        String tradeId = bisqEasyTrade.getId();

        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
        timeoutScheduler = UIScheduler.run(() -> {
                    closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY);
                    throw new RuntimeException("Take offer message sending did not succeed after 2 minutes.");
                })
                .after(150, TimeUnit.SECONDS); // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message

        bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.getMediator())
                .thenAccept(result -> UIThread.run(() -> {
                    timeoutScheduler.stop();

                    // In case the user has switched to another market we want to select that market in the offer book
                    ChatChannelSelectionService chatChannelSelectionService =
                            chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
                    bisqEasyOfferbookChannelService.findChannel(contract.getOffer().getMarket())
                            .ifPresent(chatChannelSelectionService::selectChannel);
                    model.getTakeOfferStatus().set(TradeWizardReviewModel.TakeOfferStatus.SUCCESS);
                    bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                            .ifPresent(channel -> {
                                String taker = userIdentityService.getSelectedUserIdentity().getUserProfile().getUserName();
                                String maker = channel.getPeer().getUserName();
                                String encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker);
                                chatService.getBisqEasyOpenTradeChannelService().sendTradeLogMessage(encoded, channel);
                            });
                }));
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);
        Direction direction = model.getBisqEasyOffer().getDirection();
        boolean isMainChain = model.getBitcoinPaymentMethods().stream().anyMatch(e -> e.getPaymentRail() == BitcoinPaymentRail.MAIN_CHAIN);
        model.setFeeDetailsVisible(isMainChain);
        if (direction.isSell()) {
            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFee"));
                model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.noTradeFeesLong"));
            } else {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
            }
        } else {
            if (isMainChain) {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
                model.setFeeDetails(Res.get("bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong"));
            } else {
                model.setFee(Res.get("bisqEasy.tradeWizard.review.noTradeFees"));
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (errorMessagePin != null) {
            errorMessagePin.unbind();
        }
        if (peersErrorMessagePin != null) {
            peersErrorMessagePin.unbind();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.stop();
        }
    }

    void onShowOfferbook() {
        closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY_OFFERBOOK);
    }

    void onShowOpenTrades() {
        closeAndNavigateToHandler.accept(NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }

    void onSelectBitcoinPaymentMethod(BitcoinPaymentMethod paymentMethod) {
        model.setTakersSelectedBitcoinPaymentMethod(paymentMethod);
        applyHeaderBitcoinPaymentMethod();
    }

    void onSelectFiatPaymentMethod(FiatPaymentMethod paymentMethod) {
        model.setTakersSelectedFiatPaymentMethod(paymentMethod);
        applyHeaderFiatPaymentMethod();
    }

    private void resetSelectedPaymentMethod() {
        model.setTakersSelectedBitcoinPaymentMethod(null);
        model.setTakersSelectedFiatPaymentMethod(null);
    }

    private void applyHeaderBitcoinPaymentMethod() {
        List<BitcoinPaymentMethod> bitcoinPaymentMethods = model.getBitcoinPaymentMethods();
        String bitcoinPaymentMethodsString;
        if (model.isCreateOfferMode()) {
            if (bitcoinPaymentMethods.size() > 2) {
                bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods.stream()
                        .limit(2)
                        .collect(Collectors.toList())) + ",...";
            } else {
                bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods);
            }
            bitcoinPaymentMethodsString = StringUtils.truncate(bitcoinPaymentMethodsString, 40);
        } else {
            bitcoinPaymentMethodsString = model.getTakersSelectedBitcoinPaymentMethod().getDisplayString();
        }
        model.setHeaderBitcoinPaymentMethod(bitcoinPaymentMethodsString);
        reviewDataDisplay.setBitcoinPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyHeaderFiatPaymentMethod() {
        List<FiatPaymentMethod> bitcoinPaymentMethods = model.getFiatPaymentMethods();
        String bitcoinPaymentMethodsString;
        if (model.isCreateOfferMode()) {
            if (bitcoinPaymentMethods.size() > 2) {
                bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods.stream()
                        .limit(2)
                        .collect(Collectors.toList())) + ",...";
            } else {
                bitcoinPaymentMethodsString = PaymentMethodSpecFormatter.fromPaymentMethods(bitcoinPaymentMethods);
            }
            bitcoinPaymentMethodsString = StringUtils.truncate(bitcoinPaymentMethodsString, 40);
        } else {
            bitcoinPaymentMethodsString = model.getTakersSelectedFiatPaymentMethod().getDisplayString();
        }
        model.setHeaderFiatPaymentMethod(bitcoinPaymentMethodsString);
        reviewDataDisplay.setFiatPaymentMethod(bitcoinPaymentMethodsString);
    }

    private void applyPriceDetails(PriceSpec priceSpec, Market market) {
        Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
        marketPrice.ifPresent(price -> model.setMarketPrice(price.getPriceQuote().getValue()));
        Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market).map(MarketPrice::getPriceQuote);
        String marketPriceAsString = marketPriceQuote.map(PriceFormatter::formatWithCode).orElse(Res.get("data.na"));
        Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
        double percent = percentFromMarketPrice.orElse(0d);
        if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec) && percent == 0) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails"));
        } else {
            String aboveOrBelow = percent > 0 ? Res.get("offer.price.above") : Res.get("offer.price.below");
            String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("data.na"));
            if (priceSpec instanceof FloatPriceSpec) {
                model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.float",
                        percentAsString, aboveOrBelow, marketPriceAsString));
            } else {
                if (percent == 0) {
                    model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.fix.atMarket",
                            marketPriceAsString));
                } else {
                    model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.fix",
                            percentAsString, aboveOrBelow, marketPriceAsString));
                }
            }
        }
    }
}

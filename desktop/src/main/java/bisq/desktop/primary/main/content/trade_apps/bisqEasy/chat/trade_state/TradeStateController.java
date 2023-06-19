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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states.*;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Consumer;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final SettingsService settingsService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final DefaultApplicationService applicationService;
    private final TradePhaseBox tradePhaseBox;
    private Subscription isCollapsedPin;
    private Pin tradeRulesConfirmedPin, bisqEasyTradeStatePin;

    public TradeStateController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        this.applicationService = applicationService;
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        settingsService = applicationService.getSettingsService();
        bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();

        tradePhaseBox = new TradePhaseBox(applicationService);

        model = new TradeStateModel();
        view = new TradeStateView(model, this, tradePhaseBox.getView().getRoot());
    }

    public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        tradePhaseBox.setSelectedChannel(channel);

        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        UserIdentity myUserIdentity = channel.getMyUserIdentity();
        boolean maker = isMaker(bisqEasyOffer);
        NetworkId takerNetworkId = maker ?
                channel.getPeer().getNetworkId() :
                myUserIdentity.getUserProfile().getNetworkId();
        String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
        if (bisqEasyTradeService.findTrade(tradeId).isEmpty()) {
            log.error("###");
            return;
        }
        BisqEasyTrade bisqEasyTradeModel = bisqEasyTradeService.findTrade(tradeId).orElseThrow();
        model.setBisqEasyTradeModel(bisqEasyTradeModel);

        boolean isSeller = bisqEasyTradeModel.isSeller();
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
        }
        bisqEasyTradeStatePin = bisqEasyTradeModel.tradeStateObservable().addObserver(state -> {
            UIThread.run(() -> {
                switch (state) {
                    case INIT:
                        break;
                    case TAKER_SEND_TAKE_OFFER_REQUEST:
                    case MAKER_RECEIVED_TAKE_OFFER_REQUEST:
                        if (isSeller) {
                            model.getStateInfoVBox().set(new SellerState1(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        } else {
                            model.getStateInfoVBox().set(new BuyerState1(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        }
                        break;
                    case SELLER_SENT_ACCOUNT_DATA:
                        model.getStateInfoVBox().set(new SellerState2(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case BUYER_RECEIVED_ACCOUNT_DATA:
                        model.getStateInfoVBox().set(new BuyerState2(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case BUYER_SENT_FIAT_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new BuyerState3(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case SELLER_RECEIVED_FIAT_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new SellerState3(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case SELLER_SENT_BTC_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new SellerState4(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case BUYER_RECEIVED_BTC_SENT_CONFIRMATION:
                        model.getStateInfoVBox().set(new BuyerState4(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case BTC_CONFIRMED:
                        model.getStateInfoVBox().set(new SellerState5(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        model.getStateInfoVBox().set(new BuyerState5(applicationService, bisqEasyOffer, takerNetworkId, channel).getView().getRoot());
                        break;
                    case COMPLETED:
                        //todo
                        break;
                    default:
                        log.error(state.name());
                }
            });
        });

        String directionString = isSeller ?
                Res.get("offer.selling").toUpperCase() :
                Res.get("offer.buying").toUpperCase();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
        String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
        model.getHeadline().set(Res.get("bisqEasy.tradeState.header.headline",
                directionString,
                baseAmountString,
                quoteAmountString,
                paymentMethodName));

       /* if (model.getAppliedPhaseIndex() == phaseIndex) {
            return;
        }
        model.setAppliedPhaseIndex(phaseIndex);
        boolean tradeRulesConfirmed = getTradeRulesConfirmed();
        boolean showFirstTimeItems = phaseIndex == 0 && !tradeRulesConfirmed;
        applyFirstTimeItemsVisible();
        applyPhaseAndInfoBoxVisible();
        if (showFirstTimeItems) {
            return;
        }*/
    }

    @Override
    public void onActivate() {
        tradeRulesConfirmedPin = settingsService.getTradeRulesConfirmed().addObserver(tradeRulesConfirmed -> {
            /*int phaseIndex = model.getPhaseIndex().get();
            if (phaseIndex == 0 && tradeRulesConfirmed) {
                // phaseIndexChanged(phaseIndex);
            }*/
        });
        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> {
            applyFirstTimeItemsVisible();
            applyPhaseAndInfoBoxVisible();
        });
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
        applyPhaseAndInfoBoxVisible();
    }

    @Override
    public void onDeactivate() {
        if (tradeRulesConfirmedPin != null) {
            tradeRulesConfirmedPin.unbind();
        }
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
        }
        isCollapsedPin.unsubscribe();
    }

    void onExpand() {
        setIsCollapsed(false);
    }

    void onCollapse() {
        setIsCollapsed(true);
    }

    void onHeaderClicked() {
        setIsCollapsed(!model.getIsCollapsed().get());
    }

    private void setIsCollapsed(boolean value) {
        model.getIsCollapsed().set(value);
        settingsService.setCookie(CookieKey.TRADE_ASSISTANT_COLLAPSED, value);
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }


    private boolean isBuyer() {
        return isBuyer(model.getBisqEasyTradeModel().getOffer());
    }

    private boolean isBuyer(BisqEasyOffer bisqEasyOffer) {
        return isMaker(bisqEasyOffer) ?
                bisqEasyOffer.getMakersDirection().isBuy() :
                bisqEasyOffer.getTakersDirection().isBuy();
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    private void applyFirstTimeItemsVisible() {
        // model.getFirstTimeItemsVisible().set(!model.getIsCollapsed().get() && model.getPhaseIndex().get() == 0 && !getTradeRulesConfirmed());
    }

    private void applyPhaseAndInfoBoxVisible() {
        model.getPhaseAndInfoBoxVisible().set(!model.getIsCollapsed().get() && !model.getFirstTimeItemsVisible().get());
    }

    private Boolean getTradeRulesConfirmed() {
        return settingsService.getTradeRulesConfirmed().get();
    }
}

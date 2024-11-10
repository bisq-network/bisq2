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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_details;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TradeDetailsController extends NavigationController
        implements InitWithDataController<TradeDetailsController.InitData> {
    protected final UserIdentityService userIdentityService;
    @Getter
    private final TradeDetailsView view;

    @Getter
    private final TradeDetailsModel model;

    public TradeDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.BISQ_EASY_TRADE_DETAILS);

        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        model = new TradeDetailsModel();
        view = new TradeDetailsView(model, this);
    }

    private static String getPriceSpec(BisqEasyContract contract) {
        PriceSpec priceSpec = contract.getAgreedPriceSpec();
        String priceSpecStr = "";
        switch (priceSpec) {
            case MarketPriceSpec marketPriceSpec ->
                    priceSpecStr = Res.get("bisqEasy.openTrades.tradeDetails.marketPrice");
            case FloatPriceSpec floatPriceSpec -> {
                String absPercent = PercentageFormatter.formatToPercentWithSymbol(Math.abs(floatPriceSpec.getPercentage()));
                priceSpecStr = Res.get(floatPriceSpec.getPercentage() >= 0
                        ? "bisqEasy.openTrades.tradeDetails.aboveMarketPrice"
                        : "bisqEasy.openTrades.tradeDetails.belowMarketPrice", absPercent);
            }
            case FixPriceSpec fixPriceSpec -> priceSpecStr = Res.get("bisqEasy.openTrades.tradeDetails.fixedPrice");
            case null, default -> {
                // this should not happen unless a new PriceSpec is added
            }
        }
        return priceSpecStr;
    }

    private static String formatNetworkAddresses(AddressByTransportTypeMap addressMap) {
        return Joiner.on(", ").join(addressMap.entrySet().stream()
                .map(e -> e.getValue().getFullAddress())
                .collect(Collectors.toList()));
    }

    @Override
    public void initWithData(InitData initData) {
        BisqEasyTrade trade = initData.bisqEasyTrade;
        BisqEasyOpenTradeChannel channel = initData.channel;
        model.getTradeId().set(trade.getId());
        model.getPeerUsername().set(channel.getPeer().getUserName());

        String bitcoinPaymentAddress = trade.getBitcoinPaymentData().get();
        if (StringUtils.isNotEmpty(bitcoinPaymentAddress)) {
            model.getBitcoinPaymentAddress().set(bitcoinPaymentAddress);
        } else {
            model.getBitcoinPaymentAddress().set("");
        }

        BisqEasyContract contract = trade.getContract();
        long date = contract.getTakeOfferDate();
        model.getOfferTakenDateTime().set(DateFormatter.formatDateTime(date));

        long quoteSideAmount = contract.getQuoteSideAmount();
        Monetary quoteAmount = Fiat.from(quoteSideAmount, trade.getOffer().getMarket().getQuoteCurrencyCode());

        NetworkId peerNetworkId = trade.getPeer().getNetworkId();
        String peerAddress = formatNetworkAddresses(peerNetworkId.getAddressByTransportTypeMap());
        model.getPeerNetworkAddress().set(peerAddress);

        String amountInFiat = AmountFormatter.formatAmount(quoteAmount);
        model.getAmountInFiat().set(amountInFiat);
        String currencyAbbreviation = quoteAmount.getCode();
        model.getCurrency().set(currencyAbbreviation);

        long baseSideAmount = contract.getBaseSideAmount();
        Coin amountInBTC = Coin.asBtcFromValue(baseSideAmount);
        String baseAmountString = AmountFormatter.formatAmount(amountInBTC, false);
        model.getAmountInBTC().set(baseAmountString);

        String btcPaymentMethod = contract.getBaseSidePaymentMethodSpec().getDisplayString();
        model.getBitcoinPaymentMethod().set(btcPaymentMethod);
        String fiatPaymentMethod = contract.getQuoteSidePaymentMethodSpec().getDisplayString();
        model.getFiatPaymentMethod().set(fiatPaymentMethod);

        String paymentAccountData = trade.getPaymentAccountData().get();
        if (StringUtils.isNotEmpty(paymentAccountData)) {
            model.getPaymentAccountData().set(paymentAccountData);
        } else {
            model.getPaymentAccountData().set("");
        }

        String myMakerTakerRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
        model.getMyMakerTakerRole().set(myMakerTakerRole);
        Direction direction = BisqEasyTradeFormatter.getDirectionObject(trade);
        String buyerSellerRole = (direction == Direction.BUY) ? Res.get("bisqEasy.openTrades.tradeDetails.buyBtc") : Res.get("bisqEasy.openTrades.tradeDetails.sellBtc");
        model.getMySellBuyRole().set(buyerSellerRole);

        Optional<UserProfile> mediator = channel.getMediator();
        if (mediator.isPresent()) {
            model.getMediator().set(mediator.get().getUserName());
        } else {
            model.getMediator().set("");
        }

        PriceQuote priceQuote = BisqEasyTradeUtils.getPriceQuote(trade);
        String codes = priceQuote.getMarket().getMarketCodes();
        model.getPriceSpec().set(codes + " @ " + getPriceSpec(contract));
        String price = PriceFormatter.format(BisqEasyTradeUtils.getPriceQuote(trade));
        model.getTradePrice().set(price);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    void onClose() {
        OverlayController.hide();
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final BisqEasyTrade bisqEasyTrade;
        private final BisqEasyOpenTradeChannel channel;

        public InitData(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            this.bisqEasyTrade = bisqEasyTrade;
            this.channel = channel;
        }
    }
}
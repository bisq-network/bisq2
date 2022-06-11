package bisq.social.offer;


import bisq.common.currency.Market;
import bisq.common.monetary.Coin;
import bisq.common.proto.Proto;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode
@Slf4j
@Getter
public class TradeChatOffer implements Proto {
    private final long baseSideAmount;
    private final Market market;
    private final Set<String> paymentMethods;
    private final String makersTradeTerms;
    private final long requiredTotalReputationScore;
    @Nullable
    private transient final String chatMessageText;

    public TradeChatOffer(long baseSideAmount, 
                          Market market, 
                          Set<String> paymentMethods, 
                          String makersTradeTerms, 
                          long requiredTotalReputationScore) {
        this.baseSideAmount = baseSideAmount;
        this.market = market;
        this.paymentMethods = paymentMethods;
        this.makersTradeTerms = makersTradeTerms;
        this.requiredTotalReputationScore = requiredTotalReputationScore;

        chatMessageText = Res.get("createOffer.tradeChatOffer.chatMessage",
                AmountFormatter.formatAmountWithCode(Coin.of(baseSideAmount, market.baseCurrencyCode()), true),
                market.quoteCurrencyCode(),
                Joiner.on(", ").join(this.paymentMethods));
    }

    @Override
    public bisq.social.protobuf.TradeChatOffer toProto() {
        return bisq.social.protobuf.TradeChatOffer.newBuilder()
                .setBaseSideAmount(baseSideAmount)
                .setMarket(market.toProto())
                .addAllPaymentMethods(new ArrayList<>(paymentMethods))
                .setMakersTradeTerms(makersTradeTerms)
                .setRequiredTotalReputationScore(requiredTotalReputationScore)
                .build();
    }

    public static TradeChatOffer fromProto(bisq.social.protobuf.TradeChatOffer proto) {
        return new TradeChatOffer(proto.getBaseSideAmount(),
                Market.fromProto(proto.getMarket()),
                new HashSet<>(proto.getPaymentMethodsList()),
                proto.getMakersTradeTerms(),
                proto.getRequiredTotalReputationScore());
    }
}

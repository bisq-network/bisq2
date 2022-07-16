package bisq.chat.trade.pub;


import bisq.common.currency.Market;
import bisq.common.monetary.Fiat;
import bisq.common.proto.Proto;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.presentation.formatters.AmountFormatter;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public final class TradeChatOffer implements Proto {
    private final Direction direction;
    private final long baseSideAmount;
    private final Market market;
    private final long quoteSideAmount;
    private final Set<String> paymentMethods;
    private final String makersTradeTerms;
    private final long requiredTotalReputationScore;

    private transient final String chatMessageText;

    public TradeChatOffer(Direction direction,
                          Market market,
                          long baseSideAmount,
                          long quoteSideAmount,
                          Set<String> paymentMethods,
                          String makersTradeTerms,
                          long requiredTotalReputationScore) {
        this.direction = direction;
        this.market = market;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.paymentMethods = paymentMethods;
        this.makersTradeTerms = makersTradeTerms;
        this.requiredTotalReputationScore = requiredTotalReputationScore;

        chatMessageText = Res.get("createOffer.tradeChatOffer.chatMessage",
                Res.get(direction.name().toLowerCase()).toUpperCase(),
                AmountFormatter.formatAmountWithCode(Fiat.of(quoteSideAmount, market.getQuoteCurrencyCode()), true),
                Joiner.on(", ").join(this.paymentMethods));
    }

    @Override
    public bisq.chat.protobuf.TradeChatOffer toProto() {
        return bisq.chat.protobuf.TradeChatOffer.newBuilder()
                .setDirection(direction.toProto())
                .setMarket(market.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .addAllPaymentMethods(new ArrayList<>(paymentMethods))
                .setMakersTradeTerms(makersTradeTerms)
                .setRequiredTotalReputationScore(requiredTotalReputationScore)
                .build();
    }

    public static TradeChatOffer fromProto(bisq.chat.protobuf.TradeChatOffer proto) {
        return new TradeChatOffer(Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                proto.getBaseSideAmount(),
                proto.getQuoteSideAmount(),
                new HashSet<>(proto.getPaymentMethodsList()),
                proto.getMakersTradeTerms(),
                proto.getRequiredTotalReputationScore());
    }
}

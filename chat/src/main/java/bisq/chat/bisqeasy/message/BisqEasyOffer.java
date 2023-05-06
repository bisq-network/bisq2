package bisq.chat.bisqeasy.message;


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
import java.util.Comparator;
import java.util.List;

@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public final class BisqEasyOffer implements Proto {
    private final String id;
    private final Direction direction;
    private final long baseSideAmount;
    private final Market market;
    private final long quoteSideAmount;
    private final List<String> paymentMethods;
    private final String makersTradeTerms;
    private final long requiredTotalReputationScore;

    private transient final String chatMessageText;

    public BisqEasyOffer(String id,
                         Direction direction,
                         Market market,
                         long baseSideAmount,
                         long quoteSideAmount,
                         List<String> paymentMethods,
                         String makersTradeTerms,
                         long requiredTotalReputationScore) {
        this.id = id;
        this.direction = direction;
        this.market = market;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.paymentMethods = paymentMethods;
        this.makersTradeTerms = makersTradeTerms;
        this.requiredTotalReputationScore = requiredTotalReputationScore;

        // We need to sort deterministically as the data is used in the proof of work check
        this.paymentMethods.sort(Comparator.comparing((String e) -> e));
        chatMessageText = Res.get("createOffer.bisqEasyOffer.chatMessage",
                Res.get(direction.name().toLowerCase()).toUpperCase(),
                AmountFormatter.formatAmountWithCode(Fiat.of(quoteSideAmount, market.getQuoteCurrencyCode()), true),
                Joiner.on(", ").join(this.paymentMethods));
    }

    @Override
    public bisq.chat.protobuf.BisqEasyOffer toProto() {
        return bisq.chat.protobuf.BisqEasyOffer.newBuilder()
                .setId(id)
                .setDirection(direction.toProto())
                .setMarket(market.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .addAllPaymentMethods(paymentMethods)
                .setMakersTradeTerms(makersTradeTerms)
                .setRequiredTotalReputationScore(requiredTotalReputationScore)
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.chat.protobuf.BisqEasyOffer proto) {
        return new BisqEasyOffer(proto.getId(),
                Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                proto.getBaseSideAmount(),
                proto.getQuoteSideAmount(),
                new ArrayList<>(proto.getPaymentMethodsList()),
                proto.getMakersTradeTerms(),
                proto.getRequiredTotalReputationScore());
    }
}

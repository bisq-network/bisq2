package bisq.social.offer;


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
    private final long btcAmount;
    private final String quoteCurrencyCode;
    private final Set<String> paymentMethods;
    private final String makersTradeTerms;
    @Nullable
    private transient final String chatMessageText;

    public TradeChatOffer(long btcAmount, String quoteCurrencyCode, Set<String> paymentMethods, String makersTradeTerms) {
        this.btcAmount = btcAmount;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.paymentMethods = paymentMethods;
        this.makersTradeTerms = makersTradeTerms;

        chatMessageText = Res.get("satoshisquareapp.createOffer.offerPreview",
                AmountFormatter.formatAmountWithCode(Coin.of(btcAmount, "BTC"), true),
                quoteCurrencyCode,
                Joiner.on(", ").join(this.paymentMethods));
    }

    @Override
    public bisq.social.protobuf.TradeChatOffer toProto() {
        return bisq.social.protobuf.TradeChatOffer.newBuilder()
                .setBtcAmount(btcAmount)
                .setQuoteCurrencyCode(quoteCurrencyCode)
                .addAllPaymentMethods(new ArrayList<>(paymentMethods))
                .setMakersTradeTerms(makersTradeTerms)
                .build();
    }

    public static TradeChatOffer fromProto(bisq.social.protobuf.TradeChatOffer proto) {
        return new TradeChatOffer(proto.getBtcAmount(),
                proto.getQuoteCurrencyCode(),
                new HashSet<>(proto.getPaymentMethodsList()),
                proto.getMakersTradeTerms());
    }
}

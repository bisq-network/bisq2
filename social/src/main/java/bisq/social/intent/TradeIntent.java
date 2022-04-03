package bisq.social.intent;


import bisq.common.monetary.Coin;
import bisq.common.proto.Proto;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class TradeIntent implements Proto {
    private final long btcAmount;
    private final String quoteCurrencyCode;
    private final Set<String> paymentMethods;
    private transient String chatMessageText = null;

    public TradeIntent(long btcAmount, String quoteCurrencyCode, Set<String> paymentMethods) {
        this.btcAmount = btcAmount;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.paymentMethods = paymentMethods;
    }

    @Override
    public bisq.social.protobuf.TradeIntent toProto() {
        return bisq.social.protobuf.TradeIntent.newBuilder()
                .setBtcAmount(btcAmount)
                .setQuoteCurrencyCode(quoteCurrencyCode)
                .addAllPaymentMethods(new ArrayList<>(paymentMethods))
                .build();
    }

    public static TradeIntent fromProto(bisq.social.protobuf.TradeIntent proto) {
        return new TradeIntent(proto.getBtcAmount(),
                proto.getQuoteCurrencyCode(),
                new HashSet<>(proto.getPaymentMethodsList()));
    }

    public String getChatMessageText() {
        if (chatMessageText == null) {
            chatMessageText = Res.get("satoshisquareapp.createOffer.offerPreview",
                    AmountFormatter.formatAmountWithCode(Coin.of(btcAmount, "BTC"), true),
                    quoteCurrencyCode,
                    Joiner.on(", ").join(this.paymentMethods));
        }
        return chatMessageText;
    }
}

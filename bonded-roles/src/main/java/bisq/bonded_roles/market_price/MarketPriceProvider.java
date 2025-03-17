package bisq.bonded_roles.market_price;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;
import com.google.common.base.Enums;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@ToString
public enum MarketPriceProvider implements ProtoEnum {
    BISQAGGREGATE(Res.get("component.marketPrice.provider.BISQAGGREGATE")),
    COINGECKO("CoinGecko"),
    POLO("Poloniex"),
    BITFINEX("Bitfinex"),
    OTHER;

    public static MarketPriceProvider fromName(String name) {
        return Enums.getIfPresent(MarketPriceProvider.class, name).or(OTHER);
    }

    @Getter
    private final transient Optional<String> displayName;

    MarketPriceProvider() {
        this.displayName = Optional.empty();
    }

    MarketPriceProvider(String displayName) {
        this.displayName = Optional.of(displayName);
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceProvider toProtoEnum() {
        return bisq.bonded_roles.protobuf.MarketPriceProvider.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MarketPriceProvider fromProto(bisq.bonded_roles.protobuf.MarketPriceProvider proto) {
        return ProtobufUtils.enumFromProto(MarketPriceProvider.class, proto.name(), OTHER);
    }
}

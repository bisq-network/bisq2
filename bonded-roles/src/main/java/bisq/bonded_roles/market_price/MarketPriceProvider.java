package bisq.bonded_roles.market_price;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;
import com.google.common.base.Enums;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@ToString
public enum MarketPriceProvider implements ProtoEnum {
    BISQAGGREGATE(Res.get("component.marketPrice.provider.BISQAGGREGATE")),
    COINGECKO("CoinGecko"),
    POLO("Poloniex"),
    BITFINEX("Bitfinex"),
    OTHER;

    public static MarketPriceProvider fromName(String name) {
        return Enums.getIfPresent(MarketPriceProvider.class, name).or(otherWithName(name));
    }

    private static MarketPriceProvider otherWithName(String name) {
        MarketPriceProvider other = OTHER;
        other.setDisplayName(name);
        return other;
    }


    @Nullable
    @Getter
    @Setter
    private String displayName;

    MarketPriceProvider() {
    }

    MarketPriceProvider(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public bisq.bonded_roles.protobuf.MarketPriceProvider toProtoEnum() {
        return bisq.bonded_roles.protobuf.MarketPriceProvider.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MarketPriceProvider fromProto(bisq.bonded_roles.protobuf.MarketPriceProvider proto) {
        return ProtobufUtils.enumFromProto(MarketPriceProvider.class, proto.name(), OTHER);
    }
}

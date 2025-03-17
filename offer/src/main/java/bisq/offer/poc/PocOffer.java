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

package bisq.offer.poc;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.Direction;
import bisq.offer.options.OfferOption;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_2_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class PocOffer implements DistributedData {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_2_DAYS, getClass().getSimpleName());

    public static final String ACCOUNT_AGE_WITNESS_HASH = "accountAgeWitnessHash";
    public static final String REFERRAL_ID = "referralId";
    // Only used in payment method F2F
    public static final String F2F_CITY = "f2fCity";
    public static final String F2F_EXTRA_INFO = "f2fExtraInfo";
    public static final String CASH_BY_MAIL_EXTRA_INFO = "cashByMailExtraInfo";

    // Comma separated list of ordinal of a bisq.common.app.Capability. E.g. ordinal of
    // Capability.SIGNED_ACCOUNT_AGE_WITNESS is 11 and Capability.MEDIATION is 12 so if we want to signal that maker
    // of the offer supports both capabilities we add "11, 12" to capabilities.
    public static final String CAPABILITIES = "capabilities";
    // If maker is seller and has xmrAutoConf enabled it is set to "1" otherwise it is not set
    public static final String XMR_AUTO_CONF = "xmrAutoConf";
    public static final String XMR_AUTO_CONF_ENABLED_VALUE = "1";

    private final String id;
    private final long date;
    private final NetworkId makerNetworkId;
    private final Market market;
    private final Direction direction;
    private final long baseAmount;
    private final PriceSpec priceSpec;
    private final List<TradeProtocolType> protocolTypes;
    private final List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs;
    private final List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs;
    private final List<OfferOption> offerOptions;

    public PocOffer(String id,
                    long date,
                    NetworkId makerNetworkId,
                    Market market,
                    Direction direction,
                    long baseAmount,
                    PriceSpec priceSpec,
                    List<TradeProtocolType> protocolTypes,
                    List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs,
                    List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs,
                    List<OfferOption> offerOptions) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.market = market;
        this.direction = direction;
        this.baseAmount = baseAmount;
        this.priceSpec = priceSpec;
        this.protocolTypes = protocolTypes;
        this.baseSidePaymentMethodSpecs = baseSidePaymentMethodSpecs;
        this.quoteSidePaymentMethodSpecs = quoteSidePaymentMethodSpecs;
        this.offerOptions = offerOptions;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }

    public Optional<TradeProtocolType> findProtocolType() {
        if (protocolTypes.isEmpty()) {
            return Optional.empty();
        } else if (protocolTypes.size() == 1) {
            return Optional.of(protocolTypes.get(0));
        } else {
            throw new IllegalStateException("Multiple protocolTypes are not supported yet. protocolTypes=" + protocolTypes);
        }
    }

    public Monetary getBaseAmountAsMonetary() {
        return Monetary.from(baseAmount, market.getBaseCurrencyCode());
    }

    public Monetary getQuoteAmountAsMonetary(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPriceSpec) {
            Monetary base = getBaseAmountAsMonetary();
            PriceQuote priceQuote = ((FixPriceSpec) priceSpec).getPriceQuote();
            long quoteAmountValue = priceQuote.toQuoteSideMonetary(base).getValue();
            return Monetary.from(quoteAmountValue, market.getQuoteCurrencyCode());
        } else if (priceSpec instanceof FloatPriceSpec) {
            Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public PriceQuote getQuote(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPriceSpec) {
            return ((FixPriceSpec) priceSpec).getPriceQuote();
        } else if (priceSpec instanceof FloatPriceSpec) {
            Optional<MarketPrice> marketPrice = marketPriceService.findMarketPrice(market);
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    @Override
    public Message toProto(boolean serializeForHash) {
        return null;
    }
}

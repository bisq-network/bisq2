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

package bisq.offer;

import bisq.account.protocol.SwapProtocolType;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.options.ListingOption;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class Offer implements NetworkPayload {
    private final String id;
    private final long date;
    private final NetworkId makerNetworkId;
    private final PriceSpec priceSpec;
    private final List<SwapProtocolType> protocolTypes;
    private final List<SettlementSpec> baseSideSettlementSpecs;
    private final List<SettlementSpec> quoteSideSettlementSpecs;
    private final List<ListingOption> listingOptions;
    private final Market market;
    private final Direction direction;
    private final MetaData metaData;
    private final long baseAmount;


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

    public Offer(String id,
                 long date,
                 NetworkId makerNetworkId,
                 Market market,
                 Direction direction,
                 long baseAmount,
                 PriceSpec priceSpec,
                 List<SwapProtocolType> protocolTypes,
                 List<SettlementSpec> baseSideSettlementSpecs,
                 List<SettlementSpec> quoteSideSettlementSpecs,
                 List<ListingOption> listingOptions) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.market = market;
        this.direction = direction;
        this.baseAmount = baseAmount;
        this.priceSpec = priceSpec;
        this.protocolTypes = protocolTypes;
        this.baseSideSettlementSpecs = baseSideSettlementSpecs;
        this.quoteSideSettlementSpecs = quoteSideSettlementSpecs;
        this.listingOptions = listingOptions;

      /*  if (priceOption instanceof FixPriceOption fixPriceOption) {
            Monetary base = Monetary.from(baseAmount, market.baseCurrencyCode());
            Quote quote = Quote.fromPrice(fixPriceOption.value(), market);
            quoteAmount = Quote.toQuoteMonetary(base,quote).getValue();
        }*/
        metaData = new MetaData(TimeUnit.MINUTES.toMillis(5), 100000, getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    public Optional<SwapProtocolType> findProtocolType() {
        if (protocolTypes.isEmpty()) {
            return Optional.empty();
        } else if (protocolTypes.size() == 1) {
            return Optional.of(protocolTypes.get(0));
        } else {
            throw new IllegalStateException("Multiple protocolTypes are not supported yet. protocolTypes=" + protocolTypes);
        }
    }

    public Monetary getBaseAmountAsMonetary() {
        return Monetary.from(baseAmount, market.baseCurrencyCode());
    }

    public Monetary getQuoteAmountAsMonetary(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPrice fixPriceSpec) {
            Monetary base = getBaseAmountAsMonetary();
            Quote quote = Quote.fromPrice(fixPriceSpec.value(), market);
            long quoteAmountValue = Quote.toQuoteMonetary(base, quote).getValue();
            return Monetary.from(quoteAmountValue, market.quoteCurrencyCode());
        } else if (priceSpec instanceof FloatPrice floatPrice) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public Quote getQuote(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPrice fixPriceSpec) {
            return Quote.fromPrice(fixPriceSpec.value(), market);
        } else if (priceSpec instanceof FloatPrice floatPrice) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }
}

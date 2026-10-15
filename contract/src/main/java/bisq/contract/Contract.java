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

package bisq.contract;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Defines the terms of the financial interaction with the counterparty/parties.
 */
@ToString
@Getter
@EqualsAndHashCode
public abstract class Contract<T extends Offer<?, ?>> implements NetworkProto {
    protected final long takeOfferDate;
    protected final T offer;
    protected final TradeProtocolType protocolType;

    protected transient final Party maker;

    public Contract(long takeOfferDate, T offer, TradeProtocolType protocolType, Party maker) {
        this.takeOfferDate = takeOfferDate;
        this.offer = offer;
        this.protocolType = protocolType;
        this.maker = maker;
    }

    // The final subclass calls verify() in its constructor after it set the amounts (see rule 1 of
    // docs/specifications/network/validation-at-parsing.md)
    @Override
    public void verify() {
        NetworkDataValidation.validateDate(takeOfferDate);

        // A contract is an agreement about an amount on each side, and its price is derived from the two amounts
        // (getPriceQuote()). Without a positive amount on each side, or with amounts whose price does not fit into a
        // long, the contract has no price, and getPriceQuote() throws for every component which uses the contract.
        checkArgument(getBaseSideAmount() > 0, "The base side amount of a contract must be positive");
        checkArgument(getQuoteSideAmount() > 0, "The quote side amount of a contract must be positive");
        try {
            getPriceQuote();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("The price of the amounts of a contract must be computable", e);
        }
    }

    @Override
    public abstract bisq.contract.protobuf.Contract toProto(boolean serializeForHash);

    protected bisq.contract.protobuf.Contract.Builder getContractBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.Contract.newBuilder()
                .setTakeOfferDate(takeOfferDate)
                .setOffer(offer.toProto(serializeForHash))
                .setTradeProtocolType(protocolType.toProtoEnum());
    }

    public static Contract<?> fromProto(bisq.contract.protobuf.Contract proto) throws UnresolvableProtobufException {
        return switch (proto.getMessageCase()) {
            case TWOPARTYCONTRACT -> TwoPartyContract.fromProto(proto);
            case MULTIPARTYCONTRACT -> MultiPartyContract.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    public abstract Party getTaker();

    public abstract long getBaseSideAmount();

    public abstract long getQuoteSideAmount();

    public Monetary getBaseSideMonetary() {
        return Monetary.from(getBaseSideAmount(), offer.getMarket().getBaseCurrencyCode());
    }

    public Monetary getQuoteSideMonetary() {
        return Monetary.from(getQuoteSideAmount(), offer.getMarket().getQuoteCurrencyCode());
    }

    public PriceQuote getPriceQuote() {
        return PriceQuote.from(getBaseSideMonetary(), getQuoteSideMonetary());
    }
}

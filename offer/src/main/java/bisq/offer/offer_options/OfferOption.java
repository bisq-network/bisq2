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

package bisq.offer.offer_options;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OfferOption extends Proto {
    static List<OfferOption> fromTradeTermsAndReputationScore(String makersTradeTerms, long requiredTotalReputationScore) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        if (requiredTotalReputationScore > 0) {
            offerOptions.add(new ReputationOption(requiredTotalReputationScore));
        }
        return offerOptions;
    }

    bisq.offer.protobuf.OfferOption toProto();

    default bisq.offer.protobuf.OfferOption.Builder getOfferOptionBuilder() {
        return bisq.offer.protobuf.OfferOption.newBuilder();
    }

    static Optional<TradeTermsOption> findTradeTermsOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof TradeTermsOption)
                .map(option -> (TradeTermsOption) option)
                .findAny();
    }

    static Optional<ReputationOption> findReputationOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof ReputationOption)
                .map(option -> (ReputationOption) option)
                .findAny();
    }

    static Optional<CollateralOption> findCollateralOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof CollateralOption)
                .map(option -> (CollateralOption) option)
                .findAny();
    }

    static Optional<FiatSettlementOption> findFiatSettlementOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FiatSettlementOption)
                .map(option -> (FiatSettlementOption) option)
                .findAny();
    }

    static Optional<FeeOption> findFeeOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FeeOption)
                .map(option -> (FeeOption) option)
                .findAny();
    }

    static OfferOption fromProto(bisq.offer.protobuf.OfferOption proto) {
        switch (proto.getMessageCase()) {
            case TRADETERMSOPTION: {
                return TradeTermsOption.fromProto(proto.getTradeTermsOption());
            }
            case REPUTATIONOPTION: {
                return ReputationOption.fromProto(proto.getReputationOption());
            }
            case COLLATERALOPTION: {
                return CollateralOption.fromProto(proto.getCollateralOption());
            }
            case FIATSETTLEMENTOPTION: {
                return FiatSettlementOption.fromProto(proto.getFiatSettlementOption());
            }
            case FEEOPTION: {
                return FeeOption.fromProto(proto.getFeeOption());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}

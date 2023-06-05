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

package bisq.protocol.poc.bisq_easy;

import bisq.common.proto.Proto;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class BisqEasyTrade implements Proto {
    private final BisqEasyOffer offer;
    private final long baseSideAmount;
    private final long quoteSideAmount;
    private final double premium;

    public BisqEasyTrade(BisqEasyOffer offer, long baseSideAmount, long quoteSideAmount, double premium) {
        this.offer = offer;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.premium = premium;
    }

    @Override
    public bisq.protocol.protobuf.BisqEasyTrade toProto() {
        return bisq.protocol.protobuf.BisqEasyTrade.newBuilder()
                .setOffer(offer.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .setPremium(premium)
                .build();
    }

    public static BisqEasyTrade fromProto(bisq.protocol.protobuf.BisqEasyTrade proto) {
        return new BisqEasyTrade(BisqEasyOffer.fromProto(proto.getOffer()),
                proto.getBaseSideAmount(),
                proto.getQuoteSideAmount(),
                proto.getPremium());
    }

    public String getId() {
        return offer.getId();
    }
}
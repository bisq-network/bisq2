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

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public final class SettlementSpec implements Proto {
    private final String settlementMethodName;
    private final Optional<String> saltedMakerAccountId;

    public SettlementSpec(String settlementMethodName) {
        this(settlementMethodName, Optional.empty());
    }

    /**
     * @param settlementMethodName Name of SettlementMethod enum
     * @param saltedMakerAccountId Salted local ID of maker's payment account.
     *                             In case maker had multiple payment accounts for same settlement method they
     *                             can define which account to use for that offer.
     *                             We combine the local ID with an offer specific salt, to not leak identity of multiple
     *                             offers using different identities but the same payment account.
     */
    public SettlementSpec(String settlementMethodName, Optional<String> saltedMakerAccountId) {
        this.settlementMethodName = settlementMethodName;
        this.saltedMakerAccountId = saltedMakerAccountId;
    }

    public bisq.offer.protobuf.SettlementSpec toProto() {
        bisq.offer.protobuf.SettlementSpec.Builder builder = bisq.offer.protobuf.SettlementSpec.newBuilder()
                .setSettlementMethodName(settlementMethodName);
        saltedMakerAccountId.ifPresent(builder::setSaltedMakerAccountId);
        return builder.build();
    }

    public static SettlementSpec fromProto(bisq.offer.protobuf.SettlementSpec proto) {
        return new SettlementSpec(proto.getSettlementMethodName(),
                proto.hasSaltedMakerAccountId() ? Optional.of(proto.getSaltedMakerAccountId()) : Optional.empty());
    }
}
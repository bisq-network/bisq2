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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class AccountAgeWitnessDateRequest implements NetworkProto {
    private final String hashAsHex;

    public AccountAgeWitnessDateRequest(String hashAsHex) {
        this.hashAsHex = hashAsHex;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHashAsHex(hashAsHex);
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessDateRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.AccountAgeWitnessDateRequest.newBuilder()
                .setHashAsHex(hashAsHex);
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessDateRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessDateRequest completeProto() {
        return toProto(false);
    }

    public static AccountAgeWitnessDateRequest fromProto(bisq.bridge.protobuf.AccountAgeWitnessDateRequest proto) {
        return new AccountAgeWitnessDateRequest(proto.getHashAsHex());
    }
}

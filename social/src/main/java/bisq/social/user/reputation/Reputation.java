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

package bisq.social.user.reputation;

import bisq.common.proto.Proto;
import bisq.common.util.ProtobufUtils;
import bisq.social.user.proof.Proof;

public record Reputation(Type type, Proof proof) implements Proto {
    public enum Type {
        BURNED_BSQ,
        BURNED_BSQ_AGE,
        IDENTITY_AGE,
        BISQ1_ACCOUNT_AGE,
        BISQ1_SIGNED_ACCOUNT_AGE_WITNESS
    }
    
    @Override
    public bisq.social.protobuf.Reputation toProto() {
        return bisq.social.protobuf.Reputation.newBuilder()
                .setType(type.name())
                .setProof(proof.toProto())
                .build();
    }

    public static Reputation fromProto(bisq.social.protobuf.Reputation proto) {
        return new Reputation(ProtobufUtils.enumFromProto(Type.class, proto.getType()),
                Proof.fromProto(proto.getProof()));
    }
}
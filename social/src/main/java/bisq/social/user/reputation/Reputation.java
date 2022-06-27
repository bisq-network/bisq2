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
import bisq.i18n.Res;
import bisq.social.user.proof.Proof;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Reputation implements Proto {
    public enum Source {
        BURNED_BSQ(Res.get("reputation.source.BURNED_BSQ")),
        BSQ_BOND(Res.get("reputation.source.BSQ_BOND")),
        PROFILE_AGE(Res.get("reputation.source.PROFILE_AGE")),
        BISQ1_ACCOUNT_AGE(Res.get("reputation.source.BISQ1_ACCOUNT_AGE")),
        BISQ1_SIGNED_ACCOUNT_AGE_WITNESS(Res.get("reputation.source.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS"));

        @Getter
        private final String displayString;

        Source(String displayString) {
            this.displayString = displayString;
        }
    }

    private final Source source;
    private final Proof proof;

    public Reputation(Source source, Proof proof) {
        this.source = source;
        this.proof = proof;
    }

    @Override
    public bisq.social.protobuf.Reputation toProto() {
        return bisq.social.protobuf.Reputation.newBuilder()
                .setType(source.name())
                .setProof(proof.toProto())
                .build();
    }

    public static Reputation fromProto(bisq.social.protobuf.Reputation proto) {
        return new Reputation(ProtobufUtils.enumFromProto(Source.class, proto.getType()),
                Proof.fromProto(proto.getProof()));
    }
}
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

package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class RevolutAccountPayload extends AccountPayload {
    private final String email;

    public RevolutAccountPayload(String id, String settlementMethodName, String email) {
        super(id, settlementMethodName);
        this.email = email;
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto() {
        return getAccountPayloadBuilder().setRevolutAccountPayload(
                        bisq.account.protobuf.RevolutAccountPayload.newBuilder()
                                .setEmail(email))
                .build();
    }

    public static RevolutAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return new RevolutAccountPayload(proto.getId(), proto.getSettlementMethodName(), proto.getRevolutAccountPayload().getEmail());
    }
}
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

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UpholdAccountPayload extends AccountPayload {

    private final String accountId;
    private final String accountOwner;

    public UpholdAccountPayload(String id, String paymentMethodName, String accountId, String accountOwner) {
        super(id, paymentMethodName);
        this.accountId = accountId;
        this.accountOwner = accountOwner;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(accountId, 100);
        NetworkDataValidation.validateText(accountOwner, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setUpholdAccountPayload(toUpholdAccountPayloadProto(serializeForHash));
    }

    public static UpholdAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var upholdPayload = proto.getUpholdAccountPayload();
        return new UpholdAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                upholdPayload.getAccountId(),
                upholdPayload.getAccountOwner()
        );
    }

    private bisq.account.protobuf.UpholdAccountPayload toUpholdAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUpholdAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpholdAccountPayload.Builder getUpholdAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpholdAccountPayload.newBuilder()
                .setAccountId(accountId)
                .setAccountOwner(accountOwner);
    }
}
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
public final class PayseraAccountPayload extends AccountPayload {

    private final String email;

    public PayseraAccountPayload(String id, String paymentMethodName, String email) {
        super(id, paymentMethodName);
        this.email = email;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateEmail(email);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setPayseraAccountPayload(toPayseraAccountPayloadProto(serializeForHash));
    }

    public static PayseraAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payseraPayload = proto.getPayseraAccountPayload();
        return new PayseraAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                payseraPayload.getEmail()
        );
    }

    private bisq.account.protobuf.PayseraAccountPayload toPayseraAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPayseraAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayseraAccountPayload.Builder getPayseraAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayseraAccountPayload.newBuilder()
                .setEmail(email);
    }
}
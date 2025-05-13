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

import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class TransferwiseAccountPayload extends CountryBasedAccountPayload {

    private final String email;

    public TransferwiseAccountPayload(String id,
                                      String paymentMethodName,
                                      String countryCode,
                                      String email) {
        super(id, paymentMethodName, countryCode);
        this.email = email;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateEmail(email);
    }

    public static TransferwiseAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var transferwisePayload = countryBasedAccountPayload.getTransferwiseAccountPayload();

        return new TransferwiseAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                transferwisePayload.getEmail()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setTransferwiseAccountPayload(toTransferwiseAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.TransferwiseAccountPayload toTransferwiseAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getTransferwiseAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.TransferwiseAccountPayload.Builder getTransferwiseAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.TransferwiseAccountPayload.newBuilder()
                .setEmail(email);
    }
}
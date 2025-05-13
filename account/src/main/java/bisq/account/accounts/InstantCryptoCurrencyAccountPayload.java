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
public final class InstantCryptoCurrencyAccountPayload extends AccountPayload {

    private final String address;

    public InstantCryptoCurrencyAccountPayload(String id, String paymentMethodName, String address) {
        super(id, paymentMethodName);
        this.address = address;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(address, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setInstantCryptoCurrencyAccountPayload(toInstantCryptoCurrencyAccountPayloadProto(serializeForHash));
    }

    public static InstantCryptoCurrencyAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var instantCryptoPayload = proto.getInstantCryptoCurrencyAccountPayload();
        return new InstantCryptoCurrencyAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                instantCryptoPayload.getAddress()
        );
    }

    private bisq.account.protobuf.InstantCryptoCurrencyAccountPayload toInstantCryptoCurrencyAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getInstantCryptoCurrencyAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.InstantCryptoCurrencyAccountPayload.Builder getInstantCryptoCurrencyAccountPayloadBuilder(
            boolean serializeForHash) {
        return bisq.account.protobuf.InstantCryptoCurrencyAccountPayload.newBuilder()
                .setAddress(address);
    }
}
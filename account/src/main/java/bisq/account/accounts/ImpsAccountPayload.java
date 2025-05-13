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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class ImpsAccountPayload extends IfscBasedAccountPayload {

    public ImpsAccountPayload(String id,
                              String paymentMethodName,
                              String countryCode,
                              String holderName,
                              String accountNr,
                              String ifsc) {
        super(id, paymentMethodName, countryCode, holderName, accountNr, ifsc);
    }

    public static ImpsAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var ifscBasedPayload = countryBasedAccountPayload.getIfscBasedAccountPayload();

        return new ImpsAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                ifscBasedPayload.getHolderName(),
                ifscBasedPayload.getAccountNr(),
                ifscBasedPayload.getIfsc()
        );
    }

    @Override
    protected bisq.account.protobuf.IfscBasedAccountPayload.Builder getIfscBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getIfscBasedAccountPayloadBuilder(serializeForHash)
                .setImpsAccountPayload(toImpsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.ImpsAccountPayload toImpsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getImpsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ImpsAccountPayload.Builder getImpsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ImpsAccountPayload.newBuilder();
    }
}
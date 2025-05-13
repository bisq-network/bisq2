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
public final class RtgsAccountPayload extends IfscBasedAccountPayload {

    public RtgsAccountPayload(String id,
                              String paymentMethodName,
                              String countryCode,
                              String holderName,
                              String accountNr,
                              String ifsc) {
        super(id, paymentMethodName, countryCode, holderName, accountNr, ifsc);
    }

    public static RtgsAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var ifscBasedPayload = countryBasedAccountPayload.getIfscBasedAccountPayload();

        return new RtgsAccountPayload(
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
                .setRtgsAccountPayload(toRtgsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.RtgsAccountPayload toRtgsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getRtgsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.RtgsAccountPayload.Builder getRtgsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.RtgsAccountPayload.newBuilder();
    }
}
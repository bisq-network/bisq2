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
public final class AustraliaPayidAccountPayload extends AccountPayload {

    private final String bankAccountName;
    private final String payid;

    public AustraliaPayidAccountPayload(String id, String paymentMethodName, String bankAccountName, String payid) {
        super(id, paymentMethodName);
        this.bankAccountName = bankAccountName;
        this.payid = payid;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(bankAccountName, 100);
        NetworkDataValidation.validateText(payid, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setAustraliaPayidAccountPayload(toAustraliaPayidPayloadProto(serializeForHash));
    }

    public static AustraliaPayidAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var australiaPayidPayload = proto.getAustraliaPayidAccountPayload();
        return new AustraliaPayidAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                australiaPayidPayload.getBankAccountName(),
                australiaPayidPayload.getPayid()
        );
    }

    private bisq.account.protobuf.AustraliaPayidAccountPayload toAustraliaPayidPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getAustraliaPayidAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AustraliaPayidAccountPayload.Builder getAustraliaPayidAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AustraliaPayidAccountPayload.newBuilder()
                .setBankAccountName(bankAccountName)
                .setPayid(payid);
    }
}
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
public final class SbpAccountPayload extends AccountPayload {

    private final String holderName;
    private final String mobileNumber;
    private final String bankName;

    public SbpAccountPayload(String id,
                             String paymentMethodName,
                             String holderName,
                             String mobileNumber,
                             String bankName) {
        super(id, paymentMethodName);
        this.holderName = holderName;
        this.mobileNumber = mobileNumber;
        this.bankName = bankName;
        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validatePhoneNumber(mobileNumber, "+7");
        NetworkDataValidation.validateText(bankName, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setSbpAccountPayload(toSbpAccountPayloadProto(serializeForHash));
    }

    public static SbpAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var sbpPayload = proto.getSbpAccountPayload();
        return new SbpAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                sbpPayload.getHolderName(),
                sbpPayload.getMobileNumber(),
                sbpPayload.getBankName()
        );
    }

    private bisq.account.protobuf.SbpAccountPayload toSbpAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSbpAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SbpAccountPayload.Builder getSbpAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SbpAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setMobileNumber(mobileNumber)
                .setBankName(bankName);
    }
}
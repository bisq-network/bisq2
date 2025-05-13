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
public final class JapanBankAccountPayload extends AccountPayload {

    private final String bankName;
    private final String bankCode;
    private final String bankBranchName;
    private final String bankBranchCode;
    private final String bankAccountType;
    private final String bankAccountName;
    private final String bankAccountNumber;

    public JapanBankAccountPayload(String id,
                                   String paymentMethodName,
                                   String bankName,
                                   String bankCode,
                                   String bankBranchName,
                                   String bankBranchCode,
                                   String bankAccountType,
                                   String bankAccountName,
                                   String bankAccountNumber) {
        super(id, paymentMethodName);
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.bankBranchName = bankBranchName;
        this.bankBranchCode = bankBranchCode;
        this.bankAccountType = bankAccountType;
        this.bankAccountName = bankAccountName;
        this.bankAccountNumber = bankAccountNumber;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(bankName, 100);
        NetworkDataValidation.validateText(bankCode, 10);
        NetworkDataValidation.validateText(bankBranchName, 100);
        NetworkDataValidation.validateText(bankBranchCode, 10);
        NetworkDataValidation.validateText(bankAccountType, 20);
        NetworkDataValidation.validateText(bankAccountName, 100);
        NetworkDataValidation.validateText(bankAccountNumber, 30);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setJapanBankAccountPayload(toJapanBankAccountPayloadProto(serializeForHash));
    }

    public static JapanBankAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var japanBankPayload = proto.getJapanBankAccountPayload();
        return new JapanBankAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                japanBankPayload.getBankName(),
                japanBankPayload.getBankCode(),
                japanBankPayload.getBankBranchName(),
                japanBankPayload.getBankBranchCode(),
                japanBankPayload.getBankAccountType(),
                japanBankPayload.getBankAccountName(),
                japanBankPayload.getBankAccountNumber()
        );
    }

    private bisq.account.protobuf.JapanBankAccountPayload toJapanBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getJapanBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.JapanBankAccountPayload.Builder getJapanBankAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.JapanBankAccountPayload.newBuilder()
                .setBankName(bankName)
                .setBankCode(bankCode)
                .setBankBranchName(bankBranchName)
                .setBankBranchCode(bankBranchCode)
                .setBankAccountType(bankAccountType)
                .setBankAccountName(bankAccountName)
                .setBankAccountNumber(bankAccountNumber);
    }
}
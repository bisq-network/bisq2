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
public final class SwiftAccountPayload extends AccountPayload {

    private final String beneficiaryName;
    private final String beneficiaryAccountNr;
    private final String beneficiaryAddress;
    private final String beneficiaryCity;
    private final String beneficiaryPhone;
    private final String specialInstructions;

    private final String bankSwiftCode;
    private final String bankCountryCode;
    private final String bankName;
    private final String bankBranch;
    private final String bankAddress;

    private final String intermediarySwiftCode;
    private final String intermediaryCountryCode;
    private final String intermediaryName;
    private final String intermediaryBranch;
    private final String intermediaryAddress;

    public SwiftAccountPayload(String id,
                               String paymentMethodName,
                               String beneficiaryName,
                               String beneficiaryAccountNr,
                               String beneficiaryAddress,
                               String beneficiaryCity,
                               String beneficiaryPhone,
                               String specialInstructions,
                               String bankSwiftCode,
                               String bankCountryCode,
                               String bankName,
                               String bankBranch,
                               String bankAddress,
                               String intermediarySwiftCode,
                               String intermediaryCountryCode,
                               String intermediaryName,
                               String intermediaryBranch,
                               String intermediaryAddress) {
        super(id, paymentMethodName);
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAccountNr = beneficiaryAccountNr;
        this.beneficiaryAddress = beneficiaryAddress;
        this.beneficiaryCity = beneficiaryCity;
        this.beneficiaryPhone = beneficiaryPhone;
        this.specialInstructions = specialInstructions;
        this.bankSwiftCode = bankSwiftCode;
        this.bankCountryCode = bankCountryCode;
        this.bankName = bankName;
        this.bankBranch = bankBranch;
        this.bankAddress = bankAddress;
        this.intermediarySwiftCode = intermediarySwiftCode;
        this.intermediaryCountryCode = intermediaryCountryCode;
        this.intermediaryName = intermediaryName;
        this.intermediaryBranch = intermediaryBranch;
        this.intermediaryAddress = intermediaryAddress;
        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(beneficiaryName, 100);
        NetworkDataValidation.validateText(beneficiaryAccountNr, 100);
        NetworkDataValidation.validateText(beneficiaryAddress, 100);
        NetworkDataValidation.validateText(beneficiaryCity, 100);
        NetworkDataValidation.validatePhoneNumber(beneficiaryPhone);
        NetworkDataValidation.validateText(specialInstructions, 100);
        NetworkDataValidation.validateText(bankSwiftCode, 11);
        NetworkDataValidation.validateText(bankCountryCode, 2);
        NetworkDataValidation.validateText(bankName, 100);
        NetworkDataValidation.validateText(bankBranch, 100);
        NetworkDataValidation.validateText(bankAddress, 100);
        NetworkDataValidation.validateText(intermediarySwiftCode, 11);
        NetworkDataValidation.validateText(intermediaryCountryCode, 2);
        NetworkDataValidation.validateText(intermediaryName, 100);
        NetworkDataValidation.validateText(intermediaryBranch, 100);
        NetworkDataValidation.validateText(intermediaryAddress, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setSwiftAccountPayload(toSwiftAccountPayloadProto(serializeForHash));
    }

    public static SwiftAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var swiftPayload = proto.getSwiftAccountPayload();
        return new SwiftAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                swiftPayload.getBeneficiaryName(),
                swiftPayload.getBeneficiaryAccountNr(),
                swiftPayload.getBeneficiaryAddress(),
                swiftPayload.getBeneficiaryCity(),
                swiftPayload.getBeneficiaryPhone(),
                swiftPayload.getSpecialInstructions(),
                swiftPayload.getBankSwiftCode(),
                swiftPayload.getBankCountryCode(),
                swiftPayload.getBankName(),
                swiftPayload.getBankBranch(),
                swiftPayload.getBankAddress(),
                swiftPayload.getIntermediarySwiftCode(),
                swiftPayload.getIntermediaryCountryCode(),
                swiftPayload.getIntermediaryName(),
                swiftPayload.getIntermediaryBranch(),
                swiftPayload.getIntermediaryAddress()
        );
    }

    private bisq.account.protobuf.SwiftAccountPayload toSwiftAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSwiftAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwiftAccountPayload.Builder getSwiftAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SwiftAccountPayload.newBuilder()
                .setBeneficiaryName(beneficiaryName)
                .setBeneficiaryAccountNr(beneficiaryAccountNr)
                .setBeneficiaryAddress(beneficiaryAddress)
                .setBeneficiaryCity(beneficiaryCity)
                .setBeneficiaryPhone(beneficiaryPhone)
                .setSpecialInstructions(specialInstructions)
                .setBankSwiftCode(bankSwiftCode)
                .setBankCountryCode(bankCountryCode)
                .setBankName(bankName)
                .setBankBranch(bankBranch)
                .setBankAddress(bankAddress)
                .setIntermediarySwiftCode(intermediarySwiftCode)
                .setIntermediaryCountryCode(intermediaryCountryCode)
                .setIntermediaryName(intermediaryName)
                .setIntermediaryBranch(intermediaryBranch)
                .setIntermediaryAddress(intermediaryAddress);
    }
}
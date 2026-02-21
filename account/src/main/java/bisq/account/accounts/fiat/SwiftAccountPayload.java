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

package bisq.account.accounts.fiat;

import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public final class SwiftAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int ACCOUNT_NR_MIN_LENGTH = 2;
    public static final int ACCOUNT_NR_MAX_LENGTH = 50;
    public static final int ADDRESS_MIN_LENGTH = 5;
    public static final int ADDRESS_MAX_LENGTH = 200;
    public static final int SWIFT_CODE_MIN_LENGTH = 8;
    public static final int SWIFT_CODE_MAX_LENGTH = 11;
    public static final int PHONE_MIN_LENGTH = 5;
    public static final int PHONE_MAX_LENGTH = 30;
    public static final int INSTRUCTIONS_MIN_LENGTH = 2;
    public static final int INSTRUCTIONS_MAX_LENGTH = 300;

    private final String beneficiaryName;
    private final String beneficiaryAccountNr;
    private final Optional<String> beneficiaryPhone;
    private final String beneficiaryAddress;
    private final String selectedCurrencyCode;
    private final String bankSwiftCode;
    private final String bankName;
    private final Optional<String> bankBranch;
    private final String bankAddress;
    private final Optional<String> intermediaryBankCountryCode;
    private final Optional<String> intermediaryBankSwiftCode;
    private final Optional<String> intermediaryBankName;
    private final Optional<String> intermediaryBankBranch;
    private final Optional<String> intermediaryBankAddress;
    private final Optional<String> additionalInstructions;

    public SwiftAccountPayload(String id,
                               String bankCountryCode,
                               String beneficiaryName,
                               String beneficiaryAccountNr,
                               Optional<String> beneficiaryPhone,
                               String beneficiaryAddress,
                               String selectedCurrencyCode,
                               String bankSwiftCode,
                               String bankName,
                               Optional<String> bankBranch,
                               String bankAddress,

                               Optional<String> intermediaryBankCountryCode,
                               Optional<String> intermediaryBankSwiftCode,
                               Optional<String> intermediaryBankName,
                               Optional<String> intermediaryBankBranch,
                               Optional<String> intermediaryBankAddress,

                               Optional<String> additionalInstructions) {
        this(id,
                AccountUtils.generateSalt(),
                bankCountryCode,
                beneficiaryName,
                beneficiaryAccountNr,
                beneficiaryPhone,
                beneficiaryAddress,
                selectedCurrencyCode,
                bankSwiftCode,
                bankName,
                bankBranch,
                bankAddress,

                intermediaryBankCountryCode,
                intermediaryBankSwiftCode,
                intermediaryBankName,
                intermediaryBankBranch,
                intermediaryBankAddress,

                additionalInstructions);
    }

    public SwiftAccountPayload(String id,
                               byte[] salt,
                               String bankCountryCode,
                               String beneficiaryName,
                               String beneficiaryAccountNr,
                               Optional<String> beneficiaryPhone,
                               String beneficiaryAddress,
                               String selectedCurrencyCode,
                               String bankSwiftCode,
                               String bankName,
                               Optional<String> bankBranch,
                               String bankAddress,

                               Optional<String> intermediaryBankCountryCode,
                               Optional<String> intermediaryBankSwiftCode,
                               Optional<String> intermediaryBankName,
                               Optional<String> intermediaryBankBranch,
                               Optional<String> intermediaryBankAddress,

                               Optional<String> additionalInstructions) {
        super(id, salt, bankCountryCode);
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAccountNr = beneficiaryAccountNr;
        this.beneficiaryPhone = beneficiaryPhone;
        this.beneficiaryAddress = beneficiaryAddress;
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.bankSwiftCode = bankSwiftCode;
        this.bankName = bankName;
        this.bankBranch = bankBranch;
        this.bankAddress = bankAddress;
        this.intermediaryBankCountryCode = intermediaryBankCountryCode;
        this.intermediaryBankSwiftCode = intermediaryBankSwiftCode;
        this.intermediaryBankName = intermediaryBankName;
        this.intermediaryBankBranch = intermediaryBankBranch;
        this.intermediaryBankAddress = intermediaryBankAddress;
        this.additionalInstructions = additionalInstructions;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        NetworkDataValidation.validateRequiredText(beneficiaryName, NAME_MIN_LENGTH, NAME_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(beneficiaryAccountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(beneficiaryAddress, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(bankSwiftCode, SWIFT_CODE_MIN_LENGTH, SWIFT_CODE_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(bankName, NAME_MIN_LENGTH, NAME_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(bankAddress, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH);

        bankBranch.ifPresent(value -> NetworkDataValidation.validateText(value, NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        intermediaryBankCountryCode.ifPresent(NetworkDataValidation::validateCode);
        intermediaryBankSwiftCode.ifPresent(value -> NetworkDataValidation.validateText(value, SWIFT_CODE_MIN_LENGTH, SWIFT_CODE_MAX_LENGTH));
        intermediaryBankName.ifPresent(value -> NetworkDataValidation.validateText(value, NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        intermediaryBankBranch.ifPresent(value -> NetworkDataValidation.validateText(value, NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        intermediaryBankAddress.ifPresent(value -> NetworkDataValidation.validateText(value, ADDRESS_MIN_LENGTH, ADDRESS_MAX_LENGTH));
        beneficiaryPhone.ifPresent(value -> NetworkDataValidation.validateText(value, PHONE_MIN_LENGTH, PHONE_MAX_LENGTH));
        additionalInstructions.ifPresent(value -> NetworkDataValidation.validateText(value, INSTRUCTIONS_MIN_LENGTH, INSTRUCTIONS_MAX_LENGTH));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setSwiftAccountPayload(
                toSwiftAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SwiftAccountPayload toSwiftAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSwiftAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SwiftAccountPayload.Builder getSwiftAccountPayloadBuilder(boolean serializeForHash) {
        var builder = bisq.account.protobuf.SwiftAccountPayload.newBuilder()
                .setBeneficiaryName(beneficiaryName)
                .setBeneficiaryAccountNr(beneficiaryAccountNr)
                .setBeneficiaryAddress(beneficiaryAddress)
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setBankSwiftCode(bankSwiftCode)
                .setBankName(bankName)
                .setBankAddress(bankAddress);
        bankBranch.ifPresent(builder::setBankBranch);
        intermediaryBankCountryCode.ifPresent(builder::setIntermediaryBankCountryCode);
        intermediaryBankSwiftCode.ifPresent(builder::setIntermediaryBankSwiftCode);
        intermediaryBankName.ifPresent(builder::setIntermediaryBankName);
        intermediaryBankBranch.ifPresent(builder::setIntermediaryBankBranch);
        intermediaryBankAddress.ifPresent(builder::setIntermediaryBankAddress);
        beneficiaryPhone.ifPresent(builder::setBeneficiaryPhone);
        additionalInstructions.ifPresent(builder::setAdditionalInstructions);
        return builder;
    }

    public static SwiftAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.SwiftAccountPayload payload = countryBasedAccountPayload.getSwiftAccountPayload();
        Optional<String> beneficiaryPhone = payload.hasBeneficiaryPhone()
                ? Optional.of(payload.getBeneficiaryPhone())
                : Optional.empty();
        Optional<String> bankBranch = payload.hasBankBranch()
                ? Optional.of(payload.getBankBranch())
                : Optional.empty();
        Optional<String> intermediaryBankCountryCode = payload.hasIntermediaryBankCountryCode()
                ? Optional.of(payload.getIntermediaryBankCountryCode())
                : Optional.empty();
        Optional<String> intermediaryBankSwiftCode = payload.hasIntermediaryBankSwiftCode()
                ? Optional.of(payload.getIntermediaryBankSwiftCode())
                : Optional.empty();
        Optional<String> intermediaryBankName = payload.hasIntermediaryBankName()
                ? Optional.of(payload.getIntermediaryBankName())
                : Optional.empty();
        Optional<String> intermediaryBankBranch = payload.hasIntermediaryBankBranch()
                ? Optional.of(payload.getIntermediaryBankBranch())
                : Optional.empty();
        Optional<String> intermediaryBankAddress = payload.hasIntermediaryBankAddress()
                ? Optional.of(payload.getIntermediaryBankAddress())
                : Optional.empty();
        Optional<String> additionalInstructions = payload.hasAdditionalInstructions()
                ? Optional.of(payload.getAdditionalInstructions())
                : Optional.empty();
        return new SwiftAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getBeneficiaryName(),
                payload.getBeneficiaryAccountNr(),
                beneficiaryPhone,
                payload.getBeneficiaryAddress(),
                payload.getSelectedCurrencyCode(),
                payload.getBankSwiftCode(),
                payload.getBankName(),
                bankBranch,
                payload.getBankAddress(),
                intermediaryBankCountryCode,
                intermediaryBankSwiftCode,
                intermediaryBankName,
                intermediaryBankBranch,
                intermediaryBankAddress,
                additionalInstructions
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SWIFT);
    }

    @Override
    public String getDefaultAccountName() {
        return getPaymentMethod().getShortDisplayString() + "-" + StringUtils.truncate(bankSwiftCode, 4);
    }

    @Override
    public String getAccountDataDisplayString() {
        AccountDataDisplayStringBuilder builder = new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.swift.beneficiaryName"), beneficiaryName,
                Res.get("paymentAccounts.swift.beneficiaryAccountNr"), beneficiaryAccountNr,
                Res.get("paymentAccounts.swift.beneficiaryAddress"), beneficiaryAddress,
                Res.get("paymentAccounts.swift.bankSwiftCode"), bankSwiftCode,
                Res.get("paymentAccounts.swift.bankName"), bankName,
                Res.get("paymentAccounts.swift.bankAddress"), bankAddress
        );
        beneficiaryPhone.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.beneficiaryPhone"), value));
        bankBranch.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.bankBranch"), value));
        intermediaryBankCountryCode.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.intermediaryBankCountry"), value));
        intermediaryBankSwiftCode.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.intermediaryBankSwiftCode"), value));
        intermediaryBankName.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.intermediaryBankName"), value));
        intermediaryBankBranch.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.intermediaryBankBranch"), value));
        intermediaryBankAddress.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.intermediaryBankAddress"), value));
        additionalInstructions.ifPresent(value -> builder.add(Res.get("paymentAccounts.swift.additionalInstructions"), value));
        return builder.toString();
    }

    @Override
    public byte[] getFingerprint() {
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8),
                beneficiaryAccountNr.getBytes(StandardCharsets.UTF_8));
    }
}

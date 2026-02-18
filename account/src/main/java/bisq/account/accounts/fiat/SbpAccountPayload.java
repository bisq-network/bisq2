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

import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SbpAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int BANK_NAME_MIN_LENGTH = 2;
    public static final int BANK_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String mobileNumber;
    private final String bankName;

    public SbpAccountPayload(String id, String holderName, String mobileNumber, String bankName) {
        this(id, AccountUtils.generateSalt(), holderName, mobileNumber, bankName);
    }

    public SbpAccountPayload(String id,
                             byte[] salt,
                             String holderName,
                             String mobileNumber,
                             String bankName) {
        super(id, salt, "RU");
        this.holderName = holderName;
        this.mobileNumber = mobileNumber;
        this.bankName = bankName;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateHolderName(holderName);
        NetworkDataValidation.validateRequiredText(bankName, BANK_NAME_MIN_LENGTH, BANK_NAME_MAX_LENGTH);
        checkArgument(PhoneNumberValidation.isValid(mobileNumber, "RU"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setSbpAccountPayload(toSbpAccountPayloadProto(serializeForHash));
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

    public static SbpAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var sbpAccountPayload = proto.getCountryBasedAccountPayload().getSbpAccountPayload();
        return new SbpAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                sbpAccountPayload.getHolderName(),
                sbpAccountPayload.getMobileNumber(),
                sbpAccountPayload.getBankName()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SBP);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.mobileNr"), mobileNumber,
                Res.get("paymentAccounts.bank.bankName"), bankName
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(holderName);
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(mobileNumber.getBytes(StandardCharsets.UTF_8),
                bankName.getBytes(StandardCharsets.UTF_8));
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }
}

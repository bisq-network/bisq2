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
import bisq.common.validation.EmailValidation;
import bisq.common.validation.PaymentAccountValidation;
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
public final class WiseUsdAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String holderName;
    private final String email;
    private final String beneficiaryAddress;

    public WiseUsdAccountPayload(String id,
                                 String countryCode,
                                 String holderName,
                                 String email,
                                 String beneficiaryAddress
    ) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                holderName,
                email,
                beneficiaryAddress);
    }

    public WiseUsdAccountPayload(String id,
                                 byte[] salt,
                                 String countryCode,
                                 String holderName,
                                 String email,
                                 String beneficiaryAddress
    ) {
        super(id, salt, countryCode);
        this.holderName = holderName;
        this.email = email;
        this.beneficiaryAddress = beneficiaryAddress;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        checkArgument(EmailValidation.isValid(email));
        PaymentAccountValidation.validateHolderName(holderName);
        PaymentAccountValidation.validateAddress(beneficiaryAddress);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setWiseUsdAccountPayload(toWiseUsdAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.WiseUsdAccountPayload toWiseUsdAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getWiseUsdAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.WiseUsdAccountPayload.Builder getWiseUsdAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.WiseUsdAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setEmail(email)
                .setBeneficiaryAddress(beneficiaryAddress);
    }

    public static WiseUsdAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var payload = countryBasedAccountPayload.getWiseUsdAccountPayload();
        return new WiseUsdAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getHolderName(),
                payload.getEmail(),
                payload.getBeneficiaryAddress()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE_USD);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.email"), email
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(holderName);
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(holderName.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected String getBisq1CompatiblePaymentMethodId() {
        return "TRANSFERWISE_USD";
    }
}

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

import bisq.account.accounts.AccountUtils;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.EmailValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneyBeamAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    private final String selectedCurrencyCode;
    private final String holderName;
    private final String emailOrMobileNr;

    public MoneyBeamAccountPayload(String id,
                                   String countryCode,
                                   String selectedCurrencyCode,
                                   String holderName,
                                   String emailOrMobileNr) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                selectedCurrencyCode,
                holderName,
                emailOrMobileNr);
    }

    private MoneyBeamAccountPayload(String id,
                                    byte[] salt,
                                    String countryCode,
                                    String selectedCurrencyCode,
                                    String holderName,
                                    String emailOrMobileNr) {
        super(id, salt, countryCode);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.holderName = holderName;
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        PaymentAccountValidation.validateHolderName(holderName);
        checkArgument(EmailValidation.isValid(emailOrMobileNr) || PhoneNumberValidation.isValid(emailOrMobileNr, countryCode));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setMoneyBeamAccountPayload(
                toMoneyBeamAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneyBeamAccountPayload toMoneyBeamAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyBeamAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyBeamAccountPayload.Builder getMoneyBeamAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyBeamAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setHolderName(holderName)
                .setEmailOrMobileNr(emailOrMobileNr);
    }

    public static MoneyBeamAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getCountryBasedAccountPayload().getMoneyBeamAccountPayload();
        return new MoneyBeamAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                proto.getCountryBasedAccountPayload().getCountryCode(),
                payload.getSelectedCurrencyCode(),
                payload.getHolderName(),
                payload.getEmailOrMobileNr());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MONEY_BEAM);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.emailOrMobileNr"), emailOrMobileNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(emailOrMobileNr.getBytes(StandardCharsets.UTF_8),
                holderName.getBytes(StandardCharsets.UTF_8));
        return super.getFingerprint(data);
    }
}

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
import bisq.common.validation.EmailValidation;
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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ZelleAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String holderName;
    private final String emailOrMobileNr;

    public ZelleAccountPayload(String id, String holderName, String emailOrMobileNr) {
        this(id, AccountUtils.generateSalt(), holderName, emailOrMobileNr);
    }

    public ZelleAccountPayload(String id, byte[] salt, String holderName, String emailOrMobileNr) {
        super(id, salt, "US");
        this.holderName = holderName;
        this.emailOrMobileNr = emailOrMobileNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateHolderName(holderName);
        checkArgument(EmailValidation.isValid(emailOrMobileNr) ||
                PhoneNumberValidation.isValid(emailOrMobileNr, "US"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setZelleAccountPayload(toZelleAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.ZelleAccountPayload toZelleAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getZelleAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ZelleAccountPayload.Builder getZelleAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ZelleAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setEmailOrMobileNr(emailOrMobileNr);
    }

    public static ZelleAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var zelleProto = proto.getCountryBasedAccountPayload().getZelleAccountPayload();
        return new ZelleAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                zelleProto.getHolderName(),
                zelleProto.getEmailOrMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);
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
        byte[] data = emailOrMobileNr.getBytes(StandardCharsets.UTF_8);
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }

    @Override
    protected String getBisq1CompatiblePaymentMethodId() {
        return "CLEAR_X_CHANGE";
    }
}

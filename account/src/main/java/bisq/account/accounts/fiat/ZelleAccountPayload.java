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
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.validation.EmailValidation;
import bisq.common.validation.NetworkDataValidation;
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
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ZelleAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String emailOrMobileNr;

    public ZelleAccountPayload(String id, String holderName, String emailOrMobileNr, String paymentMethodId, byte[] salt) {
        super(id, "US", paymentMethodId, salt);
        this.holderName = holderName;
        this.emailOrMobileNr = emailOrMobileNr;
    }

    public ZelleAccountPayload(String id, String holderName, String emailOrMobileNr) {
        super(id, "US");
        this.holderName = holderName;
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        checkArgument(EmailValidation.isValid(emailOrMobileNr) ||
                PhoneNumberValidation.isValid(emailOrMobileNr, "US"));
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
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
        var zelleProto = proto.getZelleAccountPayload();
        return new ZelleAccountPayload(
                proto.getId(),
                zelleProto.getHolderName(),
                zelleProto.getEmailOrMobileNr(),
                proto.getPaymentMethodId(),
                proto.getSalt().toByteArray()
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
    public byte[] getAgeWitnessInputData() {
        // We don't add holderName because we don't want to break age validation if the user recreates an account with
        // slight changes in holder name (e.g. add or remove middle name)
        // Also we want to be compatible with Bisq 1 to not break account age data
        return super.getAgeWitnessInputData(emailOrMobileNr.getBytes(StandardCharsets.UTF_8));
    }
}

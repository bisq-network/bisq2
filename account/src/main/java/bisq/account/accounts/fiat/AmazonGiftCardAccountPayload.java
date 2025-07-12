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
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.EmailValidation;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class AmazonGiftCardAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String emailOrMobileNr;

    public AmazonGiftCardAccountPayload(String id, String countryCode, String emailOrMobileNr) {
        super(id, countryCode);
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    public void verify() {
        super.verify();
        checkArgument(EmailValidation.isValid(emailOrMobileNr) || PhoneNumberValidation.isValid(emailOrMobileNr, countryCode));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setAmazonGiftCardAccountPayload(
                toAmazonGiftCardAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.AmazonGiftCardAccountPayload toAmazonGiftCardAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getAmazonGiftCardAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AmazonGiftCardAccountPayload.Builder getAmazonGiftCardAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AmazonGiftCardAccountPayload.newBuilder().setEmailOrMobileNr(emailOrMobileNr);
    }

    public static AmazonGiftCardAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload =
                proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.AmazonGiftCardAccountPayload amazonGiftCardAccountPayload =
                countryBasedAccountPayload.getAmazonGiftCardAccountPayload();
        return new AmazonGiftCardAccountPayload(
                proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                amazonGiftCardAccountPayload.getEmailOrMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.AMAZON_GIFT_CARD);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.emailOrMobileNr"), emailOrMobileNr
        ).toString();
    }
}

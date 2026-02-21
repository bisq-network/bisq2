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
public final class AmazonGiftCardAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    private final String emailOrMobileNr;
    private final String selectedCurrencyCode;

    public AmazonGiftCardAccountPayload(String id,
                                        String countryCode,
                                        String selectedCurrencyCode,
                                        String emailOrMobileNr) {
        this(id, AccountUtils.generateSalt(), countryCode, selectedCurrencyCode, emailOrMobileNr);
    }

    public AmazonGiftCardAccountPayload(String id,
                                        byte[] salt,
                                        String countryCode,
                                        String selectedCurrencyCode,
                                        String emailOrMobileNr) {
        super(id, salt, countryCode);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.emailOrMobileNr = emailOrMobileNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        checkArgument(StringUtils.isNotEmpty(emailOrMobileNr));
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
        return bisq.account.protobuf.AmazonGiftCardAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setEmailOrMobileNr(emailOrMobileNr);
    }

    public static AmazonGiftCardAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload =
                proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.AmazonGiftCardAccountPayload amazonGiftCardAccountPayload =
                countryBasedAccountPayload.getAmazonGiftCardAccountPayload();
        return new AmazonGiftCardAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                amazonGiftCardAccountPayload.getSelectedCurrencyCode(),
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
                Res.get("paymentAccounts.emailOrMobileNr"), emailOrMobileNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ("AmazonGiftCard" + emailOrMobileNr).getBytes(StandardCharsets.UTF_8);
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }
}

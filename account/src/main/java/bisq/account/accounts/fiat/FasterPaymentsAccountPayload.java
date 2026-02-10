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

import bisq.account.accounts.util.AccountUtils;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class FasterPaymentsAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String sortCode;
    private final String accountNr;

    public FasterPaymentsAccountPayload(String id, String holderName, String sortCode, String accountNr) {
        this(id, AccountUtils.generateSalt(), holderName, sortCode, accountNr);
    }

    public FasterPaymentsAccountPayload(String id,
                                         byte[] salt,
                                         String holderName,
                                         String sortCode,
                                         String accountNr) {
        super(id, salt, "UK");
        this.holderName = holderName;
        this.sortCode = sortCode;
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        PaymentAccountValidation.validateFasterPaymentsSortCode(sortCode);
        PaymentAccountValidation.validateFasterPaymentsAccountNr(accountNr);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setFasterPaymentsAccountPayload(toFasterPaymentsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload toFasterPaymentsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getFasterPaymentsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload.Builder getFasterPaymentsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.FasterPaymentsAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setSortCode(sortCode)
                .setAccountNr(accountNr);
    }

    public static FasterPaymentsAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var fasterPaymentsPayload = proto.getCountryBasedAccountPayload().getFasterPaymentsAccountPayload();
        return new FasterPaymentsAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                fasterPaymentsPayload.getHolderName(),
                fasterPaymentsPayload.getSortCode(),
                fasterPaymentsPayload.getAccountNr());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.FASTER_PAYMENTS);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.fasterPayments.sortCode"), sortCode,
                Res.get("paymentAccounts.fasterPayments.accountNr"), accountNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(sortCode.getBytes(StandardCharsets.UTF_8),
                accountNr.getBytes(StandardCharsets.UTF_8));
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }
}

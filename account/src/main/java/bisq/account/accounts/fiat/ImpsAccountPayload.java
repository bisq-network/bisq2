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
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class ImpsAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int ACCOUNT_NR_MIN_LENGTH = 1;
    public static final int ACCOUNT_NR_MAX_LENGTH = 50;
    public static final int IFSC_MIN_LENGTH = 1;
    public static final int IFSC_MAX_LENGTH = 50;

    private final String holderName;
    private final String accountNr;
    private final String ifsc;

    public ImpsAccountPayload(String id, String holderName, String accountNr, String ifsc) {
        this(id, AccountUtils.generateSalt(), holderName, accountNr, ifsc);
    }

    public ImpsAccountPayload(String id,
                              byte[] salt,
                              String holderName,
                              String accountNr,
                              String ifsc) {
        super(id, salt, "IN");
        this.holderName = holderName;
        this.accountNr = accountNr;
        this.ifsc = ifsc;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateHolderName(holderName);
        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(ifsc, IFSC_MIN_LENGTH, IFSC_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setImpsAccountPayload(toImpsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.ImpsAccountPayload toImpsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getImpsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ImpsAccountPayload.Builder getImpsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ImpsAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setAccountNr(accountNr)
                .setIfsc(ifsc);
    }

    public static ImpsAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getCountryBasedAccountPayload().getImpsAccountPayload();
        return new ImpsAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getHolderName(),
                payload.getAccountNr(),
                payload.getIfsc()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.IMPS);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.accountNr"), accountNr,
                Res.get("paymentAccounts.bank.bankId"), ifsc
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(accountNr.getBytes(StandardCharsets.UTF_8));
    }
}

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

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
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

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PayseraAccountPayload extends AccountPayload<FiatPaymentMethod>
        implements SelectableCurrencyAccountPayload {
    private final String selectedCurrencyCode;
    private final String email;

    public PayseraAccountPayload(String id, String selectedCurrencyCode, String email) {
        this(id, AccountUtils.generateSalt(), selectedCurrencyCode, email);
    }

    public PayseraAccountPayload(String id,
                                 byte[] salt,
                                 String selectedCurrencyCode,
                                 String email) {
        super(id, salt);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.email = email;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        checkArgument(EmailValidation.isValid(email));
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setPayseraAccountPayload(toPayseraAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PayseraAccountPayload toPayseraAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPayseraAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayseraAccountPayload.Builder getPayseraAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayseraAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setEmail(email);
    }

    public static PayseraAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getPayseraAccountPayload();
        return new PayseraAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getSelectedCurrencyCode(),
                payload.getEmail()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PAYSERA);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.email"), email
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(email.getBytes(StandardCharsets.UTF_8));
    }
}

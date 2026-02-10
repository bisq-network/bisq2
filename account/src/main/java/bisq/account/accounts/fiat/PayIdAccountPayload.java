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
public final class PayIdAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String holderName;
    private final String payId;

    public PayIdAccountPayload(String id, String holderName, String payId) {
        this(id, AccountUtils.generateSalt(), holderName, payId);
    }

    public PayIdAccountPayload(String id, byte[] salt, String holderName, String payId) {
        super(id, salt, "AU");
        this.holderName = holderName;
        this.payId = payId;

        verify();
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setPayIdAccountPayload(toPayIdAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PayIdAccountPayload toPayIdAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPayIdAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PayIdAccountPayload.Builder getPayIdAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PayIdAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setPayId(payId);
    }

    public static PayIdAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getCountryBasedAccountPayload().getPayIdAccountPayload();
        return new PayIdAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getHolderName(),
                payload.getPayId());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PAY_ID);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.payId.payId"), payId
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = (payId + holderName).getBytes(StandardCharsets.UTF_8);
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }

    @Override
    protected String getBisq1CompatiblePaymentMethodId() {
        return "AUSTRALIA_PAYID";
    }
}

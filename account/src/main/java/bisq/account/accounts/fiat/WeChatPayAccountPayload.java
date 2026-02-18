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
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Getter
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class WeChatPayAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int ACCOUNT_NR_MIN_LENGTH = 1;
    public static final int ACCOUNT_NR_MAX_LENGTH = 50;

    private final String accountNr;

    public WeChatPayAccountPayload(String id, String accountNr) {
        this(id, AccountUtils.generateSalt(), accountNr);
    }

    public WeChatPayAccountPayload(String id, byte[] salt, String accountNr) {
        super(id, salt, "CN");
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setWeChatPayAccountPayload(toWeChatPayAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.WeChatPayAccountPayload toWeChatPayAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getWeChatPayAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.WeChatPayAccountPayload.Builder getWeChatPayAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.WeChatPayAccountPayload.newBuilder()
                .setAccountNr(accountNr);
    }

    public static WeChatPayAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getCountryBasedAccountPayload().getWeChatPayAccountPayload();
        return new WeChatPayAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getAccountNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WECHAT_PAY);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.accountNr"), accountNr
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(accountNr);
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = accountNr.getBytes(StandardCharsets.UTF_8);
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }
}

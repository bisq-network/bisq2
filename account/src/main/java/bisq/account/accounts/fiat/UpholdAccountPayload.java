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
import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.StringUtils;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UpholdAccountPayload extends AccountPayload<FiatPaymentMethod> implements MultiCurrencyAccountPayload {
    private final List<String> selectedCurrencyCodes;
    private final String holderName;
    private final String accountId;

    public UpholdAccountPayload(String id,
                                List<String> selectedCurrencyCodes,
                                String holderName,
                                String accountId
    ) {
        this(id,
                AccountUtils.generateSalt(),
                selectedCurrencyCodes,
                holderName,
                accountId);
    }

    public UpholdAccountPayload(String id,
                                 byte[] salt,
                                 List<String> selectedCurrencyCodes,
                                 String holderName,
                                 String accountId
    ) {
        super(id, salt);
        this.selectedCurrencyCodes = selectedCurrencyCodes;
        this.holderName = holderName;
        this.accountId = accountId;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCodes(selectedCurrencyCodes);
        checkArgument(StringUtils.isNotEmpty(accountId));
        // We don't verify holderName as it was optional in Bisq 1
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setUpholdAccountPayload(toUpholdAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.UpholdAccountPayload toUpholdAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUpholdAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpholdAccountPayload.Builder getUpholdAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpholdAccountPayload.newBuilder()
                .addAllSelectedCurrencyCodes(selectedCurrencyCodes)
                .setHolderName(holderName)
                .setAccountId(accountId);
    }

    public static UpholdAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getUpholdAccountPayload();
        return new UpholdAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getSelectedCurrencyCodesList(),
                payload.getHolderName(),
                payload.getAccountId()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.UPHOLD);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.uphold.accountId"), accountId
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(holderName);
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(accountId.getBytes(StandardCharsets.UTF_8));
    }
}

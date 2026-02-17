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
import bisq.account.payment_method.fiat.FiatPaymentRailUtil;
import bisq.common.validation.PaymentAccountValidation;
import bisq.common.validation.fiat.AdvancedCashAccountNrValidation;
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
public final class AdvancedCashAccountPayload extends AccountPayload<FiatPaymentMethod> implements MultiCurrencyAccountPayload {
    private final List<String> selectedCurrencyCodes;
    private final String accountNr;

    public AdvancedCashAccountPayload(String id, List<String> selectedCurrencyCodes, String accountNr) {
        this(id, AccountUtils.generateSalt(), selectedCurrencyCodes, accountNr);
    }

    public AdvancedCashAccountPayload(String id,
                                      byte[] salt,
                                      List<String> selectedCurrencyCodes,
                                      String accountNr) {
        super(id, salt);
        this.selectedCurrencyCodes = selectedCurrencyCodes;
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateCurrencyCodes(selectedCurrencyCodes,
                FiatPaymentRailUtil.getAdvancedCashCurrencyCodes(),
                "Advanced Cash currency codes");
        checkArgument(AdvancedCashAccountNrValidation.getInstance().isValid(accountNr),
                "Account number is invalid");
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setAdvancedCashAccountPayload(toAdvancedCashAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.AdvancedCashAccountPayload toAdvancedCashAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getAdvancedCashAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AdvancedCashAccountPayload.Builder getAdvancedCashAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AdvancedCashAccountPayload.newBuilder()
                .addAllSelectedCurrencyCodes(selectedCurrencyCodes)
                .setAccountNr(accountNr);
    }

    public static AdvancedCashAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getAdvancedCashAccountPayload();
        return new AdvancedCashAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getSelectedCurrencyCodesList(),
                payload.getAccountNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ADVANCED_CASH);
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
        return super.getFingerprint(accountNr.getBytes(StandardCharsets.UTF_8));
    }
}

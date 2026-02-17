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

import bisq.account.accounts.MultiCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.EmailValidation;
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
public final class MoneyGramAccountPayload extends CountryBasedAccountPayload implements MultiCurrencyAccountPayload {
    private final List<String> selectedCurrencyCodes;
    private final String holderName;
    private final String email;
    private final String state;

    public MoneyGramAccountPayload(String id,
                                   String countryCode,
                                   List<String> selectedCurrencyCodes,
                                   String holderName,
                                   String email,
                                   String state
    ) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                selectedCurrencyCodes,
                holderName,
                email,
                state);
    }

    public MoneyGramAccountPayload(String id,
                                    byte[] salt,
                                    String countryCode,
                                    List<String> selectedCurrencyCodes,
                                    String holderName,
                                    String email,
                                    String state
    ) {
        super(id, salt, countryCode);
        this.selectedCurrencyCodes = selectedCurrencyCodes;
        this.holderName = holderName;
        this.email = email;
        this.state = state;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCodes(selectedCurrencyCodes);
        checkArgument(EmailValidation.isValid(email));
        PaymentAccountValidation.validateHolderName(holderName);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setMoneyGramAccountPayload(toMoneyGramAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneyGramAccountPayload toMoneyGramAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyGramAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyGramAccountPayload.Builder getMoneyGramAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyGramAccountPayload.newBuilder()
                .addAllSelectedCurrencyCodes(selectedCurrencyCodes)
                .setHolderName(holderName)
                .setEmail(email)
                .setState(state);
    }

    public static MoneyGramAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var payload = countryBasedAccountPayload.getMoneyGramAccountPayload();

        return new MoneyGramAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getSelectedCurrencyCodesList(),
                payload.getHolderName(),
                payload.getEmail(),
                payload.getState()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MONEY_GRAM);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.email"), email
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(holderName);
    }

    @Override
    public byte[] getFingerprint() {
        String data = countryCode + state + holderName + email;
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data.getBytes(StandardCharsets.UTF_8));
    }
}

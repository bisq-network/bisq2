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

import bisq.account.accounts.AccountUtils;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.validation.NetworkDataValidation;
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
public final class PromptPayAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int PROMPT_PAY_ID_MIN_LENGTH = 2;
    public static final int PROMPT_PAY_ID_MAX_LENGTH = 70;

    private final String promptPayId;

    public PromptPayAccountPayload(String id, String countryCode, String promptPayId) {
        this(id, AccountUtils.generateSalt(), countryCode, promptPayId);
    }

    public PromptPayAccountPayload(String id, byte[] salt, String countryCode, String promptPayId) {
        super(id, salt, countryCode);
        this.promptPayId = promptPayId;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(promptPayId, PROMPT_PAY_ID_MIN_LENGTH, PROMPT_PAY_ID_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setPromptPayAccountPayload(
                toPromptPayAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PromptPayAccountPayload toPromptPayAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPromptPayAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PromptPayAccountPayload.Builder getPromptPayAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PromptPayAccountPayload.newBuilder()
                .setPromptPayId(promptPayId);
    }

    public static PromptPayAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.PromptPayAccountPayload payload = countryBasedAccountPayload.getPromptPayAccountPayload();
        return new PromptPayAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getPromptPayId()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PROMPT_PAY);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.promptPay.promptPayId"), promptPayId
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(promptPayId.getBytes(StandardCharsets.UTF_8));
    }
}

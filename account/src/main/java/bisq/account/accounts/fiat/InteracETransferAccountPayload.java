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
import bisq.common.util.StringUtils;
import bisq.common.validation.EmailValidation;
import bisq.common.validation.PaymentAccountValidation;
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
public final class InteracETransferAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String holderName;
    private final String email;
    private final String question;
    private final String answer;

    public InteracETransferAccountPayload(String id, String holderName, String email, String question, String answer) {
        this(id, AccountUtils.generateSalt(), holderName, email, question, answer);
    }

    public InteracETransferAccountPayload(String id,
                                           byte[] salt,
                                           String holderName,
                                           String email,
                                           String question,
                                           String answer) {
        super(id, salt, "CA");
        this.holderName = holderName;
        this.email = email;
        this.question = question;
        this.answer = answer;

        verify();
    }


    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateHolderName(holderName);
        checkArgument(EmailValidation.isValid(email));
        checkArgument(StringUtils.isNotEmpty(question), "question must not be empty");
        checkArgument(StringUtils.isNotEmpty(answer), "answer must not be empty");
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setInteracETransferAccountPayload(toInteracETransferAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.InteracETransferAccountPayload toInteracETransferAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getInteracETransferAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.InteracETransferAccountPayload.Builder getInteracETransferAccountPayloadBuilder(
            boolean serializeForHash) {
        return bisq.account.protobuf.InteracETransferAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setEmail(email)
                .setQuestion(question)
                .setAnswer(answer);
    }

    public static InteracETransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var interactETransferAccountPayload = proto.getCountryBasedAccountPayload().getInteracETransferAccountPayload();
        return new InteracETransferAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                interactETransferAccountPayload.getHolderName(),
                interactETransferAccountPayload.getEmail(),
                interactETransferAccountPayload.getQuestion(),
                interactETransferAccountPayload.getAnswer()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.INTERAC_E_TRANSFER);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.email"), email,
                Res.get("paymentAccounts.interacETransfer.question"), question,
                Res.get("paymentAccounts.interacETransfer.answer"), answer
        ).toString();
    }

    @Override
    public Optional<String> getReasonForPaymentString() {
        return Optional.of(holderName);
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(email.getBytes(StandardCharsets.UTF_8),
                question.getBytes(StandardCharsets.UTF_8),
                answer.getBytes(StandardCharsets.UTF_8));
        // We do not call super.getFingerprint(data) to not include the countryCode to stay compatible with
        // Bisq 1 account age fingerprint.
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }
}

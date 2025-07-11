package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PromptPayAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int PROMPT_PAY_ID_MIN_LENGTH = 2;
    public static final int PROMPT_PAY_ID_MAX_LENGTH = 70;

    private final String promptPayId;

    public PromptPayAccountPayload(String id, String countryCode, String promptPayId) {
        super(id, countryCode);
        this.promptPayId = promptPayId;
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

    public static PromptPayAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.PromptPayAccountPayload payload = countryBasedAccountPayload.getPromptPayAccountPayload();
        return new PromptPayAccountPayload(proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getPromptPayId()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIX);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.promptPay.promptPayId"), promptPayId
        ).toString();
    }
}

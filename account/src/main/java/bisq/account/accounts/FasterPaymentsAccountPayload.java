package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class FasterPaymentsAccountPayload extends CountryBasedAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String sortCode;
    private final String accountNr;

    public FasterPaymentsAccountPayload(String id, String holderName, String sortCode, String accountNr) {
        super(id, "UK");
        this.holderName = holderName;
        this.sortCode = sortCode;
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        PaymentAccountValidation.validateFasterPaymentsSortCode(sortCode);
        PaymentAccountValidation.validateFasterPaymentsAccountNr(accountNr);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setFasterPaymentsAccountPayload(toFasterPaymentsAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload toFasterPaymentsAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getFasterPaymentsAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.FasterPaymentsAccountPayload.Builder getFasterPaymentsAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.FasterPaymentsAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setSortCode(sortCode)
                .setAccountNr(accountNr);
    }

    public static FasterPaymentsAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var fasterPaymentsPayload = proto.getFasterPaymentsAccountPayload();
        return new FasterPaymentsAccountPayload(proto.getId(),
                fasterPaymentsPayload.getHolderName(),
                fasterPaymentsPayload.getSortCode(),
                fasterPaymentsPayload.getAccountNr());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.FASTER_PAYMENTS);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.fasterPayments.sortCode"), sortCode,
                Res.get("user.paymentAccounts.fasterPayments.accountNr"), accountNr
        ).toString();
    }
}

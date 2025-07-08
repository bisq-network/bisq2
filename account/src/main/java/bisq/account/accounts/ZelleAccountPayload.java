package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.common.validation.EmailValidation;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ZelleAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String emailOrMobileNr;

    public ZelleAccountPayload(String id, String holderName, String emailOrMobileNr) {
        super(id, "US");
        this.holderName = holderName;
        this.emailOrMobileNr = emailOrMobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        checkArgument(EmailValidation.isValid(emailOrMobileNr) ||
                PhoneNumberValidation.isValid(emailOrMobileNr, "US"));
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setZelleAccountPayload(toZelleAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.ZelleAccountPayload toZelleAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getZelleAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.ZelleAccountPayload.Builder getZelleAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.ZelleAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setEmailOrMobileNr(emailOrMobileNr);
    }

    public static ZelleAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var zelleProto = proto.getZelleAccountPayload();
        return new ZelleAccountPayload(
                proto.getId(),
                zelleProto.getHolderName(),
                zelleProto.getEmailOrMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.holderName"), holderName,
                Res.get("user.paymentAccounts.emailOrMobileNr"), emailOrMobileNr
        ).toString();
    }
}

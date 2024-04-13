package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class ZelleAccountPayload extends AccountPayload {
    private final String emailOrMobileNr;
    private final String holderName;

    public ZelleAccountPayload(String id, String paymentMethodName, String emailOrMobileNr, String holderName) {
        super(id, paymentMethodName);
        this.emailOrMobileNr = emailOrMobileNr;
        this.holderName = holderName;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation).setZelleAccountPayload(
                        bisq.account.protobuf.ZelleAccountPayload.newBuilder()
                                .setEmailOrMobileNr(emailOrMobileNr)
                                .setHolderName(holderName));
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static ZelleAccountPayload fromProto(bisq.account.protobuf.AccountPayload accountPayload) {
        var zelleProto = accountPayload.getZelleAccountPayload();
        return new ZelleAccountPayload(
                accountPayload.getId(),
                accountPayload.getPaymentMethodName(),
                zelleProto.getEmailOrMobileNr(),
                zelleProto.getHolderName()
        );
    }
}

package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class ZelleAccountPayload extends AccountPayload<FiatPaymentMethod> {
    private final String emailOrMobileNr;
    private final String holderName;

    public ZelleAccountPayload(String id, String emailOrMobileNr, String holderName) {
        super(id);
        this.emailOrMobileNr = emailOrMobileNr;
        this.holderName = holderName;
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
                .setEmailOrMobileNr(emailOrMobileNr)
                .setHolderName(holderName);
    }

    public static ZelleAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var zelleProto = proto.getZelleAccountPayload();
        return new ZelleAccountPayload(
                proto.getId(),
                zelleProto.getEmailOrMobileNr(),
                zelleProto.getHolderName()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);
    }
}

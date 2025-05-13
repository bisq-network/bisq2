package bisq.account.accounts;

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class VoletAccountPayload extends AccountPayload {

    private final String accountNr;

    public VoletAccountPayload(String id, String paymentMethodName, String accountNr) {
        super(id, paymentMethodName);
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(accountNr, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setVoletAccountPayload(toVoletAccountPayloadProto(serializeForHash));
    }

    public static VoletAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var voletAccountPayload = proto.getVoletAccountPayload();
        return new VoletAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                voletAccountPayload.getAccountNr()
        );
    }

    private bisq.account.protobuf.VoletAccountPayload toVoletAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getVoletAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.VoletAccountPayload.Builder getVoletAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.VoletAccountPayload.newBuilder()
                .setAccountNr(accountNr);
    }
}
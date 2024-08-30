package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashByMailAccountPayload extends AccountPayload {

    private final String postalAddress;
    private final String contact;
    private final String extraInfo;

    public CashByMailAccountPayload(String id, String paymentMethodName, String postalAddress, String contact, String extraInfo) {
        super(id, paymentMethodName);
        this.postalAddress = postalAddress;
        this.contact = contact;
        this.extraInfo = extraInfo;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setCashByMailAccountPayload(toCashByMailAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.CashByMailAccountPayload toCashByMailAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCashByMailAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashByMailAccountPayload.Builder getCashByMailAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashByMailAccountPayload.newBuilder()
                .setPostalAddress(postalAddress)
                .setContact(contact)
                .setExtraInfo(extraInfo);
    }

    public static CashByMailAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cashByMailPayload = proto.getCashByMailAccountPayload();
        return new CashByMailAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                cashByMailPayload.getPostalAddress(),
                cashByMailPayload.getContact(),
                cashByMailPayload.getExtraInfo()
        );
    }
}

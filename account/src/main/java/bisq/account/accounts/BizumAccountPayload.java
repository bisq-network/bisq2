package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class BizumAccountPayload extends CountryBasedAccountPayload {

    private final String mobileNr;

    public BizumAccountPayload(String id, String paymentMethodName, String countryCode, String mobileNr) {
        super(id, paymentMethodName, countryCode);
        this.mobileNr = mobileNr;
    }

    @Override
    public AccountPayload toProto() {
        return getAccountPayloadBuilder()
                .setCountryBasedAccountPayload(
                        getCountryBasedAccountPayloadBuilder()
                                .setBizumAccountPayload(
                                        bisq.account.protobuf.BizumAccountPayload.newBuilder()
                                                .setMobileNr(mobileNr)))
                .build();
    }

    public static BizumAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        return new BizumAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                countryBasedAccountPayload.getBizumAccountPayload().getMobileNr());
    }
}

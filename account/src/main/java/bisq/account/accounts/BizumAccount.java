package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.Account;
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class BizumAccount extends CountryBasedAccount<BizumAccountPayload, FiatPaymentMethod> {

    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.BIZUM);

    public BizumAccount(String accountName, BizumAccountPayload accountPayload, Country country) {
        super(accountName, PAYMENT_METHOD, accountPayload, country);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setBizumAccount(
                toBizumAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.BizumAccount toBizumAccountProto(boolean serializeForHash) {
        return resolveBuilder(getBizumAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.BizumAccount.Builder getBizumAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BizumAccount.newBuilder();
    }

    public static BizumAccount fromProto(Account proto) {
        return new BizumAccount(
                proto.getAccountName(),
                BizumAccountPayload.fromProto(proto.getAccountPayload()),
                Country.fromProto(proto.getCountryBasedAccount().getCountry()));
    }
}

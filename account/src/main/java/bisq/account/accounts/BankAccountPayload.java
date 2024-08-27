package bisq.account.accounts;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

//fixme (low prio) use Optional instead of Nullable
@EqualsAndHashCode(callSuper = true)
@Setter
@Getter
@ToString
@Slf4j
public abstract class BankAccountPayload extends CountryBasedAccountPayload {
    protected String holderName;
    protected String bankName;
    protected String branchId;
    protected String accountNr;
    protected String accountType;
    @Nullable
    protected String holderTaxId;
    protected String bankId;
    @Nullable
    protected String nationalAccountId;

    protected BankAccountPayload(String id,
                                 String paymentMethodName,
                                 String countryCode,
                                 String holderName,
                                 @Nullable String bankName,
                                 @Nullable String branchId,
                                 @Nullable String accountNr,
                                 @Nullable String accountType,
                                 @Nullable String holderTaxId,
                                 @Nullable String bankId,
                                 @Nullable String nationalAccountId) {
        super(id, paymentMethodName, countryCode);

        this.holderName = Optional.ofNullable(holderName).orElse("");
        this.bankName = Optional.ofNullable(bankName).orElse("");
        this.branchId = Optional.ofNullable(branchId).orElse("");
        this.accountNr = Optional.ofNullable(accountNr).orElse("");
        this.accountType = Optional.ofNullable(accountType).orElse("");
        this.holderTaxId = holderTaxId;
        this.bankId = Optional.ofNullable(bankId).orElse("");
        this.nationalAccountId = nationalAccountId;
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(bankName, 100);
        NetworkDataValidation.validateText(branchId, 30);
        NetworkDataValidation.validateText(accountNr, 30);
        NetworkDataValidation.validateText(accountType, 20);
        if (holderTaxId != null) {
            NetworkDataValidation.validateText(holderTaxId, 50);
        }
        NetworkDataValidation.validateText(bankId, 50);
        if (nationalAccountId != null) {
            NetworkDataValidation.validateText(nationalAccountId, 50);
        }
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setBankAccountPayload(toBankAccountPayloadProto(serializeForHash));
    }

    protected bisq.account.protobuf.BankAccountPayload toBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        var builder = bisq.account.protobuf.BankAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setBankName(bankName)
                .setBranchId(branchId)
                .setAccountNr(accountNr)
                .setAccountType(accountType)
                .setBranchId(branchId)
                .setBankId(bankId);
        Optional.ofNullable(holderTaxId).ifPresent(builder::setHolderTaxId);
        Optional.ofNullable(nationalAccountId).ifPresent(builder::setNationalAccountId);
        return builder;
    }

    public static BankAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getCountryBasedAccountPayload().getBankAccountPayload().getMessageCase()) {
            case ACHTRANSFERACCOUNTPAYLOAD -> AchTransferAccountPayload.fromProto(proto);
            case NATIONALBANKACCOUNTPAYLOAD -> NationalBankAccountPayload.fromProto(proto);
            case CASHDEPOSITACCOUNTPAYLOAD -> CashDepositAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}

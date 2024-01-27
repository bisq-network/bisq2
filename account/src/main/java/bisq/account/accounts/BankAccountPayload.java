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

    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder() {
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
        switch (proto.getCountryBasedAccountPayload().getBankAccountPayload().getMessageCase()) {
            case ACHTRANSFERACCOUNTPAYLOAD:
                return AchTransferAccountPayload.fromProto(proto);
            case NATIONALBANKACCOUNTPAYLOAD:
                return NationalBankAccountPayload.fromProto(proto);
            case CASHDEPOSITACCOUNTPAYLOAD:
                return CashDepositAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}

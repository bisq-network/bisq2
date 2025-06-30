package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
@Slf4j
public abstract class BankAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;
    public static final int HOLDER_ID_MIN_LENGTH = 2;
    public static final int HOLDER_ID_MAX_LENGTH = 50;
    public static final int BANK_NAME_MIN_LENGTH = 2;
    public static final int BANK_NAME_MAX_LENGTH = 70;
    public static final int BANK_ID_MIN_LENGTH = 1;
    public static final int BANK_ID_MAX_LENGTH = 50;
    public static final int BRANCH_ID_MIN_LENGTH = 1;
    public static final int BRANCH_ID_MAX_LENGTH = 50;
    public static final int ACCOUNT_NR_MIN_LENGTH = 1;
    public static final int ACCOUNT_NR_MAX_LENGTH = 50;
    public static final int NATIONAL_ACCOUNT_ID_MIN_LENGTH = 1;
    public static final int NATIONAL_ACCOUNT_ID_MAX_LENGTH = 50;

    protected final String selectedCurrencyCode;
    protected final Optional<String> holderName;
    protected final Optional<String> holderId;
    protected final Optional<String> bankName;
    protected final Optional<String> bankId;
    protected final Optional<String> branchId;
    protected final String accountNr;
    protected final Optional<BankAccountType> bankAccountType;
    protected final Optional<String> nationalAccountId;

    protected BankAccountPayload(String id,
                                 String countryCode,
                                 String selectedCurrencyCode,
                                 Optional<String> holderName,
                                 Optional<String> holderId,
                                 Optional<String> bankName,
                                 Optional<String> bankId,
                                 Optional<String> branchId,
                                 String accountNr,
                                 Optional<BankAccountType> bankAccountType,
                                 Optional<String> nationalAccountId) {
        super(id, countryCode);

        this.selectedCurrencyCode = selectedCurrencyCode;
        this.holderName = holderName;
        this.holderId = holderId;
        this.bankName = bankName;
        this.bankId = bankId;
        this.branchId = branchId;
        this.accountNr = accountNr;
        this.bankAccountType = bankAccountType;
        this.nationalAccountId = nationalAccountId;
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        holderName.ifPresent(holderName -> NetworkDataValidation.validateRequiredText(holderName,
                HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH));
        holderId.ifPresent(holderId -> NetworkDataValidation.validateRequiredText(holderId,
                HOLDER_ID_MIN_LENGTH, HOLDER_ID_MAX_LENGTH));
        bankName.ifPresent(bankName -> NetworkDataValidation.validateRequiredText(bankName,
                BANK_NAME_MIN_LENGTH, BANK_NAME_MAX_LENGTH));
        bankId.ifPresent(bankId -> NetworkDataValidation.validateRequiredText(bankId,
                BANK_ID_MIN_LENGTH, BANK_ID_MAX_LENGTH));
        branchId.ifPresent(branchId -> NetworkDataValidation.validateRequiredText(branchId,
                BRANCH_ID_MIN_LENGTH, BRANCH_ID_MAX_LENGTH));
        NetworkDataValidation.validateRequiredText(accountNr,
                ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH);
        nationalAccountId.ifPresent(nationalAccountId -> NetworkDataValidation.validateRequiredText(nationalAccountId,
                NATIONAL_ACCOUNT_ID_MIN_LENGTH, NATIONAL_ACCOUNT_ID_MAX_LENGTH));
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
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setAccountNr(accountNr);
        holderName.ifPresent(builder::setHolderName);
        holderId.ifPresent(builder::setHolderId);
        bankName.ifPresent(builder::setBankName);
        bankId.ifPresent(builder::setBankId);
        branchId.ifPresent(builder::setBranchId);
        bankAccountType.ifPresent(type -> builder.setBankAccountType(type.toProtoEnum()));
        nationalAccountId.ifPresent(builder::setNationalAccountId);
        return builder;
    }

    public static BankAccountPayload fromProto(AccountPayload proto) {
        return switch (proto.getCountryBasedAccountPayload().getBankAccountPayload().getMessageCase()) {
            case ACHTRANSFERACCOUNTPAYLOAD -> AchTransferAccountPayload.fromProto(proto);
            case NATIONALBANKACCOUNTPAYLOAD -> NationalBankAccountPayload.fromProto(proto);
            case CASHDEPOSITACCOUNTPAYLOAD -> CashDepositAccountPayload.fromProto(proto);
            case SAMEBANKACCOUNTPAYLOAD -> SameBankAccountPayload.fromProto(proto);
            case DOMESTICWIRETRANSFERACCOUNTPAYLOAD -> DomesticWireTransferAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}

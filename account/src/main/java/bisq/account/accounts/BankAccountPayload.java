package bisq.account.accounts;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.normalize;
import static bisq.common.validation.NetworkDataValidation.validateText;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
@Slf4j
public abstract class BankAccountPayload extends CountryBasedAccountPayload {

    protected Optional<String> holderName;
    protected Optional<String> bankName;
    protected Optional<String> branchId;
    protected Optional<String> accountNr;
    protected Optional<String> accountType;
    protected Optional<String> holderTaxId;
    protected Optional<String> bankId;
    protected Optional<String> nationalAccountId;

    protected BankAccountPayload(String id,
                                 String paymentMethodName,
                                 String countryCode,
                                 Optional<String> holderName,
                                 Optional<String> bankName,
                                 Optional<String> branchId,
                                 Optional<String> accountNr,
                                 Optional<String> accountType,
                                 Optional<String> holderTaxId,
                                 Optional<String> bankId,
                                 Optional<String> nationalAccountId) {
        super(id, paymentMethodName, countryCode);

        this.holderName = normalize(holderName);
        this.bankName = normalize(bankName);
        this.branchId = normalize(branchId);
        this.accountNr = normalize(accountNr);
        this.accountType = normalize(accountType);
        this.holderTaxId = normalize(holderTaxId);
        this.bankId = normalize(bankId);
        this.nationalAccountId = normalize(nationalAccountId);
    }

    @Override
    public void verify() {
        super.verify();
        validateText(holderName, 100);
        validateText(bankName, 100);
        validateText(branchId, 30);
        validateText(accountNr, 30);
        validateText(accountType, 20);
        validateText(holderTaxId, 50);
        validateText(bankId, 50);
        validateText(nationalAccountId, 50);
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
        var builder = bisq.account.protobuf.BankAccountPayload.newBuilder();
        holderName.ifPresent(builder::setHolderName);
        bankName.ifPresent(builder::setBankName);
        branchId.ifPresent(builder::setBranchId);
        accountNr.ifPresent(builder::setAccountNr);
        accountType.ifPresent(builder::setAccountType);
        holderTaxId.ifPresent(builder::setHolderTaxId);
        bankId.ifPresent(builder::setBankId);
        nationalAccountId.ifPresent(builder::setNationalAccountId);
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

package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashDepositAccountPayload extends BankAccountPayload {

    private final String requirements;

    public CashDepositAccountPayload(String id, String paymentMethodName, String countryCode,
                                     String holderName, String bankName, String branchId,
                                     String accountNr, String accountType,
                                     String holderTaxId, String bankId,
                                     String nationalAccountId, String requirements) {
        super(id, paymentMethodName, countryCode,
                holderName, bankName, branchId,
                accountNr, accountType, holderTaxId,
                bankId, nationalAccountId);
        this.requirements = requirements;

        verify();
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateText(requirements, 500);
    }

    @Override
    public AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation).setCountryBasedAccountPayload(
                getCountryBasedAccountPayloadBuilder(ignoreAnnotation).setBankAccountPayload(
                        getBankAccountPayloadBuilder(ignoreAnnotation)
                                .setCashDepositAccountPayload(
                                        bisq.account.protobuf.CashDepositAccountPayload.newBuilder()
                                                .setRequirements(requirements)
                                )
                )
        );
    }

    public static CashDepositAccountPayload fromProto(AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new CashDepositAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getHolderName(),
                bankAccountPayload.getBankName(),
                bankAccountPayload.getBranchId(),
                bankAccountPayload.getAccountNr(),
                bankAccountPayload.getAccountType(),
                bankAccountPayload.getHolderTaxId(),
                bankAccountPayload.getBankId(),
                bankAccountPayload.getNationalAccountId(),
                bankAccountPayload.getCashDepositAccountPayload().getRequirements());
    }
}

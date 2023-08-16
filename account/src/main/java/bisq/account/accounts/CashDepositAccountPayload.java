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
public final class CashDepositAccountPayload extends BankAccountPayload {

    private final String requirements;

    protected CashDepositAccountPayload(String id, String paymentMethodName, String countryCode,
                                         String holderName, String bankName, String branchId,
                                         String accountNr, String accountType,
                                         String holderTaxId, String bankId,
                                         String nationalAccountId, String requirements) {
        super(id, paymentMethodName, countryCode,
                holderName, bankName, branchId,
                accountNr, accountType, holderTaxId,
                bankId, nationalAccountId);
        this.requirements = requirements;
    }

    @Override
    public AccountPayload toProto() {
        return getAccountPayloadBuilder().setCountryBasedAccountPayload(
                getCountryBasedAccountPayloadBuilder().setBankAccountPayload(
                        getBankAccountPayloadBuilder()
                                .setCashDepositAccountPayload(
                                        bisq.account.protobuf.CashDepositAccountPayload.newBuilder()
                                                .setRequirements(requirements)
                                                .build()
                                )
                )
        ).build();
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

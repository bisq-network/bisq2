package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class CashDepositAccountPayload extends BankAccountPayload {
    public static final int REQUIREMENTS_MAX_LENGTH = 150;
    private final Optional<String> requirements;

    public CashDepositAccountPayload(String id,
                                     String countryCode,
                                     String selectedCurrencyCode,
                                     String holderName,
                                     Optional<String> holderTaxId,
                                     String bankName,
                                     Optional<String> bankId,
                                     Optional<String> branchId,
                                     String accountNr,
                                     Optional<BankAccountType> bankAccountType,
                                     Optional<String> nationalAccountId,
                                     Optional<String> requirements) {
        super(id,
                countryCode,
                selectedCurrencyCode,
                Optional.of(holderName),
                holderTaxId,
                Optional.of(bankName),
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);
        this.requirements = requirements;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateText(requirements, REQUIREMENTS_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setCashDepositAccountPayload(
                toCashDepositAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.CashDepositAccountPayload toCashDepositAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCashDepositAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashDepositAccountPayload.Builder getCashDepositAccountPayloadBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CashDepositAccountPayload.Builder builder = bisq.account.protobuf.CashDepositAccountPayload.newBuilder();
        requirements.ifPresent(builder::setRequirements);
        return builder;
    }

    public static CashDepositAccountPayload fromProto(AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var cashDepositAccountPayload = bankAccountPayload.getCashDepositAccountPayload();
        checkArgument(bankAccountPayload.hasBankName(), "Bank name for Cash Deposit must be present");
        return new CashDepositAccountPayload(proto.getId(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getSelectedCurrencyCode(),
                bankAccountPayload.getHolderName(),
                bankAccountPayload.hasHolderId() ? Optional.of(bankAccountPayload.getHolderId()) : Optional.empty(),
                bankAccountPayload.getBankName(),
                bankAccountPayload.hasBankId() ? Optional.of(bankAccountPayload.getBankId()) : Optional.empty(),
                bankAccountPayload.hasBranchId() ? Optional.of(bankAccountPayload.getBranchId()) : Optional.empty(),
                bankAccountPayload.getAccountNr(),
                bankAccountPayload.hasBankAccountType() ? Optional.of(BankAccountType.fromProto(bankAccountPayload.getBankAccountType())) : Optional.empty(),
                bankAccountPayload.hasNationalAccountId() ? Optional.of(bankAccountPayload.getNationalAccountId()) : Optional.empty(),
                cashDepositAccountPayload.hasRequirements() ? Optional.of(cashDepositAccountPayload.getRequirements()) : Optional.empty());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.CASH_DEPOSIT);
    }
}

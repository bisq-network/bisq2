package bisq.account.accounts;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class NationalBankAccountPayload extends BankAccountPayload implements SelectableCurrencyAccountPayload {
    public NationalBankAccountPayload(String id,
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
        super(id,
                countryCode,
                selectedCurrencyCode,
                holderName,
                holderId,
                bankName,
                bankId,
                branchId,
                accountNr,
                bankAccountType,
                nationalAccountId);
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setNationalBankAccountPayload(
                toNationalBankAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.NationalBankAccountPayload toNationalBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getNationalBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.NationalBankAccountPayload.Builder getNationalBankAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.NationalBankAccountPayload.newBuilder();
    }

    public static NationalBankAccountPayload fromProto(AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new NationalBankAccountPayload(
                proto.getId(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getSelectedCurrencyCode(),
                bankAccountPayload.hasHolderName() ? Optional.of(bankAccountPayload.getHolderName()) : Optional.empty(),
                bankAccountPayload.hasHolderId() ? Optional.of(bankAccountPayload.getHolderId()) : Optional.empty(),
                bankAccountPayload.hasBankName() ? Optional.of(bankAccountPayload.getBankName()) : Optional.empty(),
                bankAccountPayload.hasBankId() ? Optional.of(bankAccountPayload.getBankId()) : Optional.empty(),
                bankAccountPayload.hasBranchId() ? Optional.of(bankAccountPayload.getBranchId()) : Optional.empty(),
                bankAccountPayload.getAccountNr(),
                bankAccountPayload.hasBankAccountType() ? Optional.of(BankAccountType.fromProto(bankAccountPayload.getBankAccountType())) : Optional.empty(),
                bankAccountPayload.hasNationalAccountId() ? Optional.of(bankAccountPayload.getNationalAccountId()) : Optional.empty());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
    }

    @Override
    public String getAccountDataDisplayString() {
        AccountDataDisplayStringBuilder builder = new AccountDataDisplayStringBuilder();
        holderName.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.holderName"), value));
        holderId.ifPresent(value -> builder.add(BankAccountUtils.getHolderIdDescription(countryCode), value));
        bankName.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.bank.bankName"), value));
        bankId.ifPresent(value -> builder.add(BankAccountUtils.getBankIdDescription(countryCode), value));
        branchId.ifPresent(value -> builder.add(BankAccountUtils.getBranchIdDescription(countryCode), value));
        builder.add(BankAccountUtils.getAccountNrDescription(countryCode), accountNr);
        bankAccountType.ifPresent(value -> builder.add(Res.get("user.paymentAccounts.bank.bankAccountType"),
                Res.get("user.paymentAccounts.bank.bankAccountType." + value.name())));
        nationalAccountId.ifPresent(value -> builder.add(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
        return builder.toString();
    }
}

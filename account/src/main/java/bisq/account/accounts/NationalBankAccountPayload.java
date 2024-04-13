package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class NationalBankAccountPayload extends BankAccountPayload {

    protected NationalBankAccountPayload(String id, String paymentMethodName, String countryCode,
                                         String holderName, @Nullable String bankName, @Nullable String branchId,
                                         @Nullable String accountNr, @Nullable String accountType,
                                         @Nullable String holderTaxId, @Nullable String bankId,
                                         @Nullable String nationalAccountId) {
        super(id, paymentMethodName, countryCode,
                holderName, bankName, branchId,
                accountNr, accountType, holderTaxId,
                bankId, nationalAccountId);
    }

    @Override
    public AccountPayload.Builder getBuilder(boolean ignoreAnnotation) {
        return getAccountPayloadBuilder(ignoreAnnotation).setCountryBasedAccountPayload(
                getCountryBasedAccountPayloadBuilder(ignoreAnnotation).setBankAccountPayload(
                        getBankAccountPayloadBuilder(ignoreAnnotation).setNationalBankAccountPayload(
                                bisq.account.protobuf.NationalBankAccountPayload.newBuilder()
                        )
                )
        );
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static NationalBankAccountPayload fromProto(AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new NationalBankAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getHolderName(),
                bankAccountPayload.getBankName().isEmpty() ? null : bankAccountPayload.getBankName(),
                bankAccountPayload.getBranchId().isEmpty() ? null : bankAccountPayload.getBranchId(),
                bankAccountPayload.getAccountNr().isEmpty() ? null : bankAccountPayload.getAccountNr(),
                bankAccountPayload.getAccountType().isEmpty() ? null : bankAccountPayload.getAccountType(),
                bankAccountPayload.getHolderTaxId().isEmpty() ? null : bankAccountPayload.getHolderTaxId(),
                bankAccountPayload.getBankId().isEmpty() ? null : bankAccountPayload.getBankId(),
                bankAccountPayload.getNationalAccountId().isEmpty() ? null : bankAccountPayload.getNationalAccountId());
    }
}

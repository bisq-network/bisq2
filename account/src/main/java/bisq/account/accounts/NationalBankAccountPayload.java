package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.toOptional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public class NationalBankAccountPayload extends BankAccountPayload {

    protected NationalBankAccountPayload(String id,
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
        super(id, paymentMethodName, countryCode,
                holderName, bankName, branchId,
                accountNr, accountType, holderTaxId,
                bankId, nationalAccountId);
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
                proto.getPaymentMethodName(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                toOptional(bankAccountPayload.getHolderName()),
                toOptional(bankAccountPayload.getBankName()),
                toOptional(bankAccountPayload.getBranchId()),
                toOptional(bankAccountPayload.getAccountNr()),
                toOptional(bankAccountPayload.getAccountType()),
                toOptional(bankAccountPayload.getHolderTaxId()),
                toOptional(bankAccountPayload.getBankId()),
                toOptional(bankAccountPayload.getNationalAccountId()));
    }
}

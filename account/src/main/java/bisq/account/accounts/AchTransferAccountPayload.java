package bisq.account.accounts;

import bisq.account.protobuf.AccountPayload;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccountPayload extends BankAccountPayload {

    private final String holderAddress;

    public AchTransferAccountPayload(String paymentMethodName,
                                     String id,
                                     String countryCode,
                                     String holderName,
                                     String bankName,
                                     String branchId,
                                     String accountNr,
                                     String accountType,
                                     String holderAddress) {
        super(paymentMethodName,
                id,
                countryCode,
                holderName,
                bankName,
                branchId,
                accountNr,
                accountType,
                null,
                null,
                null);
        this.holderAddress = Optional.ofNullable(holderAddress).orElse("");
    }

    @Override
    public AccountPayload toProto() {
        var builder = bisq.account.protobuf.AchTransferAccountPayload.newBuilder().setHolderAddress(holderAddress);
        var bankAccountPayloadBuilder = getAccountPayloadBuilder()
                .getCountryBasedAccountPayloadBuilder()
                .getBankAccountPayloadBuilder()
                .setAchTransferAccountPayload(builder);
        var countryBasedPaymentAccountPayloadBuilder = getAccountPayloadBuilder()
                .getCountryBasedAccountPayloadBuilder()
                .setBankAccountPayload(bankAccountPayloadBuilder);
        return getAccountPayloadBuilder()
                .setCountryBasedAccountPayload(countryBasedPaymentAccountPayloadBuilder)
                .build();
    }

    public static AchTransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var accountPayload = bankAccountPayload.getAchTransferAccountPayload();
        return new AchTransferAccountPayload(
                proto.getPaymentMethodName(),
                proto.getId(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                bankAccountPayload.getHolderName(),
                bankAccountPayload.getBankName().isEmpty() ? null : bankAccountPayload.getBankName(),
                bankAccountPayload.getBranchId().isEmpty() ? null : bankAccountPayload.getBranchId(),
                bankAccountPayload.getAccountNr().isEmpty() ? null : bankAccountPayload.getAccountNr(),
                bankAccountPayload.getAccountType().isEmpty() ? null : bankAccountPayload.getAccountType(),
                accountPayload.getHolderAddress().isEmpty() ? null : accountPayload.getHolderAddress());
    }
}

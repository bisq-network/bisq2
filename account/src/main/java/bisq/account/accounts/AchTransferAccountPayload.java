package bisq.account.accounts;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.*;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccountPayload extends BankAccountPayload {

    private final Optional<String> holderAddress;

    public AchTransferAccountPayload(String id,
                                     String paymentMethodName,
                                     String countryCode,
                                     Optional<String> holderName,
                                     Optional<String> bankName,
                                     Optional<String> branchId,
                                     Optional<String> accountNr,
                                     Optional<String> accountType,
                                     Optional<String> holderAddress) {
        super(
                id,
                paymentMethodName,
                countryCode,
                holderName,
                bankName,
                branchId,
                accountNr,
                accountType,
                null,
                null,
                null);
        this.holderAddress = normalize(holderAddress);
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash).setAchTransferAccountPayload(
                toAchTransferAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.AchTransferAccountPayload toAchTransferAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getAchTransferAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.AchTransferAccountPayload.Builder getAchTransferAccountPayloadBuilder(boolean serializeForHash) {
        var builder = bisq.account.protobuf.AchTransferAccountPayload.newBuilder();
        this.holderAddress.ifPresent(builder::setHolderAddress);
        return builder;
    }

    public static AchTransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var accountPayload = bankAccountPayload.getAchTransferAccountPayload();
        return new AchTransferAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedPaymentAccountPayload.getCountryCode(),
                toOptional(bankAccountPayload.getHolderName()),
                toOptional(bankAccountPayload.getBankName()),
                toOptional(bankAccountPayload.getBranchId()),
                toOptional(bankAccountPayload.getAccountNr()),
                toOptional(bankAccountPayload.getAccountType()),
                toOptional(accountPayload.getHolderAddress()));
    }
}
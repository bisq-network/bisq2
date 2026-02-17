/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.account.accounts.fiat;

import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.accounts.util.BankAccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public final class AchTransferAccountPayload extends BankAccountPayload {
    public static final int HOLDER_ADDRESS_MIN_LENGTH = 5;
    public static final int HOLDER_ADDRESS_MAX_LENGTH = 150;

    private final String holderAddress;

    public AchTransferAccountPayload(String id,
                                     String holderName,
                                     String holderAddress,
                                     String bankName,
                                     String routingNr,
                                     String accountNr,
                                     BankAccountType bankAccountType) {
        this(id,
                AccountUtils.generateSalt(),
                holderName,
                holderAddress,
                bankName,
                routingNr,
                accountNr,
                bankAccountType);
    }

    public AchTransferAccountPayload(String id,
                                     byte[] salt,
                                     String holderName,
                                     String holderAddress,
                                     String bankName,
                                     String routingNr,
                                     String accountNr,
                                     BankAccountType bankAccountType) {
        super(id,
                salt,
                "US",
                "USD",
                Optional.of(holderName),
                Optional.empty(),
                Optional.of(bankName),
                Optional.of(routingNr),
                Optional.empty(),
                accountNr,
                Optional.of(bankAccountType),
                Optional.empty());
        this.holderAddress = holderAddress;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderAddress, HOLDER_ADDRESS_MIN_LENGTH, HOLDER_ADDRESS_MAX_LENGTH);
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
        return bisq.account.protobuf.AchTransferAccountPayload.newBuilder()
                .setHolderAddress(holderAddress);
    }

    public static AchTransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var achAccountPayload = bankAccountPayload.getAchTransferAccountPayload();
        checkArgument(bankAccountPayload.hasBankName(), "Bank name for ACH must be present");
        checkArgument(bankAccountPayload.hasBankId(), "BankId (Routing number) for ACH must be present");
        checkArgument(bankAccountPayload.hasBankAccountType(), "AccountType for ACH must be present");
        return new AchTransferAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                bankAccountPayload.getHolderName(),
                achAccountPayload.getHolderAddress(),
                bankAccountPayload.getBankName(),
                bankAccountPayload.getBankId(),
                bankAccountPayload.getAccountNr(),
                BankAccountType.fromProto(bankAccountPayload.getBankAccountType()));
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
    }

    @Override
    public String getAccountDataDisplayString() {
        AccountDataDisplayStringBuilder builder = new AccountDataDisplayStringBuilder();
        holderName.ifPresent(value -> builder.add(Res.get("paymentAccounts.holderName"), value));
        holderId.ifPresent(value -> builder.add(BankAccountUtils.getHolderIdDescription(countryCode), value));
        builder.add(Res.get("paymentAccounts.holderAddress"), holderAddress);
        bankName.ifPresent(value -> builder.add(Res.get("paymentAccounts.bank.bankName"), value));
        bankId.ifPresent(value -> builder.add(BankAccountUtils.getBankIdDescription(countryCode), value));
        branchId.ifPresent(value -> builder.add(BankAccountUtils.getBranchIdDescription(countryCode), value));
        builder.add(BankAccountUtils.getAccountNrDescription(countryCode), accountNr);
        bankAccountType.ifPresent(value -> builder.add(Res.get("paymentAccounts.bank.bankAccountType"),
                Res.get("paymentAccounts.bank.bankAccountType." + value.name())));
        nationalAccountId.ifPresent(value -> builder.add(BankAccountUtils.getNationalAccountIdDescription(countryCode), value));
        return builder.toString();
    }
}

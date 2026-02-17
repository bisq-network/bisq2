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
public final class DomesticWireTransferAccountPayload extends BankAccountPayload {
    public static final int HOLDER_ADDRESS_MIN_LENGTH = 5;
    public static final int HOLDER_ADDRESS_MAX_LENGTH = 150;

    private final String holderAddress;

    public DomesticWireTransferAccountPayload(String id,
                                              String holderName,
                                              String holderAddress,
                                              String bankName,
                                              String routingNr,
                                              String accountNr) {
        this(id,
                AccountUtils.generateSalt(),
                holderName,
                holderAddress,
                bankName,
                routingNr,
                accountNr);
    }

    public DomesticWireTransferAccountPayload(String id,
                                               byte[] salt,
                                               String holderName,
                                               String holderAddress,
                                               String bankName,
                                               String routingNr,
                                               String accountNr) {
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
                Optional.empty(),
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
        return super.getBankAccountPayloadBuilder(serializeForHash).setDomesticWireTransferAccountPayload(
                toDomesticWireTransferAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.DomesticWireTransferAccountPayload toDomesticWireTransferAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getDomesticWireTransferAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.DomesticWireTransferAccountPayload.Builder getDomesticWireTransferAccountPayloadBuilder(
            boolean serializeForHash) {
        return bisq.account.protobuf.DomesticWireTransferAccountPayload.newBuilder()
                .setHolderAddress(holderAddress);
    }

    public static DomesticWireTransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var domesticWireAccountPayload = bankAccountPayload.getDomesticWireTransferAccountPayload();
        checkArgument(bankAccountPayload.hasBankName(), "Bank name for DomesticWireTransfer must be present");
        checkArgument(bankAccountPayload.hasBankId(), "BankId (Routing number) for DomesticWireTransfer must be present");
        return new DomesticWireTransferAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                bankAccountPayload.getHolderName(),
                domesticWireAccountPayload.getHolderAddress(),
                bankAccountPayload.getBankName(),
                bankAccountPayload.getBankId(),
                bankAccountPayload.getAccountNr());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.DOMESTIC_WIRE_TRANSFER);
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

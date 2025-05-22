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

package bisq.account.accounts;

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.normalize;
import static bisq.common.util.OptionalUtils.toOptional;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public final class DomesticWireTransferAccountPayload extends BankAccountPayload {

    private final Optional<String> holderAddress;

    public DomesticWireTransferAccountPayload(String id,
                                              String paymentMethodName,
                                              String countryCode,
                                              Optional<String> holderName,
                                              Optional<String> bankName,
                                              Optional<String> branchId,
                                              Optional<String> accountNr,
                                              Optional<String> accountType,
                                              Optional<String> holderTaxId,
                                              Optional<String> bankId,
                                              Optional<String> nationalAccountId,
                                              Optional<String> holderAddress) {
        super(id, paymentMethodName, countryCode,
                holderName, bankName, branchId,
                accountNr, accountType, holderTaxId,
                bankId, nationalAccountId);
        this.holderAddress = normalize(holderAddress);
        verify();
    }

    @Override
    public void verify() {
        super.verify();
        holderAddress.ifPresent(address -> NetworkDataValidation.validateRequiredText(address, 100));
    }

    public static DomesticWireTransferAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        var domesticWireTransferPayload = bankAccountPayload.getDomesticWireTransferAccountPayload();
        return new DomesticWireTransferAccountPayload(
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
                toOptional(bankAccountPayload.getNationalAccountId()),
                toOptional(domesticWireTransferPayload.getHolderAddress()));
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
        var builder = bisq.account.protobuf.DomesticWireTransferAccountPayload.newBuilder();
        holderAddress.ifPresent(builder::setHolderAddress);
        return builder;
    }
}
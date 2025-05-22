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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.toOptional;

@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SameBankAccountPayload extends NationalBankAccountPayload {

    public SameBankAccountPayload(String id,
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
        verify();
    }

    @Override
    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        return super.getBankAccountPayloadBuilder(serializeForHash)
                .setSameBankAccountPayload(buildSameBankAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SameBankAccountPayload buildSameBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSameBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SameBankAccountPayload.Builder getSameBankAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SameBankAccountPayload.newBuilder();
    }

    public static SameBankAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedPaymentAccountPayload = proto.getCountryBasedAccountPayload();
        var bankAccountPayload = countryBasedPaymentAccountPayload.getBankAccountPayload();
        return new SameBankAccountPayload(
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
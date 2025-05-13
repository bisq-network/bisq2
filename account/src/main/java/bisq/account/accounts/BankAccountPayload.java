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

import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.common.util.OptionalUtils.normalize;
import static bisq.common.validation.NetworkDataValidation.validateText;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString
@Slf4j
public abstract class BankAccountPayload extends CountryBasedAccountPayload {

    protected Optional<String> holderName;
    protected Optional<String> bankName;
    protected Optional<String> branchId;
    protected Optional<String> accountNr;
    protected Optional<String> accountType;
    protected Optional<String> holderTaxId;
    protected Optional<String> bankId;
    protected Optional<String> nationalAccountId;

    protected BankAccountPayload(String id,
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
        super(id, paymentMethodName, countryCode);

        this.holderName = normalize(holderName);
        this.bankName = normalize(bankName);
        this.branchId = normalize(branchId);
        this.accountNr = normalize(accountNr);
        this.accountType = normalize(accountType);
        this.holderTaxId = normalize(holderTaxId);
        this.bankId = normalize(bankId);
        this.nationalAccountId = normalize(nationalAccountId);
    }

    @Override
    public void verify() {
        super.verify();
        validateText(holderName, 100);
        validateText(bankName, 100);
        validateText(branchId, 30);
        validateText(accountNr, 30);
        validateText(accountType, 20);
        validateText(holderTaxId, 50);
        validateText(bankId, 50);
        validateText(nationalAccountId, 50);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setBankAccountPayload(toBankAccountPayloadProto(serializeForHash));
    }

    protected bisq.account.protobuf.BankAccountPayload toBankAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getBankAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.BankAccountPayload.Builder getBankAccountPayloadBuilder(boolean serializeForHash) {
        var builder = bisq.account.protobuf.BankAccountPayload.newBuilder();
        holderName.ifPresent(builder::setHolderName);
        bankName.ifPresent(builder::setBankName);
        branchId.ifPresent(builder::setBranchId);
        accountNr.ifPresent(builder::setAccountNr);
        accountType.ifPresent(builder::setAccountType);
        holderTaxId.ifPresent(builder::setHolderTaxId);
        bankId.ifPresent(builder::setBankId);
        nationalAccountId.ifPresent(builder::setNationalAccountId);
        return builder;
    }

    public static BankAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getCountryBasedAccountPayload().getBankAccountPayload().getMessageCase()) {
            case ACHTRANSFERACCOUNTPAYLOAD -> AchTransferAccountPayload.fromProto(proto);
            case NATIONALBANKACCOUNTPAYLOAD -> NationalBankAccountPayload.fromProto(proto);
            case CASHDEPOSITACCOUNTPAYLOAD -> CashDepositAccountPayload.fromProto(proto);
            case SAMEBANKACCOUNTPAYLOAD -> SameBankAccountPayload.fromProto(proto);
            case SPECIFICBANKSACCOUNTPAYLOAD -> SpecificBanksAccountPayload.fromProto(proto);
            case DOMESTICWIRETRANSFERACCOUNTPAYLOAD -> DomesticWireTransferAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}

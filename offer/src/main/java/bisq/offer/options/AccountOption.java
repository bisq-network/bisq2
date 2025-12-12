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

package bisq.offer.options;

import bisq.account.accounts.fiat.BankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
@EqualsAndHashCode
public final class AccountOption implements OfferOption {
    private final PaymentMethod<? extends PaymentRail> paymentMethod;
    private final String saltedAccountId;
    private final Optional<String> countryCode;
    private final List<String> acceptedCountryCodes;
    private final Optional<String> bankId;
    private final List<String> acceptedBanks;

    public AccountOption(PaymentMethod<? extends PaymentRail> paymentMethod,
                         String saltedAccountId,
                         Optional<String> countryCode,
                         List<String> acceptedCountryCodes,
                         Optional<String> bankId,
                         List<String> acceptedBanks) {
        this.paymentMethod = paymentMethod;
        this.saltedAccountId = saltedAccountId;
        this.countryCode = countryCode;
        this.acceptedCountryCodes = acceptedCountryCodes;
        this.bankId = bankId;
        this.acceptedBanks = acceptedBanks;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHashAsHex(saltedAccountId);
        countryCode.ifPresent(NetworkDataValidation::validateCode);
        checkArgument(acceptedCountryCodes.size() < 100, "acceptedCountryCodes must be < 100 items");
        checkArgument(acceptedCountryCodes.toString().length() < 1000, "Length of acceptedCountryCodes must be < 1000");
        bankId.ifPresent(e -> NetworkDataValidation.validateText(e, BankAccountPayload.BANK_ID_MIN_LENGTH, BankAccountPayload.BANK_ID_MAX_LENGTH));
        checkArgument(acceptedBanks.size() < 10, "acceptedBanks must be < 100 items");
        checkArgument(acceptedBanks.toString().length() < 500, "Length of acceptedBanks must be < 500");
    }

    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        bisq.offer.protobuf.AccountOption.Builder builder = bisq.offer.protobuf.AccountOption.newBuilder()
                .setPaymentMethod(paymentMethod.toProto(serializeForHash))
                .setSaltedAccountId(saltedAccountId)
                .addAllAcceptedCountryCodes(acceptedCountryCodes)
                .addAllAcceptedBanks(acceptedBanks);
        countryCode.ifPresent(builder::setCountryCode);
        bankId.ifPresent(builder::setBankId);
        return getOfferOptionBuilder(serializeForHash).setAccountOption(builder);
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AccountOption fromProto(bisq.offer.protobuf.AccountOption proto) {
        return new AccountOption(PaymentMethod.fromProto(proto.getPaymentMethod()),
                proto.getSaltedAccountId(),
                proto.hasCountryCode() ? Optional.of(proto.getCountryCode()) : Optional.empty(),
                proto.getAcceptedCountryCodesList(),
                proto.hasBankId() ? Optional.of(proto.getBankId()) : Optional.empty(),
                proto.getAcceptedBanksList()
        );
    }
}
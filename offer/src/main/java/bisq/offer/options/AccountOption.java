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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@ToString
public final class AccountOption implements OfferOption {
    private final PaymentMethod<? extends PaymentRail> paymentMethod;
    private final String saltedAccountId;
    private final Optional<String> countryCode;
    private final List<String> acceptedCountryCodes;
    private final Optional<String> bankId;
    private final List<String> acceptedBanks;
    // Nodes of v2.1.13 do not know this field. They drop it, and they compute the hash serialization of an offer
    // without it. So the field is not part of the hash serialization (see "Adding a field to a DistributedData type"
    // in docs/specifications/network/distributed-data-payload-bytes.md), and not part of hashCode, by which Offer
    // sorts its options. Otherwise a node of v2.1.13 computes another signatureV1 preimage and proof of work payload
    // for an offer with this option. It rejects the offer, which a node of this version stores and serves in every
    // inventory response, and it closes the connection to a node of this version which relays the offer.
    // Signature V2 signs the kept payload bytes, so it covers the field, and equals compares it, so that a maker
    // detects a changed hash in the offer of a take offer request.
    private final byte[] saltedAccountPayloadHash;

    public AccountOption(PaymentMethod<? extends PaymentRail> paymentMethod,
                         String saltedAccountId,
                         Optional<String> countryCode,
                         List<String> acceptedCountryCodes,
                         Optional<String> bankId,
                         List<String> acceptedBanks,
                         byte[] saltedAccountPayloadHash) {
        this.paymentMethod = paymentMethod;
        this.saltedAccountId = saltedAccountId;
        this.countryCode = countryCode;
        this.acceptedCountryCodes = acceptedCountryCodes;
        this.bankId = bankId;
        this.acceptedBanks = acceptedBanks;
        this.saltedAccountPayloadHash = saltedAccountPayloadHash.clone();

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
        NetworkDataValidation.validateHash(saltedAccountPayloadHash);
    }

    public bisq.offer.protobuf.OfferOption.Builder getBuilder(boolean serializeForHash) {
        bisq.offer.protobuf.AccountOption.Builder builder = bisq.offer.protobuf.AccountOption.newBuilder()
                .setPaymentMethod(paymentMethod.toProto(serializeForHash))
                .setSaltedAccountId(saltedAccountId)
                .addAllAcceptedCountryCodes(acceptedCountryCodes)
                .addAllAcceptedBanks(acceptedBanks);
        if (!serializeForHash) {
            builder.setSaltedAccountPayloadHash(com.google.protobuf.ByteString.copyFrom(saltedAccountPayloadHash));
        }
        countryCode.ifPresent(builder::setCountryCode);
        bankId.ifPresent(builder::setBankId);
        return getOfferOptionBuilder(serializeForHash).setAccountOption(builder);
    }

    @Override
    public bisq.offer.protobuf.OfferOption toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountOption other)) {
            return false;
        }
        return Objects.equals(paymentMethod, other.paymentMethod) &&
                Objects.equals(saltedAccountId, other.saltedAccountId) &&
                Objects.equals(countryCode, other.countryCode) &&
                Objects.equals(acceptedCountryCodes, other.acceptedCountryCodes) &&
                Objects.equals(bankId, other.bankId) &&
                Objects.equals(acceptedBanks, other.acceptedBanks) &&
                Arrays.equals(saltedAccountPayloadHash, other.saltedAccountPayloadHash);
    }

    // The value which Lombok's @EqualsAndHashCode computes for the fields of v2.1.13, as nodes of v2.1.13 do. It
    // leaves out saltedAccountPayloadHash (see the comment at the field). Equal options still have equal hash codes.
    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + hashCodeOf(paymentMethod);
        result = result * 59 + hashCodeOf(saltedAccountId);
        result = result * 59 + hashCodeOf(countryCode);
        result = result * 59 + hashCodeOf(acceptedCountryCodes);
        result = result * 59 + hashCodeOf(bankId);
        result = result * 59 + hashCodeOf(acceptedBanks);
        return result;
    }

    private static int hashCodeOf(Object value) {
        return value == null ? 43 : value.hashCode();
    }

    public static AccountOption fromProto(bisq.offer.protobuf.AccountOption proto)
            throws UnresolvableProtobufException {
        try {
            return new AccountOption(PaymentMethod.fromProto(proto.getPaymentMethod()),
                    proto.getSaltedAccountId(),
                    proto.hasCountryCode() ? Optional.of(proto.getCountryCode()) : Optional.empty(),
                    proto.getAcceptedCountryCodesList(),
                    proto.hasBankId() ? Optional.of(proto.getBankId()) : Optional.empty(),
                    proto.getAcceptedBanksList(),
                    proto.getSaltedAccountPayloadHash().toByteArray()
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public byte[] getSaltedAccountPayloadHash() {
        return saltedAccountPayloadHash.clone();
    }
}
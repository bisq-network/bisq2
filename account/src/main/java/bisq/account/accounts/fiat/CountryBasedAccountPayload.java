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

import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class CountryBasedAccountPayload extends AccountPayload<FiatPaymentMethod> {
    protected final String countryCode;

    public CountryBasedAccountPayload(String id, String countryCode, String paymentMethodId, byte[] salt) {
        super(id, paymentMethodId, salt);
        this.countryCode = countryCode;
    }

    public CountryBasedAccountPayload(String id, String countryCode) {
        super(id);
        this.countryCode = countryCode;
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateCode(countryCode);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setCountryBasedAccountPayload(toCountryBasedAccountPayloadProto(serializeForHash));
    }

    public static CountryBasedAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getCountryBasedAccountPayload().getMessageCase()) {
            case BANKACCOUNTPAYLOAD -> BankAccountPayload.fromProto(proto);
            case SEPAACCOUNTPAYLOAD -> SepaAccountPayload.fromProto(proto);
            case SEPAINSTANTACCOUNTPAYLOAD -> SepaInstantAccountPayload.fromProto(proto);
            case WISEACCOUNTPAYLOAD -> WiseAccountPayload.fromProto(proto);
            case F2FACCOUNTPAYLOAD -> F2FAccountPayload.fromProto(proto);
            case PIXACCOUNTPAYLOAD -> PixAccountPayload.fromProto(proto);
            case STRIKEACCOUNTPAYLOAD -> StrikeAccountPayload.fromProto(proto);
            case AMAZONGIFTCARDACCOUNTPAYLOAD -> AmazonGiftCardAccountPayload.fromProto(proto);
            case UPIACCOUNTPAYLOAD -> UpiAccountPayload.fromProto(proto);
            case BIZUMACCOUNTPAYLOAD -> BizumAccountPayload.fromProto(proto);
            case WISEUSDACCOUNTPAYLOAD -> WiseUsdAccountPayload.fromProto(proto);
            case MONEYBEAMACCOUNTPAYLOAD -> MoneyBeamAccountPayload.fromProto(proto);
            case SWISHACCOUNTPAYLOAD -> SwishAccountPayload.fromProto(proto);
            case UPHOLDACCOUNTPAYLOAD -> UpholdAccountPayload.fromProto(proto);
            case MONEYGRAMACCOUNTPAYLOAD -> MoneyGramAccountPayload.fromProto(proto);
            case PROMPTPAYACCOUNTPAYLOAD -> PromptPayAccountPayload.fromProto(proto);
            case HALCASHACCOUNTPAYLOAD -> HalCashAccountPayload.fromProto(proto);
            case PIN4ACCOUNTPAYLOAD -> Pin4AccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    private bisq.account.protobuf.CountryBasedAccountPayload toCountryBasedAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCountryBasedAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CountryBasedAccountPayload.newBuilder()
                .setCountryCode(countryCode);
    }

    public Country getCountry() {
        return CountryRepository.getCountry(countryCode);
    }
}
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

import bisq.account.accounts.crypto.CryptoAssetAccount;
import bisq.account.accounts.fiat.CashByMailAccount;
import bisq.account.accounts.fiat.CountryBasedAccount;
import bisq.account.accounts.fiat.FasterPaymentsAccount;
import bisq.account.accounts.fiat.InteracETransferAccount;
import bisq.account.accounts.fiat.PayIdAccount;
import bisq.account.accounts.fiat.RevolutAccount;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.KeyPairProtoUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;

/**
 * Account is only stored locally and never shared with the peer. It can contain sensitive data.
 */
@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class Account<M extends PaymentMethod<?>, P extends AccountPayload<M>> implements PersistableProto {
    protected final String id;
    protected final long creationDate;
    protected final String accountName;
    protected final P accountPayload;
    protected final KeyPair keyPair; // account specific key pair used for account age verification for proof of ownership
    protected final String keyAlgorithm; // DSA for Bisq 1 imported accounts or EC for new Bisq 2 accounts

    public Account(String id,
                   long creationDate,
                   String accountName,
                   P accountPayload) {
        this(id,
                creationDate,
                accountName,
                accountPayload,
                KeyGeneration.generateKeyPair(),
                KeyGeneration.EC);
    }

    public Account(String id,
                   long creationDate,
                   String accountName,
                   P accountPayload,
                   KeyPair keyPair,
                   String keyAlgorithm) {
        this.id = id;
        this.creationDate = creationDate;
        this.accountName = accountName;
        this.accountPayload = accountPayload;
        this.keyPair = keyPair;
        this.keyAlgorithm = keyAlgorithm;
    }

    @Override
    public bisq.account.protobuf.Account toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.account.protobuf.Account completeProto() {
        return toProto(false);
    }

    protected bisq.account.protobuf.Account.Builder getAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.Account.newBuilder()
                .setId(id)
                .setCreationDate(creationDate)
                .setAccountName(accountName)
                .setAccountPayload(accountPayload.toProto(serializeForHash))
                .setKeyPair(KeyPairProtoUtil.toProto(keyPair))
                .setKeyAlgorithm(keyAlgorithm);
    }

    public static Account<?, ?> fromProto(bisq.account.protobuf.Account proto) {
        return switch (proto.getMessageCase()) {
            case ZELLEACCOUNT -> ZelleAccount.fromProto(proto);
            case USERDEFINEDFIATACCOUNT -> UserDefinedFiatAccount.fromProto(proto);
            case REVOLUTACCOUNT -> RevolutAccount.fromProto(proto);
            case COUNTRYBASEDACCOUNT -> CountryBasedAccount.fromProto(proto);
            case FASTERPAYMENTSACCOUNT -> FasterPaymentsAccount.fromProto(proto);
            case PAYIDACCOUNT -> PayIdAccount.fromProto(proto);
            case USPOSTALMONEYORDERACCOUNT -> USPostalMoneyOrderAccount.fromProto(proto);
            case CASHBYMAILACCOUNT -> CashByMailAccount.fromProto(proto);
            case INTERACETRANSFERACCOUNT -> InteracETransferAccount.fromProto(proto);
            case CRYPTOASSETACCOUNT -> CryptoAssetAccount.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public M getPaymentMethod() {
        return accountPayload.getPaymentMethod();
    }

    public List<String> getSupportedCurrencyCodes() {
        return getPaymentMethod().getSupportedCurrencyCodes();
    }
}
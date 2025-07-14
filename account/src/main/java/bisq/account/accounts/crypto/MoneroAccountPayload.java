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

package bisq.account.accounts.crypto;

import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.CryptoPaymentRail;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneroAccountPayload extends CryptoCurrencyAccountPayload {
    private final boolean useSubAddresses;
    private final Optional<String> privateViewKey;
    private final Optional<Integer> accountIndex;
    private final Optional<Integer> initialSubAddressIndex;

    public MoneroAccountPayload(String id,
                                String address,
                                boolean isInstant,
                                boolean isAutoConf,
                                boolean useSubAddresses,
                                Optional<String> privateViewKey,
                                Optional<Integer> accountIndex,
                                Optional<Integer> initialSubAddressIndex) {
        this(id,
                "XMR",
                address,
                isInstant,
                isAutoConf,
                useSubAddresses,
                privateViewKey,
                accountIndex,
                initialSubAddressIndex);
    }

    private MoneroAccountPayload(String id,
                                 String currencyCode,
                                 String address,
                                 boolean isInstant,
                                 boolean isAutoConf,
                                 boolean useSubAddresses,
                                 Optional<String> privateViewKey,
                                 Optional<Integer> accountIndex,
                                 Optional<Integer> initialSubAddressIndex) {
        super(id, currencyCode, address, isInstant, isAutoConf);
        this.useSubAddresses = useSubAddresses;
        this.privateViewKey = privateViewKey;
        this.accountIndex = accountIndex;
        this.initialSubAddressIndex = initialSubAddressIndex;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoCurrencyAccountPayload.Builder builder = getCryptoCurrencyAccountPayloadBuilder(serializeForHash)
                .setMoneroAccountPayload(getMoneroAccountPayloadBuilder(serializeForHash));
        bisq.account.protobuf.CryptoCurrencyAccountPayload cryptoCurrencyAccountPayload = resolveBuilder(builder, serializeForHash).build();
        return getAccountPayloadBuilder(serializeForHash)
                .setCryptoCurrencyAccountPayload(cryptoCurrencyAccountPayload);
    }

    private bisq.account.protobuf.MoneroAccountPayload.Builder getMoneroAccountPayloadBuilder(
            boolean serializeForHash) {
        bisq.account.protobuf.MoneroAccountPayload.Builder builder = bisq.account.protobuf.MoneroAccountPayload.newBuilder()
                .setUseSubAddresses(useSubAddresses);
        privateViewKey.ifPresent(builder::setPrivateViewKey);
        accountIndex.ifPresent(builder::setAccountIndex);
        initialSubAddressIndex.ifPresent(builder::setInitialSubAddressIndex);
        return builder;
    }

    public static MoneroAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cryptoCurrency = proto.getCryptoCurrencyAccountPayload();
        var monero = cryptoCurrency.getMoneroAccountPayload();
        return new MoneroAccountPayload(
                proto.getId(),
                cryptoCurrency.getCurrencyCode(),
                cryptoCurrency.getAddress(),
                cryptoCurrency.getIsInstant(),
                cryptoCurrency.getIsAutoConf(),
                monero.getUseSubAddresses(),
                monero.hasPrivateViewKey() ? Optional.of(monero.getPrivateViewKey()) : Optional.empty(),
                monero.hasAccountIndex() ? Optional.of(monero.getAccountIndex()) : Optional.empty(),
                monero.hasInitialSubAddressIndex() ? Optional.of(monero.getInitialSubAddressIndex()) : Optional.empty()
        );
    }

    @Override
    public CryptoPaymentMethod getPaymentMethod() {
        return CryptoPaymentMethod.fromPaymentRail(CryptoPaymentRail.NATIVE_CHAIN, "XMR");
    }

  /*  @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.altcoin.currencyCode"), currencyCode,
                Res.get("user.paymentAccounts.altcoin.address"), address
        ).toString();
    }*/
}

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

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CryptoAssetAccountPayload extends AccountPayload<CryptoPaymentMethod> implements SingleCurrencyAccountPayload {
    protected final String currencyCode;
    protected final String address;
    protected final boolean isInstant;
    protected final Optional<Boolean> isAutoConf;
    protected final Optional<Integer> autoConfNumConfirmations;
    protected final Optional<Long> autoConfMaxTradeAmount;
    protected final Optional<String> autoConfExplorerUrls;

    public CryptoAssetAccountPayload(String id,
                                     String currencyCode,
                                     String address,
                                     boolean isInstant,
                                     Optional<Boolean> isAutoConf,
                                     Optional<Integer> autoConfNumConfirmations,
                                     Optional<Long> autoConfMaxTradeAmount,
                                     Optional<String> autoConfExplorerUrls) {
        super(id);
        this.currencyCode = currencyCode;
        this.address = address;
        this.isInstant = isInstant;
        this.isAutoConf = isAutoConf;
        this.autoConfNumConfirmations = autoConfNumConfirmations;
        this.autoConfMaxTradeAmount = autoConfMaxTradeAmount;
        this.autoConfExplorerUrls = autoConfExplorerUrls;
    }

    protected bisq.account.protobuf.CryptoAssetAccountPayload toCryptoAssetAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCryptoAssetAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CryptoAssetAccountPayload.Builder getCryptoAssetAccountPayloadBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoAssetAccountPayload.Builder builder = bisq.account.protobuf.CryptoAssetAccountPayload.newBuilder()
                .setCurrencyCode(currencyCode)
                .setAddress(address)
                .setIsInstant(isInstant);
        isAutoConf.ifPresent(builder::setIsAutoConf);
        autoConfNumConfirmations.ifPresent(builder::setAutoConfNumConfirmations);
        autoConfMaxTradeAmount.ifPresent(builder::setAutoConfMaxTradeAmount);
        autoConfExplorerUrls.ifPresent(builder::setAutoConfExplorerUrls);
        return builder;
    }

    public static CryptoAssetAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getCryptoAssetAccountPayload().getMessageCase()) {
            case MONEROACCOUNTPAYLOAD -> MoneroAccountPayload.fromProto(proto);
            case OTHERCRYPTOASSETACCOUNTPAYLOAD -> OtherCryptoAssetAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    @Override
    public CryptoPaymentMethod getPaymentMethod() {
        return new CryptoPaymentMethod(currencyCode);
    }

    @Override
    public String getDefaultAccountName() {
        return getPaymentMethod().getName() + "-" + StringUtils.truncate(address, 8);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.crypto.summary.tickerSymbol"), currencyCode,
                Res.get("paymentAccounts.crypto.address.address"), address
        ).toString();
    }

    public String getCodeAndDisplayName() {
        return CryptoAssetRepository.findName(currencyCode)
                .map(name -> currencyCode + " (" + name + ")")
                .orElse(currencyCode);
    }


    @Override
    protected byte[] getFingerprint(byte[] data) {
        String codeAndAddress = currencyCode + address;
        return super.getFingerprint(ByteArrayUtils.concat(codeAndAddress.getBytes(StandardCharsets.UTF_8), data));
    }
}

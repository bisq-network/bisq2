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

package bisq.account.accounts.stable_coin;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodUtil;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.common.asset.StableCoin;
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class StableCoinAccountPayload extends AccountPayload<StableCoinPaymentMethod> implements SingleCurrencyAccountPayload {
    public static final int MAX_ADDRESS_LENGTH = 256;
    private static final java.util.regex.Pattern EVM_ADDRESS_PATTERN =
            java.util.regex.Pattern.compile("^0x[0-9a-fA-F]{40}$");

    private final String currencyCode;
    private final String address;
    private final String network;

    public StableCoinAccountPayload(String id, String currencyCode, String address, String network) {
        this(id, AccountUtils.generateSalt(), currencyCode, address, network);
    }

    public StableCoinAccountPayload(String id, byte[] salt, String currencyCode, String address, String network) {
        super(id, salt);
        this.currencyCode = currencyCode;
        this.address = address;
        this.network = network;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        checkArgument(StringUtils.isNotEmpty(address), "Address must not be empty");
        checkArgument(address.length() <= MAX_ADDRESS_LENGTH,
                "Address length must not exceed " + MAX_ADDRESS_LENGTH);
        checkArgument(StringUtils.isNotEmpty(currencyCode), "Currency code must not be empty");
        checkArgument(StringUtils.isNotEmpty(network), "Network must not be empty");
    }

    public static boolean isValidEvmAddress(String address) {
        return address != null && EVM_ADDRESS_PATTERN.matcher(address).matches();
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setStableCoinAccountPayload(
                        bisq.account.protobuf.StableCoinAccountPayload.newBuilder()
                                .setCurrencyCode(currencyCode)
                                .setAddress(address)
                                .setNetwork(network));
    }

    public static StableCoinAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getStableCoinAccountPayload();
        return new StableCoinAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getCurrencyCode(),
                payload.getAddress(),
                payload.getNetwork());
    }

    @Override
    public StableCoinPaymentMethod getPaymentMethod() {
        for (StableCoinPaymentRail rail : StableCoinPaymentRail.values()) {
            StableCoin sc = rail.getStableCoin();
            if (sc.getCode().equals(currencyCode) && sc.getNetwork().name().equals(network)) {
                return StableCoinPaymentMethod.fromPaymentRail(rail);
            }
        }
        return StableCoinPaymentMethodUtil.getPaymentMethod(currencyCode + "_" + network);
    }

    @Override
    public String getDefaultAccountName() {
        return currencyCode + " (" + network + ") " + StringUtils.truncate(address, 8);
    }

    public static String networkTag(String networkDisplayName) {
        return "(" + networkDisplayName + ")";
    }

    public static boolean containsNetworkTag(String accountData, String networkDisplayName) {
        return accountData != null && accountData.contains(networkTag(networkDisplayName));
    }

    @Override
    public String getAccountDataDisplayString() {
        String networkDisplayName = resolveNetworkDisplayName();
        return address + " " + networkTag(networkDisplayName);
    }

    private String resolveNetworkDisplayName() {
        for (StableCoinPaymentRail rail : StableCoinPaymentRail.values()) {
            StableCoin sc = rail.getStableCoin();
            if (sc.getCode().equals(currencyCode) && sc.getNetwork().name().equals(network)) {
                return sc.getNetwork().getDisplayName();
            }
        }
        return network;
    }

    @Override
    public byte[] getBisq1CompatibleFingerprint() {
        return super.getBisq1CompatibleFingerprint(address.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected byte[] getBisq2Fingerprint() {
        return super.getBisq2Fingerprint(joinWithSeparator(currencyCode, address, network));
    }
}

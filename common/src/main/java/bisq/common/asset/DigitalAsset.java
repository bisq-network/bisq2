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

package bisq.common.asset;


import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class DigitalAsset extends Asset {
    public DigitalAsset(String code) {
        this(code, code);
    }

    public DigitalAsset(String code, String name) {
        super(code, name);
    }

    public bisq.common.protobuf.DigitalAsset.Builder getDigitalAssetBuilder() {
        return bisq.common.protobuf.DigitalAsset.newBuilder();
    }

    public static Asset fromProto(bisq.common.protobuf.Asset proto) {
        return switch (proto.getDigitalAsset().getMessageCase()) {
            case CRYPTOASSET -> CryptoAsset.fromProto(proto);
            case STABLECOIN -> StableCoin.fromProto(proto);
            case CENTRALBANKDIGITALCURRENCY -> CentralBankDigitalCurrency.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public abstract boolean isCustom();
}

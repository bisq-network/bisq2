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


import bisq.common.validation.Validation;
import bisq.common.validation.crypto.CryptoAddressValidationRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CryptoAsset extends DigitalAsset {
    private static final Set<String> SUPPORT_AUTO_CONF_CODES = Set.of("XMR");

    @Getter
    private transient final Validation addressValidation;
    @Getter
    private transient final boolean supportAutoConf;

    // For custom cryptoCurrencies not listed in Bisq
    public CryptoAsset(String code) {
        this(code, code);
    }

    public CryptoAsset(String code, String name) {
        super(code, name);
        this.addressValidation = CryptoAddressValidationRepository.getValidation(code);
        supportAutoConf = SUPPORT_AUTO_CONF_CODES.contains(code);
    }

    @Override
    public bisq.common.protobuf.Asset.Builder getBuilder(boolean serializeForHash) {
        return getAssetBuilder().setDigitalAsset(bisq.common.protobuf.DigitalAsset.newBuilder());
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CryptoAsset fromProto(bisq.common.protobuf.Asset baseProto) {
        return new CryptoAsset(baseProto.getCode(), baseProto.getName());
    }

    @Override
    public boolean isCustom() {
        return CryptoAssetRepository.find(code).isEmpty();
    }
}

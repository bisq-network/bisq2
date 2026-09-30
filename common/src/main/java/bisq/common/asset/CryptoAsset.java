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

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class CryptoAsset extends DigitalAsset {

    @Getter
    @EqualsAndHashCode.Exclude
    private final int precision;
    @Getter
    private transient final Validation addressValidation;
    @Getter
    private transient final boolean supportAutoConf;

    // For custom cryptoCurrencies not listed in Bisq. Their native precision is unknown, so we
    // fall back to 8. Listed assets must state their precision explicitly (see the constructor
    // below) so a coin with fewer than 8 decimals cannot silently use 8 and carry unsendable digits.
    public CryptoAsset(String code) {
        this(code, code, 8);
    }

    public CryptoAsset(String code, String name, int precision) {
        super(code, name);
        // precision is the number of decimals Bisq trades the coin at; it must be <= the coin's
        // native on-chain decimals (fewer is safe, coarser rounding). 12 (XMR) is the highest
        // value Bisq currently uses and the ceiling that keeps amounts within a long.
        checkArgument(precision >= 0 && precision <= 12, "precision must be within [0, 12] but was " + precision);
        this.precision = precision;
        this.addressValidation = CryptoAddressValidationRepository.getValidation(code);
        supportAutoConf = CryptoAssetRepository.isAutoConfSupported(code);
    }

    @Override
    public bisq.common.protobuf.Asset.Builder getBuilder(boolean serializeForHash) {
        return getAssetBuilder().setDigitalAsset(bisq.common.protobuf.DigitalAsset.newBuilder());
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CryptoAsset fromProto(bisq.common.protobuf.Asset baseProto) {
        // Precision is not part of the Asset proto; re-derive it from the repository (custom -> 8).
        int precision = CryptoAssetRepository.find(baseProto.getCode())
                .map(CryptoAsset::getPrecision)
                .orElse(8);
        return new CryptoAsset(baseProto.getCode(), baseProto.getName(), precision);
    }

    @Override
    public boolean isCustom() {
        return CryptoAssetRepository.find(code).isEmpty();
    }
}

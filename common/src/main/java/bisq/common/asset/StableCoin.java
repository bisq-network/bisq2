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


import bisq.common.proto.ProtobufUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class StableCoin extends DigitalAsset {
    private final String pegCurrencyCode;
    private final Network network;
    private final TokenStandard tokenStandard;

    // We consider issues as only informational data, if for instance the backing company gets sold, the coin
    // should not change.
    @EqualsAndHashCode.Exclude
    private final Issuer issuer;

    public StableCoin(String code,
                      String name,
                      String pegCurrencyCode,
                      Network network,
                      TokenStandard tokenStandard,
                      Issuer issuer) {
        super(code, name);
        this.pegCurrencyCode = pegCurrencyCode;
        this.network = network;
        this.tokenStandard = tokenStandard;
        this.issuer = issuer;
    }

    public static boolean isStableCoin(String code) {
        return !StableCoinRepository.allWithCode(code).isEmpty();
    }

    //todo
    @Override
    public bisq.common.protobuf.Asset.Builder getBuilder(boolean serializeForHash) {
        return getAssetBuilder().setDigitalAsset(bisq.common.protobuf.DigitalAsset.newBuilder()
                .setStableCoin(
                        bisq.common.protobuf.StableCoin.newBuilder()
                                .setPegCurrencyCode(pegCurrencyCode)
                                .setNetwork(network.name())
                                .setTokenStandard(tokenStandard.name())
                                .setIssuer(issuer.name())));
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static StableCoin fromProto(bisq.common.protobuf.Asset baseProto) {
        bisq.common.protobuf.StableCoin stableCoinCurrencyProto = baseProto.getDigitalAsset().getStableCoin();
        Network network = ProtobufUtils.enumFromProto(Network.class, stableCoinCurrencyProto.getNetwork(), Network.UNDEFINED);
        TokenStandard standard = ProtobufUtils.enumFromProto(TokenStandard.class, stableCoinCurrencyProto.getTokenStandard(), TokenStandard.UNDEFINED);
        Issuer issuer = ProtobufUtils.enumFromProto(Issuer.class, stableCoinCurrencyProto.getIssuer(), Issuer.UNDEFINED);
        return new StableCoin(baseProto.getCode(), baseProto.getName(),
                stableCoinCurrencyProto.getPegCurrencyCode(),
                network,
                standard,
                issuer);
    }

    @Override
    public String getDisplayName() {
        // E.g. Tether USD (USDT, Ethereum ERC-20)
        return name + " (" + code + ", " + network + " " + tokenStandard + ")";
    }

    @Override
    public boolean isCustom() {
        return StableCoinRepository.find(code).isEmpty();
    }

    public String getShortDisplayName() {
        // E.g. USDT (ERC-20)
        return code + " (" + tokenStandard + ")";
    }

    public enum Network {
        UNDEFINED("Undefined"),
        ETHEREUM("Ethereum"),
        TRON("Tron"),
        BNB_SMART_CHAIN("BNB Smart Chain"),
        SOLANA("Solana"),
        BITCOIN("Bitcoin"),
        TAPROOT_ASSETS("Taproot Assets"),
        LIQUID("Liquid Bitcoin");
        /*RGB,
        FEDIMINT,
        STABLESATS*/
        @Getter
        private final String displayName;

        Network(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum Issuer {
        UNDEFINED("Undefined"),
        TETHER("Tether Ltd."),
        CIRCLE("Circle Internet Financial, LLC"),
        MAKERDAO("MakerDAO"),
        FIRST_DIGITAL("First Digital Trust Limited"),
        PAXOS("Paxos Trust Company"),
        GEMINI("Gemini Trust Company, LLC"),
        TECHTERYX("Techteryx Ltd."), // issuer of TrueUSD (TUSD)
        STABLESAT("Galoy Inc."),     // synthetic USD via Stablesats
        LIGHTNING_LABS("Lightning Labs"), // Taproot Assets experimental issuer
        FEDIMINT("Fedimint Federation");

        @Getter
        private final String displayName;

        Issuer(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum TokenStandard {
        UNDEFINED("Undefined"),
        // Ethereum
        ERC20("ERC-20"),
        ERC721("ERC-721"),
        ERC1155("ERC-1155"),
        ERC4626("ERC-4626"),
        ERC777("ERC-777"),

        // Thron
        TRC20("TRC-20"),

        // Bitcoin
        RGB("RGB"),
        OMNI("Omni"),

        // Lightning Network
        TAPROOT_ASSETS("Taproot Assets"),
        BOLT11("BOLT-11"),

        // Solana
        SPL("SPL"),

        // BNB Smart Chain
        BEP20("BEP-20"),
        BEP721("BEP-721"),
        BEP1155("BEP-1155");

        @Getter
        private final String displayName;

        TokenStandard(String displayName) {
            this.displayName = displayName;
        }
    }
}

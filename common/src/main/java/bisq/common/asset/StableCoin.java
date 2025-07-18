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


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class StableCoin extends Asset {
    private final String pegCurrencyCode;
    private final String chain;//TODO use StableCoinChain
    private final String standard; //TODO use StableCoinTokenStandard

    // We consider issues as only informational data, if for instance the backing company gets sold, the coin
    // should not change.
    @EqualsAndHashCode.Exclude
    private final String issuer; //TODO use StableCoinIssuer

    public StableCoin(String code,
                      String name,
                      String pegCurrencyCode,
                      StableCoinChain chain,
                      StableCoinTokenStandard standard,
                      StableCoinIssuer issuer) {
        this(code, name, pegCurrencyCode, chain.getDisplayName(), standard.getDisplayName(), issuer.getDisplayName());
    }

    public StableCoin(String code,
                      String name,
                      String pegCurrencyCode,
                      String chain,
                      String standard,
                      String issuer) {
        super(code, name);
        this.pegCurrencyCode = pegCurrencyCode;
        this.chain = chain;
        this.standard = standard;
        this.issuer = issuer;
    }

    public static boolean isStableCoin(String code) {
        return !StableCoinRepository.allWithCode(code).isEmpty();
    }

    //todo
    @Override
    public bisq.common.protobuf.Asset.Builder getBuilder(boolean serializeForHash) {
        return getAssetBuilder().setStableCoin(
                bisq.common.protobuf.StableCoin.newBuilder()
                        .setPegCurrencyCode(pegCurrencyCode)
                        .setChain(chain)
                        .setStandard(standard)
                        .setIssuer(issuer));
    }

    @Override
    public bisq.common.protobuf.Asset toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static StableCoin fromProto(bisq.common.protobuf.Asset baseProto) {
        bisq.common.protobuf.StableCoin stableCoinCurrencyProto = baseProto.getStableCoin();
        return new StableCoin(baseProto.getCode(), baseProto.getName(),
                stableCoinCurrencyProto.getPegCurrencyCode(),
                stableCoinCurrencyProto.getChain(),
                stableCoinCurrencyProto.getStandard(),
                stableCoinCurrencyProto.getIssuer());
    }

    @Override
    public String getDisplayName() {
        // E.g. Tether USD (USDT, Ethereum ERC-20)
        return name + " (" + code + ", " + chain + " " + standard + ")";
    }

    public String getShortDisplayName() {
        // E.g. USDT_ERC-20)
        return code + "_" + standard;
    }


    public enum StableCoinChain {
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

        StableCoinChain(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum StableCoinIssuer {
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

        StableCoinIssuer(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum StableCoinTokenStandard {
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

        StableCoinTokenStandard(String displayName) {
            this.displayName = displayName;
        }
    }
}

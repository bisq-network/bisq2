package bisq.wallets;

import lombok.Getter;

public enum AddressType {
    LEGACY("legacy"),
    P2SH_SEGWIT("p2sh-segwit"),
    BECH32("bech32");

    @Getter
    private final String name;

    AddressType(String name) {
        this.name = name;
    }
}

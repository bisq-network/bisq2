package bisq.wallets;

public enum AddressType {
    LEGACY("legacy"),
    P2SH_SEGWIT("p2sh-segwit"),
    BECH32("bech32");

    private final String name;

    AddressType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

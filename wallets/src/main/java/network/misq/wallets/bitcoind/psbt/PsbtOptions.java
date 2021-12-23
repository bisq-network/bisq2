package network.misq.wallets.bitcoind.psbt;

public record PsbtOptions(
        boolean includeWatching,
        int[] subtractFeeFromOutputs) {
}

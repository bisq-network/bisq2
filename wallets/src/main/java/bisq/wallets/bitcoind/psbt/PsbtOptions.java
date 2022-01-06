package bisq.wallets.bitcoind.psbt;

public record PsbtOptions(
        boolean includeWatching,
        int[] subtractFeeFromOutputs) {
}

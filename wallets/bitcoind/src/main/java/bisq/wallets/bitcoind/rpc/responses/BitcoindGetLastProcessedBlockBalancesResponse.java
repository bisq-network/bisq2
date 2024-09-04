package bisq.wallets.bitcoind.rpc.responses;

import lombok.Getter;

@Getter
public class BitcoindGetLastProcessedBlockBalancesResponse {
    private String hash;
    long height;
}

package bisq.wallet.vo;

import bisq.common.monetary.Coin;

public class GrpcUtxo implements Utxo {
    private final bisq.wallets.grpc.pb.Utxo utxo;

    public GrpcUtxo(bisq.wallets.grpc.pb.Utxo utxo) {
        this.utxo = utxo;
    }

    @Override
    public String getTxId() {
        return utxo.getTxId();
    }

    @Override
    public String getAddress() {
        return utxo.getAddress();
    }

    @Override
    public double getAmount() {
        return Coin.fromValue(utxo.getAmount(), "BTC").asDouble();
    }
}

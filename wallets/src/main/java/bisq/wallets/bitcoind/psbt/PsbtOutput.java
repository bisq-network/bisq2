package bisq.wallets.bitcoind.psbt;

import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PsbtOutput {
    private final Map<String, Double> amountByAddress = new HashMap<>();

    @Setter
    private String data = "00";

    public void addOutput(String address, double amount) {
        amountByAddress.put(address, amount);
    }

    public Object[] toPsbtOutputObject() {
        var dataMap = new HashMap<>();
        dataMap.put("data", data);
        return new Object[]{amountByAddress, dataMap};
    }
}

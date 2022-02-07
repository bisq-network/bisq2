package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.desktop.common.view.Model;
import bisq.wallets.model.Utxo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class WalletUtxosModel implements Model {
    public final ObservableList<WalletUtxoListItem> listItems = FXCollections.observableArrayList();

    public void addUtxos(List<Utxo> utxos) {
        utxos.stream()
                .map(WalletUtxoListItem::new)
                .forEach(listItems::add);
    }
}

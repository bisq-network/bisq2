package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.wallet.create_wallet.backup.CreateWalletBackupModel;
import bisq.desktop.main.content.wallet.create_wallet.backup.CreateWalletBackupView;
import bisq.wallets.core.WalletService;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateWalletBackupController implements Controller {
    private final CreateWalletBackupModel model;
    @Getter
    private final CreateWalletBackupView view;

    public CreateWalletBackupController(ServiceProvider serviceProvider) {
        model = new CreateWalletBackupModel();
        WalletService walletService = serviceProvider.getWalletService().orElseThrow();
        setSeedWords(walletService.getSeedWords());

        view = new CreateWalletBackupView(model, this);
    }

    public void setSeedWords(List<String> seedWords) {
        for (int i = 0; i < 12; i++) {
            model.getSeedWords()[i].set(seedWords.get(i));
        }
    }

    @Override
    public void onActivate() {
        
    }

    @Override
    public void onDeactivate() {

    }

}
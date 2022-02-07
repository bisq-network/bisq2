package bisq.desktop.primary.main.content.wallet.dialog;

import bisq.application.ApplicationConfig;
import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.wallets.NetworkType;
import bisq.wallets.WalletService;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class WalletConfigDialogController implements Controller {
    private final ApplicationConfig applicationConfig;
    private final WalletService walletService;

    private final WalletConfigDialogModel model;
    @Getter
    private final WalletConfigDialogView view;

    public WalletConfigDialogController(DefaultApplicationService applicationService) {
        this.applicationConfig = applicationService.getApplicationConfig();
        this.walletService = applicationService.getWalletService();

        model = new WalletConfigDialogModel();
        view = new WalletConfigDialogView(model, this);
    }

    public void showDialogAndConnectToWallet() {
        boolean userSubmittedData = view.createAndShowDialog();
        if (userSubmittedData) {
            String passphrase = model.walletPassphraseProperty().get();
            model.walletPassphraseProperty().setValue(""); // Wipe passphrase from memory

            Path walletsDataDirPath = getWalletsDataDirPath();
            RpcConfig rpcConfig = createRpcConfigFromModel();
            try {
                CompletableFuture<Void> future = walletService.initialize(walletsDataDirPath, rpcConfig, passphrase);
                future.get(); // Block until wallet connected.
            } catch (InterruptedException | ExecutionException e) {
                log.error("Wallet Initialization failed.", e);
            }
        }
    }

    private RpcConfig createRpcConfigFromModel() {
        return new RpcConfig.Builder()
                .networkType(NetworkType.REGTEST)
                .hostname(model.hostnameProperty().get())
                .port(Integer.parseInt(model.portProperty().get()))
                .user(model.usernameProperty().get())
                .password(model.passwordProperty().get())
                .build();
    }

    private Path getWalletsDataDirPath() {
        String baseDir = applicationConfig.baseDir();
        return FileSystems.getDefault().getPath(baseDir, "wallets");
    }

}

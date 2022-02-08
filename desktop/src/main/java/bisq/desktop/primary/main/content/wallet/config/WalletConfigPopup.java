/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.wallet.config;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.overlay.Popup;
import bisq.i18n.Res;
import bisq.wallets.NetworkType;
import bisq.wallets.WalletService;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class WalletConfigPopup extends Popup {
    private final Controller controller;

    public WalletConfigPopup(DefaultApplicationService applicationService, Runnable closeHandler) {
        super();
        controller = new Controller(applicationService, this, closeHandler);
    }

    @Override
    public void addContent() {
        super.addContent();
        controller.addContent();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final WalletService walletService;
        private final Popup popup;
        private final Runnable closeHandler;

        private final Model model;
        @Getter
        private final View view;

        private Controller(DefaultApplicationService applicationService, Popup popup, Runnable closeHandler) {
            this.walletService = applicationService.getWalletService();
            this.popup = popup;
            this.closeHandler = closeHandler;

            model = new Model();
            view = new View(model, this, popup);
            model.walletPathProperty.set(applicationService.getApplicationConfig().baseDir() + File.separator + "wallets");
        }

        @Override
        public void onViewAttached() {
        }

        @Override
        public void onViewDetached() {
        }

        private void onConnectToWallet() {
            String passphrase = model.walletPassphraseProperty.get();
            model.walletPassphraseProperty.setValue(""); // Wipe passphrase from memory

            Path walletsDataDirPath = FileSystems.getDefault().getPath(model.walletPathProperty.get());
            RpcConfig rpcConfig = createRpcConfigFromModel();
            walletService.initialize(walletsDataDirPath, rpcConfig, passphrase)
                    .whenComplete((__, throwable) -> {
                        if (throwable == null) {
                            UIThread.run(() -> {
                                popup.hide();
                                closeHandler.run();
                            });
                        } else {
                            throwable.printStackTrace();
                            UIThread.run(() -> {
                                popup.hide();
                                closeHandler.run();
                                new Popup().error(throwable.toString()).show();
                            });
                        }
                    });
        }

        private void onSelectWalletPath() {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(model.walletPathProperty.get()));
            directoryChooser.setTitle(Res.get("wallet.config.walletPath"));
            File file = directoryChooser.showDialog(popup.getGridPane().getScene().getWindow());
            model.walletPathProperty.set(file.getAbsolutePath());
        }

        private RpcConfig createRpcConfigFromModel() {
            return new RpcConfig.Builder()
                    .networkType(NetworkType.REGTEST)
                    .hostname(model.hostnameProperty.get())
                    .port(Integer.parseInt(model.portProperty.get()))
                    .user(model.usernameProperty.get())
                    .password(model.passwordProperty.get())
                    .build();
        }


        private void addContent() {
            view.addContent();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<String> walletBackends = FXCollections.observableArrayList("Bitcoin Core");
        private final StringProperty walletPathProperty = new SimpleStringProperty(this, "walletPath");
        private final StringProperty hostnameProperty = new SimpleStringProperty(this, "hostname", "127.0.0.1");
        private final StringProperty portProperty = new SimpleStringProperty(this, "port", "18443");
        private final StringProperty usernameProperty = new SimpleStringProperty(this, "username", "bisq");
        private final StringProperty passwordProperty = new SimpleStringProperty(this, "password", "bisq");
        private final StringProperty walletPassphraseProperty = new SimpleStringProperty(this, "wallet_passphrase", "bisq");
    }

    private static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final Popup popup;

        private View(Model model, Controller controller, Popup popup) {
            super(new Pane(), model, controller);
            this.popup = popup;

            popup.headLine(Res.get("wallet.config.title"));
            popup.message(Res.get("wallet.config.header"));
            popup.actionButtonText(Res.get("wallet.config.connect"));
            popup.onAction(controller::onConnectToWallet);
            popup.doCloseOnAction(false);
        }

        private void addContent() {
            BisqGridPane gridPane = popup.getGridPane();

            gridPane.addButton(Res.get("wallet.config.selectWalletPath"), controller::onSelectWalletPath);
            gridPane.addTextField(Res.get("wallet.config.walletPath"), model.walletPathProperty);

            BisqComboBox<String> walletBackendComboBox = gridPane.addComboBox(model.walletBackends);
            walletBackendComboBox.setPromptText(Res.get("wallet.config.selectWallet"));
            walletBackendComboBox.getSelectionModel().selectFirst();

            gridPane.addTextField(Res.get("wallet.config.enterHostname"), model.hostnameProperty);
            gridPane.addTextField(Res.get("wallet.config.enterPort"), model.portProperty);
            gridPane.addTextField(Res.get("wallet.config.enterUsername"), model.usernameProperty);
            gridPane.addPasswordField(Res.get("wallet.config.enterPassword"), model.passwordProperty);
            gridPane.addPasswordField(Res.get("wallet.config.enterPassphrase"), model.walletPassphraseProperty);
        }
    }
}
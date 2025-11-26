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

package bisq.desktop.main.content.wallet;

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.ContentTabView;
import bisq.desktop.main.content.wallet.txs.WalletTxsView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class WalletView extends ContentTabView<WalletModel, WalletController> {
    private Button setupWalletButton;
    private Hyperlink restoreWalletLink;
    private Subscription isWalletInitializedPin;

    public WalletView(WalletModel model, WalletController controller) {
        super(model, controller);

        addTab(Res.get("wallet.dashboard"), NavigationTarget.WALLET_DASHBOARD);
        addTab(Res.get("wallet.coinManagement"), NavigationTarget.WALLET_COIN_MANAGEMENT);
        addTab(Res.get("wallet.send"), NavigationTarget.WALLET_SEND);
        addTab(Res.get("wallet.receive"), NavigationTarget.WALLET_RECEIVE);
        addTab(Res.get("wallet.txs"), NavigationTarget.WALLET_TXS);
        addTab(Res.get("wallet.settings"), NavigationTarget.WALLET_SETTINGS);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        isWalletInitializedPin = EasyBind.subscribe(model.getIsWalletInitialized(), isInitialized -> {
            log.info("Wallet initialization status changed: {}", isInitialized);
            if (isInitialized) {
                setContentToTabs();
            } else {
                setContentToNotInitialized();
                Navigation.navigateTo(NavigationTarget.SETUP_WALLET);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        isWalletInitializedPin.unsubscribe();

        if (setupWalletButton != null) {
            setupWalletButton.setOnAction(null);
        }
        if (restoreWalletLink != null) {
            restoreWalletLink.setOnAction(null);
        }
    }

    @Override
    protected boolean useFitToHeight(View<? extends Parent, ? extends Model, ? extends Controller> childView) {
        return childView instanceof WalletTxsView;
    }

    private void setContentToTabs() {
        root.setPadding(new Insets(0));
        root.getChildren().setAll(topBox, lineAndMarker, scrollPane);
        if (model.getView().get() != null) {
            scrollPane.setContent(model.getView().get().getRoot());
        }
    }

    private void setContentToNotInitialized() {
        Label label = new Label(Res.get("wallet.noSetup.headline"));
        label.getStyleClass().addAll("thin-text", "very-large-text");

        setupWalletButton = new Button(Res.get("wallet.noSetup.button.setup"));
        setupWalletButton.setDefaultButton(true);
        setupWalletButton.setOnAction(e -> controller.onSetupWalletButtonClicked());

        restoreWalletLink = new Hyperlink(Res.get("wallet.noSetup.button.restore"));
        restoreWalletLink.setOnAction(e -> controller.onRestoreWalletLinkClicked());

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(label, setupWalletButton, restoreWalletLink);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.CENTER);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        VBox.setMargin(label, new Insets(0, 0, 20, 0));
        root.setPadding(new Insets(40, 40, 20, 40));
        root.getChildren().setAll(contentBox);
    }
}

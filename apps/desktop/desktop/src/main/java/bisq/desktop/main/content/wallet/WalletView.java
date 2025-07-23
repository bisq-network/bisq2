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

import bisq.desktop.main.content.wallet.components.VerticalCard;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@Slf4j
public class WalletView extends ContentTabView<WalletModel, WalletController> {

    private static final Insets CARD_CONTAINER_INSETS = new Insets(25, 0, 50, 0);
    private static final Insets CONTAINER_INSETS = new Insets(50, 20, 20, 20);
    private Button createWalletButton;
    private Button restoreWalletButton;

    private Subscription isWalletInitializedPin;
    private final VBox notInitializedBox;

    public WalletView(WalletModel model, WalletController controller) {
        super(model, controller);

        addTab(Res.get("wallet.dashboard"), NavigationTarget.WALLET_DASHBOARD);
        addTab(Res.get("wallet.send"), NavigationTarget.WALLET_SEND);
        addTab(Res.get("wallet.receive"), NavigationTarget.WALLET_RECEIVE);
        addTab(Res.get("wallet.txs"), NavigationTarget.WALLET_TXS);
        addTab(Res.get("wallet.settings"), NavigationTarget.WALLET_SETTINGS);

        notInitializedBox = createNotInitializedUI();

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
            }
        });
    }

    private void setContentToTabs() {
        root.setPadding(new Insets(0));
        root.getChildren().setAll(topBox, lineAndMarker, scrollPane);
        if (model.getView().get() != null) {
            scrollPane.setContent(model.getView().get().getRoot());
        }
    }

    private void setContentToNotInitialized() {
        root.setPadding(new Insets(40));
        root.getChildren().setAll(notInitializedBox);

        createWalletButton.setOnAction(e -> getController().onCreateWallet());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        if (isWalletInitializedPin != null) {
            isWalletInitializedPin.unsubscribe();
            isWalletInitializedPin = null;
        }
    }

    public VBox createNotInitializedUI() {
        Label headlineLabel = new Label(Res.get("wallet.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label descriptionLabel = new Label(Res.get("wallet.description"));
        descriptionLabel.getStyleClass().addAll("bisq-text-3");

        // TODO: Update with right images
        VBox card1 = new VerticalCard("1", "wallet-protect", Res.get("wallet.instruction.caption1"));
        VBox card2 = new VerticalCard("2", "wallet-seed-backup", Res.get("wallet.instruction.caption2"));
        VBox card3 = new VerticalCard("3", "wallet-seed-verify", Res.get("wallet.instruction.caption3"));

        HBox cardContainer = new HBox(25);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(CARD_CONTAINER_INSETS);
        cardContainer.getChildren().addAll(card1, card2, card3);

        restoreWalletButton = new Button(Res.get("wallet.button.restoreWallet"));

        createWalletButton = new Button(Res.get("wallet.button.createWallet"));
        createWalletButton.setDefaultButton(true);

        HBox buttons = new HBox(30, restoreWalletButton, createWalletButton);
        buttons.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(20, headlineLabel, descriptionLabel, cardContainer, buttons);
        vBox.setPadding(CONTAINER_INSETS);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("bisq-common-bg");
        VBox.setVgrow(vBox, Priority.ALWAYS);

        return vBox;

    }

    private WalletModel getModel() {
        return (WalletModel) model;
    }

    private WalletController getController() {
        return (WalletController) controller;
    }
}

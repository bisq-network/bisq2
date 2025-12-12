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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.setup_or_restore;

import bisq.desktop.common.view.View;
import bisq.desktop.main.content.wallet.components.VerticalCard;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetupWalletWizardSetupOrRestoreView extends View<VBox, SetupWalletWizardSetupOrRestoreModel, SetupWalletWizardSetupOrRestoreController> {
    private static final Insets CARD_CONTAINER_INSETS = new Insets(10, 0, 10, 0);

    public SetupWalletWizardSetupOrRestoreView(SetupWalletWizardSetupOrRestoreModel model, SetupWalletWizardSetupOrRestoreController controller) {
        super(new VBox(25), model, controller);

        Label headlineLabel = new Label(Res.get("wallet.setupOrRestoreWallet.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label descriptionLabel = new Label(Res.get("wallet.setupOrRestoreWallet.description"));
        descriptionLabel.getStyleClass().add("bisq-text-3");

        VBox card1 = new VerticalCard("1", "wallet-password", Res.get("wallet.setupOrRestoreWallet.instruction.caption1"));
        VBox card2 = new VerticalCard("2", "wallet-seeds", Res.get("wallet.setupOrRestoreWallet.instruction.caption2"));
        VBox card3 = new VerticalCard("3", "wallet-verify", Res.get("wallet.setupOrRestoreWallet.instruction.caption3"));

        HBox cardContainer = new HBox(25);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(CARD_CONTAINER_INSETS);
        cardContainer.getChildren().addAll(card1, card2, card3);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(headlineLabel, descriptionLabel, cardContainer);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}

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

package network.misq.desktop.main.left;

import javafx.geometry.Insets;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.controls.AutoTooltipToggleButton;
import network.misq.desktop.main.content.createoffer.CreateOfferController;
import network.misq.desktop.main.content.markets.MarketsController;
import network.misq.desktop.main.content.offerbook.OfferbookController;

public class NavigationView extends View<VBox, NavigationViewModel, NavigationViewController> {
    private final ToggleGroup navButtons = new ToggleGroup();

    public NavigationView(NavigationViewModel model, NavigationViewController controller) {
        super(new VBox(), model, controller);

        root.setMaxWidth(337);
        root.setMinWidth(337);
        root.setPadding(new Insets(0, 0, 0, 20));


        NavButton markets = new NavButton(MarketsController.class, "Markets");
        NavButton offerBook = new NavButton(OfferbookController.class, "Offerbook");
        NavButton createOffer = new NavButton(CreateOfferController.class, "Create offer");
      /*   NavButton trades = new NavButton(TradesViewController.class, "Trades");
        NavButton funds = new NavButton(FundsViewController.class, "Funds");
        NavButton accounts = new NavButton(AccountsViewController.class, "Accounts");
        NavButton settings = new NavButton(SettingsViewController.class, "SettingsView");*/

        root.getChildren().addAll(markets, offerBook, createOffer /*,trades, funds, accounts, settings*/);

      /*  Navigation.addListener((viewPath, data) -> {
            if (viewPath.size() != 2 || viewPath.indexOf(MainView.class) != 0) {
                return;
            }

            Class<? extends View> tip = viewPath.tip();
            navButtons.getToggles().stream()
                    .filter(toggle -> tip == ((NavButton) toggle).target)
                    .forEach(toggle -> toggle.setSelected(true));
        });*/


    }


    private class NavButton extends AutoTooltipToggleButton {
        final Class<? extends Controller> target;

        NavButton(Class<? extends Controller> target, String title) {
            super(title);
            this.target = target;
            this.setToggleGroup(navButtons);
            this.getStyleClass().add("navigation-button");
            this.selectedProperty().addListener((ov, oldValue, newValue) -> this.setMouseTransparent(newValue));
            this.setOnAction(e -> controller.onShowView(target));
        }
    }
}

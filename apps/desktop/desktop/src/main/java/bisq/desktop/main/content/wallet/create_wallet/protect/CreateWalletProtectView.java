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

package bisq.desktop.main.content.wallet.create_wallet.protect;

import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CreateWalletProtectView extends View<StackPane, CreateWalletProtectModel, CreateWalletProtectController> {
    private final Label headlineLabel;
    private final VBox content;

    public CreateWalletProtectView(CreateWalletProtectModel model,
                               CreateWalletProtectController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, Spacer.fillVBox());

        root.getChildren().addAll(content);
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText("Protect your wallet");
    }

    @Override
    protected void onViewDetached() {
        root.setOnKeyPressed(null);
    }

}

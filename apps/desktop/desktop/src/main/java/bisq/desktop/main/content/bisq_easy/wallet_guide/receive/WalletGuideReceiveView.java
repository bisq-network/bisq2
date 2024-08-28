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

package bisq.desktop.main.content.bisq_easy.wallet_guide.receive;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletGuideReceiveView extends View<HBox, WalletGuideReceiveModel, WalletGuideReceiveController> {
    private final Button backButton, closeButton;
    private final Hyperlink link1, link2;
    private final ImageView image1, image2;
    private FadeTransition fadeTransition1, fadeTransition2;
    private UIScheduler scheduler1, scheduler2, scheduler3;

    public WalletGuideReceiveView(WalletGuideReceiveModel model, WalletGuideReceiveController controller) {
        super(new HBox(20), model, controller);

        VBox vBox = new VBox(20);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("bisqEasy.walletGuide.receive.headline"));
        headline.getStyleClass().add("bisq-easy-trade-guide-headline");

        Text text = new Text(Res.get("bisqEasy.walletGuide.receive.content"));
        text.getStyleClass().add("bisq-easy-trade-guide-content");
        TextFlow content = new TextFlow(text);

        link1 = new Hyperlink(Res.get("bisqEasy.walletGuide.receive.link1"));
        link2 = new Hyperlink(Res.get("bisqEasy.walletGuide.receive.link2"));
        backButton = new Button(Res.get("action.back"));
        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        HBox buttons = new HBox(20, backButton, closeButton);
        VBox.setMargin(headline, new Insets(0, 0, -5, 0));
        VBox.setMargin(link1, new Insets(-10, 0, -22.5, 0));
        VBox.setMargin(link2, new Insets(0, 0, 0, 0));
        vBox.getChildren().addAll(headline, content, link1, link2, buttons);

        image1 = ImageUtil.getImageViewById("blue-wallet-tx");
        image2 = ImageUtil.getImageViewById("blue-wallet-qr");
        StackPane images = new StackPane(image1, image2);
        images.setAlignment(Pos.TOP_LEFT);
        root.getChildren().addAll(vBox, images);

        fadeTransition1 = new FadeTransition(Duration.millis(1000), image2);
        fadeTransition1.setFromValue(0);
        fadeTransition1.setToValue(1);

        fadeTransition2 = new FadeTransition(Duration.millis(1000), image2);
        fadeTransition2.setFromValue(1);
        fadeTransition2.setToValue(0);
    }

    @Override
    protected void onViewAttached() {
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        link1.setOnAction(e -> controller.onOpenLink1());
        link2.setOnAction(e -> controller.onOpenLink2());

        // TODO (low prio) create carousel component for it (See https://github.com/bisq-network/bisq2/issues/1262)
        image2.setOpacity(0);
        scheduler1 = UIScheduler.run(() -> fadeTransition1.playFromStart()).after(2000);
        fadeTransition1.setOnFinished(e -> {
            if (scheduler2 != null) {
                scheduler2.stop();
            }
            scheduler2 = UIScheduler.run(() -> fadeTransition2.playFromStart()).after(2000);
        });
        fadeTransition2.setOnFinished(e -> {
            if (scheduler3 != null) {
                scheduler3.stop();
            }
            scheduler3 = UIScheduler.run(() -> fadeTransition1.playFromStart()).after(2000);
        });
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        link1.setOnAction(null);
        link2.setOnAction(null);

        scheduler1.stop();
        fadeTransition1.stop();
        fadeTransition2.stop();
        if (scheduler2 != null) {
            scheduler2.stop();
        }
        if (scheduler3 != null) {
            scheduler3.stop();
        }
    }
}

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

package bisq.desktop.primary.main.content.social.education;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EducationView extends View<VBox, EducationModel, EducationController> {
    private static final int MARGIN = 66;
    private static final int SCROLLBAR_WIDTH = 12;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private ChangeListener<Number> widthListener;
    private Parent parent;

    public EducationView(EducationModel model, EducationController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(MARGIN);

        addHeaderBox();
        addSmallBox("bisq", "bitcoin");
        addSmallBox("security", "privacy");
        addSmallBox("wallets", "foss");
    }

    private void addHeaderBox() {
        Text headlineLabel = new Text(Res.get("social.education.headline"));
        headlineLabel.setStyle("-fx-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 4em");

        Text contentLabel = new Text(Res.get("social.education.content"));
        contentLabel.setStyle("-fx-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2em");

        VBox box = new VBox();
        box.setSpacing(MARGIN / 2);
        box.setStyle("-fx-background-color: -bisq-dark-bg");
        box.setPadding(new Insets(MARGIN - 16, 0, MARGIN - 6, MARGIN));
        box.getChildren().addAll(headlineLabel, contentLabel);
        root.getChildren().add(box);
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double right = root.getPadding().getRight();
            double value = w.doubleValue() - right + SCROLLBAR_WIDTH;
            double wrappingWidth = value - box.getPadding().getLeft() - box.getPadding().getRight();
            contentLabel.setWrappingWidth(wrappingWidth);
            headlineLabel.setWrappingWidth(wrappingWidth);
            box.setPrefWidth(value);
            box.setMinWidth(value);
            box.setMaxWidth(value);
        }));
    }

    private void addSmallBox(String leftTopic, String rightTopic) {
        VBox leftBox = getWidgetBox(Res.get("social.education." + leftTopic + ".headline"),
                Res.get("social.education." + leftTopic + ".content"),
                Res.get("social.education." + leftTopic + ".button"));

        VBox rightBox = getWidgetBox(Res.get("social.education." + rightTopic + ".headline"),
                Res.get("social.education." + rightTopic + ".content"),
                Res.get("social.education." + rightTopic + ".button"));

        HBox box = Layout.hBoxWith(leftBox, rightBox);
        box.setSpacing(MARGIN);
        root.getChildren().add(box);
    }

    private VBox getWidgetBox(String headline, String content, String buttonLabel) {
        Label headlineLabel = new Label(headline);
        headlineLabel.setWrapText(true);
        headlineLabel.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 3em");

        // Using Label does not work with layout
        Text contentLabel = new Text(content);
        contentLabel.setStyle("-fx-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2em");
        Button button = new BisqButton(buttonLabel);
        // textArea.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.46em");

        VBox box = Layout.vBoxWith(headlineLabel, contentLabel, button);
        box.setSpacing(MARGIN / 2);
        box.setStyle("-fx-background-color: -bisq-dark-bg");
        box.setPadding(new Insets(MARGIN - 16, 0, MARGIN - 6, MARGIN));
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double value = (w.doubleValue() - root.getPadding().getRight() - MARGIN + SCROLLBAR_WIDTH) / 2;
            contentLabel.setWrappingWidth(value - box.getPadding().getLeft() - box.getPadding().getRight());
            box.setPrefWidth(value);
            box.setMinWidth(value);
            box.setMaxWidth(value);
        }));
        return box;
    }

    @Override
    protected void onViewAttached() {

        // As we have scroll pane as parent container our root grows when increasing width but does not shrink anymore.
        // The adjustment for the width of the boxes is needed as otherwise the 
        parent = root.getParent();
        if (parent != null) {
            int maxIterations = 10;
            int iterations = 0;
            while (parent != null && !(parent instanceof VBox) && iterations < maxIterations) {
                parent = parent.getParent();
                iterations++;
            }
            if (iterations < maxIterations) {
                widthListener = (observable, oldValue, newValue) -> {
                    // scroll bar width
                    double value = newValue.doubleValue() - MARGIN ;
                    root.setMinWidth(value);
                };
                if (parent instanceof VBox vBox) {
                    vBox.widthProperty().addListener(widthListener);
                }
            }
        }
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);

        if (parent instanceof VBox vBox) {
            vBox.widthProperty().removeListener(widthListener);
        }
    }
}

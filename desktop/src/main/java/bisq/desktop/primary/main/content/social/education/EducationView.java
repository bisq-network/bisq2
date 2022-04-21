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

import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EducationView extends View<VBox, EducationModel, EducationController> implements TabViewChild {
    private static final int MARGIN = 44;
    private static final int TEXT_SPACE = 22;
    private static final int SCROLLBAR_WIDTH = 12;

    private final Set<Subscription> subscriptions = new HashSet<>();
    @Nullable
    private ChangeListener<Number> widthListener;
    @Nullable
    private Parent parent;

    public EducationView(EducationModel model, EducationController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(MARGIN);
    }

    @Override
    protected void onViewAttached() {
        addHeaderBox();
        addSmallBox("bisq", "bitcoin");
        addSmallBox("security", "privacy");
        addSmallBox("wallets", "foss");

        // As we have scroll pane as parent container our root grows when increasing width but does not shrink anymore.
        // If anyone finds a better solution would be nice to get rid of that hack...
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
                    double value = newValue.doubleValue() - MARGIN;
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

        if (widthListener != null && parent instanceof VBox vBox) {
            vBox.widthProperty().removeListener(widthListener);
        }
    }

    private void addHeaderBox() {
        Text headlineLabel = new Text(Res.get("social.education.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-1");

        Text contentLabel = new Text(Res.get("social.education.content"));
        contentLabel.getStyleClass().add("bisq-text-1");

        VBox box = new VBox();
        box.setSpacing(TEXT_SPACE);
        box.getStyleClass().add("bisq-box-1");
        box.setPadding(new Insets(MARGIN - 16, 0, MARGIN - 6, MARGIN));
        box.getChildren().addAll(headlineLabel, contentLabel);
        root.getChildren().add(box);
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double right = root.getPadding().getRight();
            double value = w.doubleValue() - right + SCROLLBAR_WIDTH;
            double wrappingWidth = value - box.getPadding().getLeft() - box.getPadding().getRight();
            contentLabel.setWrappingWidth(wrappingWidth - MARGIN);
            headlineLabel.setWrappingWidth(wrappingWidth - MARGIN);
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
        Text headlineLabel = new Text(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Text contentLabel = new Text(content);
        contentLabel.getStyleClass().add("bisq-text-1");

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("bisq-border-dark-bg-button");

        VBox box = Layout.vBoxWith(headlineLabel, contentLabel, button);
        box.setSpacing(TEXT_SPACE);
        box.getStyleClass().add("bisq-box-1");
        box.setPadding(new Insets(MARGIN - 16, 0, MARGIN - 6, MARGIN));
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double value = (w.doubleValue() - root.getPadding().getRight() - MARGIN + SCROLLBAR_WIDTH) / 2;
            double wrappingWidth = value - box.getPadding().getLeft() - box.getPadding().getRight();
            headlineLabel.setWrappingWidth(wrappingWidth - MARGIN);
            contentLabel.setWrappingWidth(wrappingWidth - MARGIN);
            box.setPrefWidth(value);
            box.setMinWidth(value);
            box.setMaxWidth(value);
        }));
        return box;
    }
}

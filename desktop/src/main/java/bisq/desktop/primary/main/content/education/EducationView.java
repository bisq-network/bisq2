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

package bisq.desktop.primary.main.content.education;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class EducationView extends View<VBox, EducationModel, EducationController> {
    private static final int MARGIN = 44;
    private static final int TEXT_SPACE = 22;
    private static final int SCROLLBAR_WIDTH = 12;

    private final Set<Subscription> subscriptions = new HashSet<>();
    @Nullable
    private ChangeListener<Number> widthListener;
    @Nullable
    private Parent parent;

    //todo move out content to a academy screen
    public EducationView(EducationModel model, EducationController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(MARGIN);

        addHeaderBox();
        addSmallBox("dashboard-bisq", "onboarding-1-fraction",
                "bisq", "bitcoin",
                NavigationTarget.BISQ_ACADEMY, NavigationTarget.BITCOIN_ACADEMY);
        addSmallBox("onboarding-1-reputation", "onboarding-2-offer",
                "security", "privacy",
                NavigationTarget.SECURITY_ACADEMY, NavigationTarget.PRIVACY_ACADEMY);
        addSmallBox("onboarding-3-profile", "onboarding-2-chat",
                "wallets", "foss",
                NavigationTarget.WALLETS_ACADEMY, NavigationTarget.FOSS_ACADEMY);

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
                if (parent != null) {
                    ((VBox) parent).widthProperty().addListener(widthListener);
                }
            }
        }
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);

        if (widthListener != null && parent instanceof VBox) {
            ((VBox) parent).widthProperty().removeListener(widthListener);
        }
    }

    private void addHeaderBox() {
        Text headlineLabel = new Text(Res.get("social.education.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-4");

        Text contentLabel = new Text(Res.get("social.education.content"));
        contentLabel.getStyleClass().add("bisq-text-16");

        VBox box = new VBox();
        box.setSpacing(TEXT_SPACE);
        box.getStyleClass().add("bisq-box-2");
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

    private void addSmallBox(String leftIconId,
                             String rightIconId,
                             String leftTopic,
                             String rightTopic,
                             NavigationTarget leftNavigationTarget,
                             NavigationTarget rightNavigationTarget) {
        VBox leftBox = getWidgetBox(
                leftIconId,
                Res.get("social.education." + leftTopic + ".headline"),
                Res.get("social.education." + leftTopic + ".content"),
                Res.get("social.education." + leftTopic + ".button"),
                leftNavigationTarget
        );

        VBox rightBox = getWidgetBox(
                rightIconId,
                Res.get("social.education." + rightTopic + ".headline"),
                Res.get("social.education." + rightTopic + ".content"),
                Res.get("social.education." + rightTopic + ".button"),
                rightNavigationTarget
        );

        HBox box = Layout.hBoxWith(leftBox, rightBox);
        box.setSpacing(MARGIN);
        root.getChildren().add(box);
    }

    private VBox getWidgetBox(String iconId, String headline, String content, String buttonLabel, NavigationTarget navigationTarget) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(iconId));
        headlineLabel.setGraphicTextGap(15);

        Text contentLabel = new Text(content);
        contentLabel.getStyleClass().add("bisq-text-3");

        Button button = new Button(buttonLabel.toUpperCase());
        button.getStyleClass().addAll("text-button", "no-background");
        button.setOnAction(e -> controller.onSelect(navigationTarget));

        HBox.setMargin(button, new Insets(0, 15, 0, 0));
        HBox hBox = new HBox(headlineLabel, Spacer.fillHBox(), button);

        VBox vBox = new VBox(hBox, contentLabel);
        vBox.setOnMouseClicked(e -> controller.onSelect(navigationTarget));
        vBox.setSpacing(TEXT_SPACE);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(MARGIN - 20, 0, MARGIN - 6, MARGIN));
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double value = (w.doubleValue() - root.getPadding().getRight() - MARGIN + SCROLLBAR_WIDTH) / 2;
            double wrappingWidth = value - vBox.getPadding().getLeft() - vBox.getPadding().getRight();
            contentLabel.setWrappingWidth(wrappingWidth - MARGIN);
            vBox.setPrefWidth(value);
            vBox.setMinWidth(value);
            vBox.setMaxWidth(value);
        }));
        return vBox;
    }
}

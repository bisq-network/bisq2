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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
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
public class TradeOverviewView extends View<VBox, TradeOverviewModel, TradeOverviewController> {
    private static final int MARGIN = 44;
    private static final int TEXT_SPACE = 22;
    private static final int SCROLLBAR_WIDTH = 12;

    private final Set<Subscription> subscriptions = new HashSet<>();
    @Nullable
    private ChangeListener<Number> widthListener;
    @Nullable
    private Parent parent;

    public TradeOverviewView(TradeOverviewModel model, TradeOverviewController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(MARGIN);
    }

    @Override
    protected void onViewAttached() {
        addHeaderBox();

        addSmallBox("satoshiSquare",
                true,
                () -> Navigation.navigateTo(NavigationTarget.SATOSHI_SQUARE),
                "liquidSwap",
                false,
                () -> Navigation.navigateTo(NavigationTarget.LIQUID_SWAPS));
        addSmallBox("xmrSwap",
                false,
                () -> Navigation.navigateTo(NavigationTarget.XMR_SWAPS),
                "multiSig",
                false,
                () -> Navigation.navigateTo(NavigationTarget.MULTI_SIG));
        addSmallBox("bsqSwap",
                false,
                () -> Navigation.navigateTo(NavigationTarget.BSQ_SWAPS),
                "lightning",
                false,
                () -> Navigation.navigateTo(NavigationTarget.LIGHTNING));

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
        Text headlineLabel = new Text(Res.get("trade.protocols.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-1");

        Text contentLabel = new Text(Res.get("trade.protocols.content"));
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


    private void addSmallBox(String leftTopic,
                             boolean leftIsDeployed,
                             Runnable leftActionHandler,
                             String rightTopic,
                             boolean rightIsDeployed,
                             Runnable rightActionHandler
    ) {
        VBox leftBox = getWidgetBox(Res.get("trade.protocols." + leftTopic + ".protocolName"),
                Res.get("trade.protocols." + leftTopic + ".description"),
                Res.get("trade.protocols." + leftTopic + ".supportedMarkets"),
                Res.get("trade.protocols." + leftTopic + ".requirements"),
                leftIsDeployed ? null : Res.get("trade.protocols." + leftTopic + ".estimatedDeployment"),
                Res.get("trade.protocols." + leftTopic + ".action"),
                leftActionHandler);

        VBox rightBox = getWidgetBox(Res.get("trade.protocols." + rightTopic + ".protocolName"),
                Res.get("trade.protocols." + rightTopic + ".description"),
                Res.get("trade.protocols." + rightTopic + ".supportedMarkets"),
                Res.get("trade.protocols." + rightTopic + ".requirements"),
                rightIsDeployed ? null : Res.get("trade.protocols." + rightTopic + ".estimatedDeployment"),
                Res.get("trade.protocols." + rightTopic + ".action"),
                rightActionHandler);

        HBox box = Layout.hBoxWith(leftBox, rightBox);
        box.setSpacing(MARGIN);
        root.getChildren().add(box);
    }

    private VBox getWidgetBox(String protocolName,
                              String description,
                              String supportedMarkets,
                              String requirements,
                              @Nullable String estimatedDeployment,
                              String buttonText,
                              Runnable actionHandler) {
        Text protocolNameLabel = new Text(protocolName);
        protocolNameLabel.getStyleClass().add("bisq-text-headline-2");

        Text descriptionLabel = new Text(description);
        descriptionLabel.getStyleClass().add("bisq-text-1");

        String COLON = ": ";
        Text supportedMarketsLabel = new Text(Res.get("trade.protocols.supportedMarkets") + COLON + supportedMarkets);
        supportedMarketsLabel.getStyleClass().add("bisq-text-1");

        Text requirementsLabel = new Text(Res.get("trade.protocols.requirements") + COLON + requirements);
        requirementsLabel.getStyleClass().add("bisq-text-1");

        Button button = new Button(buttonText);
        button.setOnAction(e -> actionHandler.run());
        button.getStyleClass().add("bisq-border-dark-bg-button");

        VBox box = Layout.vBoxWith(protocolNameLabel,
                descriptionLabel,
                supportedMarketsLabel,
                requirementsLabel,
                button);

        Text estimatedDeploymentLabel = new Text(Res.get("trade.protocols.estimatedDeployment") + COLON + estimatedDeployment);
        estimatedDeploymentLabel.getStyleClass().add("bisq-text-1");
        if (estimatedDeployment != null) {
            box.getChildren().add(4, estimatedDeploymentLabel);
        }

        box.setSpacing(TEXT_SPACE);
        box.getStyleClass().add("bisq-box-1");
        box.setPadding(new Insets(MARGIN - 16, 0, MARGIN - 6, MARGIN));
        subscriptions.add(EasyBind.subscribe(root.widthProperty(), w -> {
            double value = (w.doubleValue() - root.getPadding().getRight() - MARGIN + SCROLLBAR_WIDTH) / 2;
            double wrappingWidth = value - box.getPadding().getLeft() - box.getPadding().getRight();
            protocolNameLabel.setWrappingWidth(wrappingWidth - MARGIN);
            descriptionLabel.setWrappingWidth(wrappingWidth - MARGIN);
            supportedMarketsLabel.setWrappingWidth(wrappingWidth - MARGIN);
            requirementsLabel.setWrappingWidth(wrappingWidth - MARGIN);
            estimatedDeploymentLabel.setWrappingWidth(wrappingWidth - MARGIN);
            box.setPrefWidth(value);
            box.setMinWidth(value);
            box.setMaxWidth(value);
        }));
        return box;
    }
}

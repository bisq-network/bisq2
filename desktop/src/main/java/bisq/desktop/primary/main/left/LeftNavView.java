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

package bisq.desktop.primary.main.left;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class LeftNavView extends View<VBox, LeftNavModel, LeftNavController> {
    private final static int EXPANDED_WIDTH = 215;
    private final static int COLLAPSED_WIDTH = 95;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final Map<NavigationTarget, NavigationButton> buttonsMap = new HashMap<>();
    private final Label expandIcon, collapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Pane expandMenuIcons;
    private Subscription navigationTargetSubscription, menuExpandedSubscription;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new VBox(), model, controller);

        root.setStyle("-fx-background-color: green");
        root.setSpacing(10);
        root.setPadding(new Insets(15, 30, 15, 15));
        root.setStyle("-fx-background-color: -bisq-menu-bg;; -fx-background-insets: 0 15 0 0");

        NavigationButton trade = createNavigationButton(Res.get("trade"), AwesomeIcon.EXCHANGE, NavigationTarget.TRADE);
        NavigationButton portfolio = createNavigationButton(Res.get("portfolio"), AwesomeIcon.STACKEXCHANGE, NavigationTarget.PORTFOLIO);
        NavigationButton education = createNavigationButton(Res.get("education"), AwesomeIcon.BOOK, NavigationTarget.EDUCATION);
        NavigationButton social = createNavigationButton(Res.get("social"), AwesomeIcon.GROUP, NavigationTarget.SOCIAL);
        NavigationButton events = createNavigationButton(Res.get("events"), AwesomeIcon.CALENDAR, NavigationTarget.EVENTS);
        social.setOnAction(() -> {
            controller.onNavigationTargetSelected(NavigationTarget.SOCIAL);
            if (model.getMenuExpanded().get()) {
                controller.onToggleExpandMenu();
            }
        });
        NavigationButton markets = createNavigationButton(Res.get("markets"), AwesomeIcon.BAR_CHART, NavigationTarget.MARKETS);
        NavigationButton wallet = createNavigationButton(Res.get("wallet"), AwesomeIcon.MONEY, NavigationTarget.WALLET);
        NavigationButton settings = createNavigationButton(Res.get("settings"), AwesomeIcon.GEARS, NavigationTarget.SETTINGS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        networkInfoBox = new NetworkInfoBox(model, () -> controller.onNavigationTargetSelected(NavigationTarget.NETWORK_INFO));
        model.addNavigationTarget(NavigationTarget.NETWORK_INFO);

        expandIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_RIGHT, "24");
        expandIcon.setCursor(Cursor.HAND);
        collapseIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_LEFT, "24");
        collapseIcon.setCursor(Cursor.HAND);
        expandMenuIcons = new Pane();
        expandMenuIcons.getChildren().addAll(expandIcon, collapseIcon);
        expandMenuIcons.setOpacity(0.3);
      /*  expandMenuIcons.setPadding(new Insets(0, 0, -18, 0));
        VBox.setMargin(expandMenuIcons, new Insets(12, 0, 25, 0));*/
       // expandMenuIcons.setPadding(new Insets(0, 0, -18, 0));
        VBox.setMargin(expandMenuIcons, new Insets(12+35, 0, -10, 0));
        logoExpanded = ImageUtil.getImageViewById("bisq-logo");
        logoCollapsed = ImageUtil.getImageViewById("bisq-logo-mark");

        root.getChildren().addAll(logoExpanded, logoCollapsed, expandMenuIcons,
                trade, portfolio, education, social, events, markets, wallet, settings,
                spacer, networkInfoBox);
    }

    @Override
    protected void onViewAttached() {
        expandIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        collapseIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        navigationTargetSubscription = EasyBind.subscribe(model.getNavigationTarget(), navigationTarget -> {
            if (navigationTarget != null) {
                Optional.ofNullable(buttonsMap.get(navigationTarget)).ifPresent(toggleGroup::selectToggle);
            }
        });
        menuExpandedSubscription = EasyBind.subscribe(model.getMenuExpanded(), menuExpanded -> {
            int width = menuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            root.setMaxWidth(width);
            root.setMinWidth(width);
            expandIcon.setLayoutX(width - 40);
            collapseIcon.setLayoutX(width - 40);
            buttonsMap.values().forEach(e -> e.setMenuExpanded(menuExpanded, width - 45));


            networkInfoBox.setVisible(menuExpanded);
            networkInfoBox.setManaged(menuExpanded);

            logoExpanded.setVisible(menuExpanded);
            logoExpanded.setManaged(menuExpanded);
            logoCollapsed.setVisible(!menuExpanded);
            logoCollapsed.setManaged(!menuExpanded);

            collapseIcon.setVisible(menuExpanded);
            collapseIcon.setManaged(menuExpanded);
            expandIcon.setVisible(!menuExpanded);
            expandIcon.setManaged(!menuExpanded);
        });
    }

    @Override
    protected void onViewDetached() {
        expandIcon.setOnMouseClicked(null);
        collapseIcon.setOnMouseClicked(null);
        navigationTargetSubscription.unsubscribe();
        menuExpandedSubscription.unsubscribe();
    }

    private NavigationButton createNavigationButton(String title, AwesomeIcon awesomeIcon, NavigationTarget navigationTarget) {
        NavigationButton button = new NavigationButton(title, awesomeIcon, toggleGroup);
        button.setOnAction(() -> controller.onNavigationTargetSelected(navigationTarget));
        buttonsMap.put(navigationTarget, button);
        model.addNavigationTarget(navigationTarget);
        return button;
    }

    private static class NetworkInfoBox extends VBox {
        private NetworkInfoBox(LeftNavModel model, Runnable handler) {
            setSpacing(5);
            setPadding(new Insets(10, 10, 10, 10));
            setOnMouseClicked(e -> handler.run());

            HBox clearNetBox = getTransportTypeBox("clear-net",
                    model.getClearNetNumConnections(),
                    model.getClearNetNumTargetConnections(),
                    model.getClearNetIsVisible(),
                    new Insets(1, 0, 5, 3));

            HBox torBox = getTransportTypeBox("tor",
                    model.getTorNumConnections(),
                    model.getTorNumTargetConnections(),
                    model.getTorIsVisible(),
                    new Insets(-6, 0, 5, 1));

            HBox i2pBox = getTransportTypeBox("i2p",
                    model.getI2pNumConnections(),
                    model.getI2pNumTargetConnections(),
                    model.getI2pIsVisible(),
                    new Insets(0, 0, 5, 0));

            getChildren().addAll(clearNetBox, torBox, i2pBox);
        }

        private HBox getTransportTypeBox(String iconId,
                                         StringProperty numConnections,
                                         StringProperty numTargetConnections,
                                         BooleanProperty isVisible,
                                         Insets iconMargin) {
            HBox hBox = new HBox();
            hBox.setSpacing(5);
            hBox.setMinHeight(20);
            hBox.setMaxHeight(hBox.getMinHeight());
            hBox.managedProperty().bind(isVisible);
            hBox.visibleProperty().bind(isVisible);

            Label peers = new Label(Res.get("peers"));

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.textProperty().bind(numConnections);

            Label separator = new Label("|");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.textProperty().bind(numTargetConnections);

            ImageView icon = new ImageView();
            icon.setId(iconId);

            HBox.setMargin(icon, iconMargin);

            hBox.getChildren().addAll(peers, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);
            return hBox;
        }
    }
}

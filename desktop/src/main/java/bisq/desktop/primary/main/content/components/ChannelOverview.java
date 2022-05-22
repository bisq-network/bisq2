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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.utils.Icons;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PublicTradeChannel;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

public class ChannelOverview {
    private final Controller controller;

    public ChannelOverview(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final ChatService chatService;

        private Controller(DefaultApplicationService applicationService) {
            chatService = applicationService.getChatService();
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.getFilteredItems().setPredicate(e -> e.getChannel().isVisible());
            model.getListItems().setAll(chatService.getPublicTradeChannels().stream()
                    .map(ListItem::new)
                    .collect(Collectors.toList()));
        }

        @Override
        public void onDeactivate() {
        }

        public void onSelect(ListItem item) {
            chatService.selectTradeChannel(item.getChannel());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        final ObservableList<ListItem> listItems = FXCollections.observableArrayList();
        final FilteredList<ListItem> filteredItems = new FilteredList<>(listItems);
        final SortedList<ListItem> sortedItems = new SortedList<>(filteredItems);
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {
        private static final int VERTICAL_MARGIN = 30;

        public View(Model model, Controller controller) {
            super(new GridPane(), model, controller);

            getRoot().setHgap(26);
            getRoot().setVgap(26);
        }

        @Override
        protected void onViewAttached() {
            getRoot().getChildren().clear();
            int index = 0;
            for (ListItem items : model.getSortedItems()) {
                getRoot().add(getBox(items), index % 2, index >> 1);
                index++;
            }
        }

        @Override
        protected void onViewDetached() {
        }

        private Pane getBox(ListItem listItem) {
            StackPane pane = new StackPane();
            pane.getStyleClass().add("bisq-box-2");
            pane.setMinWidth(360);
            GridPane.setHgrow(pane, Priority.ALWAYS);

            VBox box = new VBox();
            pane.getChildren().add(box);

            Label headlineLabel = new Label(listItem.getDisplayString());
            headlineLabel.setPadding(new Insets(16, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
            headlineLabel.getStyleClass().add("bisq-text-headline-2");
            // headlineLabel.setGraphic(ImageUtil.getImageViewById(listItem.getIconId()));
            box.getChildren().add(headlineLabel);

           /* Label basicInfo = new Label(listItem.getBasicInfo());
            basicInfo.getStyleClass().addAll("bisq-text-3", "wrap-text");
            basicInfo.setPadding(new Insets(4, VERTICAL_MARGIN, 0, VERTICAL_MARGIN));
            basicInfo.setAlignment(Pos.TOP_LEFT);
            basicInfo.setMaxWidth(384);
            basicInfo.setMinHeight(60);
            box.getChildren().add(basicInfo);*/

            Region line = new Region();
            line.getStyleClass().addAll("border-bottom");
            box.getChildren().add(line);

            GridPane paramsPane = new GridPane();
            paramsPane.setPadding(new Insets(24, VERTICAL_MARGIN, 20, VERTICAL_MARGIN));
            paramsPane.setVgap(12);
           /* paramsPane.add(
                    getParameterPane(Res.get("markets"), listItem.getMarkets()),
                    0,
                    0
            );

            paramsPane.add(
                    getParameterPane(
                            Res.get("trade.protocols.table.header.release"),
                            listItem.getReleaseDate()
                    ),
                    1,
                    0
            );

            paramsPane.add(
                    getParameterPane(
                            Res.get("trade.protocols.table.header.security"),
                            listItem.getSwapProtocolType().getSecurity().ordinal(),
                            listItem.getSecurityInfo()
                    ),
                    0,
                    1
            );

            paramsPane.add(
                    getParameterPane(
                            Res.get("trade.protocols.table.header.privacy"),
                            listItem.getSwapProtocolType().getPrivacy().ordinal(),
                            listItem.getPrivacyInfo()
                    ),
                    1,
                    1
            );

            paramsPane.add(
                    getParameterPane(
                            Res.get("trade.protocols.table.header.convenience"),
                            listItem.getSwapProtocolType().getConvenience().ordinal(),
                            listItem.getConvenienceInfo()
                    ),
                    2,
                    1
            );*/

            box.getChildren().add(paramsPane);
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> controller.onSelect(listItem));

            Button button = new Button(Res.get("select"));
            button.getStyleClass().setAll("bisq-transparent-button", "bisq-text-3");
            button.setOnAction(e -> controller.onSelect(listItem));
            StackPane.setAlignment(button, Pos.TOP_RIGHT);
            StackPane.setMargin(button, new Insets(20, 14, 0, 0));
            pane.getChildren().add(button);

            return pane;
        }


        private VBox getParameterPane(String title, String info) {
            Label infoLabel = new Label(info);
            infoLabel.getStyleClass().add("bisq-text-1");
            return getParameterPane(title, infoLabel);
        }

        private VBox getParameterPane(String title, int value, String tooltipText) {
            return getParameterPane(title, getStarsNode(value, tooltipText));
        }

        private VBox getParameterPane(String title, Node node) {
            VBox box = new VBox();
            GridPane.setHgrow(box, Priority.ALWAYS);
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().add("bisq-text-4");
            box.getChildren().addAll(titleLabel, node);

            return box;
        }

        private Node getStarsNode(int value, String tooltipText) {
            final HBox hBox = new HBox();
            hBox.setPadding(new Insets(5, 0, 5, 0));
            hBox.setSpacing(5);

            for (int i = 0; i < 3; i++) {
                Label label = Icons.getIcon(AwesomeIcon.STAR, "1.2em");
                label.setMouseTransparent(false);
                label.setOpacity(i <= value ? 1 : 0.2);
                hBox.getChildren().add(label);
            }

            final Tooltip tooltip = new Tooltip();
            tooltip.setMaxWidth(240);
            tooltip.setWrapText(true);
            tooltip.setText(tooltipText);
            Tooltip.install(hBox, tooltip);

            return hBox;
        }
    }

    @EqualsAndHashCode
    @Getter
    static class ListItem {
        private final PublicTradeChannel channel;

        public ListItem(PublicTradeChannel channel) {
            this.channel = channel;
        }

        public String getDisplayString() {
            return channel.getMarket().map(Market::toString).orElse(Res.get("tradeChat.addMarketChannel.any"));
        }
    }
}

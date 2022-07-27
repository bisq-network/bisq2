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

package bisq.desktop.primary.main.content.chat;

import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class ChatView extends NavigationView<HBox, ChatModel, ChatController<?, ?>> {
    private final Label headline;
    private final Button helpButton;
    private final VBox left;
    private final VBox sideBar;
    protected final Pane chatMessagesComponent;
    private final Pane channelInfo;
    protected final HBox centerToolbar;
    private final Button createOfferButton;
    protected final VBox center;
    private final SearchBox searchBox;
    private Pane chatUserOverviewRoot;
    private Subscription chatUserOverviewRootSubscription;
    private Subscription channelIconPin;

    public ChatView(ChatModel model,
                    ChatController<?, ?> controller,
                    Pane marketChannelSelection,
                    Pane privateChannelSelection,
                    Pane chatMessagesComponent,
                    Pane channelInfo) {
        super(new HBox(), model, controller);
        this.chatMessagesComponent = chatMessagesComponent;

        this.channelInfo = channelInfo;

        // Undo default padding of ContentView 
        root.setPadding(new Insets(-34, -67, -67, -68));

        createOfferButton = new Button(Res.get("satoshisquareapp.chat.createOffer.button"));
        createOfferButton.setMaxWidth(Double.MAX_VALUE);
        createOfferButton.setMinHeight(37);
        createOfferButton.setDefaultButton(true);

        VBox.setMargin(createOfferButton, new Insets(-2, 24, 17, 24));

        // Left
        left = Layout.vBoxWith(
                marketChannelSelection,
                Layout.separator(),
                privateChannelSelection,
                Spacer.fillVBox(),
                createOfferButton
        );
        left.getStyleClass().add("bisq-grey-2-bg");
        left.setPrefWidth(210);
        left.setMinWidth(210);

        // Center toolbar
        headline = new Label();
        headline.setId("chat-messages-headline");
        HBox.setMargin(headline, new Insets(0, 0, 0, 0));

        searchBox = new SearchBox();
        searchBox.setPrefWidth(200);
        helpButton = BisqIconButton.createIconButton("icon-help", Res.get("help"));

        centerToolbar = new HBox(
                10,
                headline,
                Spacer.fillHBox(),
                searchBox,
                helpButton
        );
        centerToolbar.setAlignment(Pos.CENTER);
        centerToolbar.setMinHeight(64);
        centerToolbar.setPadding(new Insets(0, 20, 0, 24));

        // sideBar
        sideBar = new VBox(channelInfo);
        sideBar.getStyleClass().add("bisq-grey-2-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        center = new VBox(centerToolbar, chatMessagesComponent);
        chatMessagesComponent.setMinWidth(700);
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(center, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        root.getChildren().addAll(left, center, sideBar);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getSelectedChannelAsString());
        channelInfo.visibleProperty().bind(model.getChannelInfoVisible());
        channelInfo.managedProperty().bind(model.getChannelInfoVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());
        createOfferButton.visibleProperty().bind(model.getCreateOfferButtonVisible());
        createOfferButton.managedProperty().bind(model.getCreateOfferButtonVisible());

        helpButton.setOnAction(e -> controller.onToggleHelp());
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        chatUserOverviewRootSubscription = EasyBind.subscribe(model.getChatUserDetailsRoot(),
                pane -> {
                    if (chatUserOverviewRoot != null) {
                        sideBar.getChildren().remove(chatUserOverviewRoot);
                        chatUserOverviewRoot = null;
                    }

                    if (pane != null) {
                        sideBar.getChildren().add(pane);
                        chatUserOverviewRoot = pane;
                    }
                });

        channelIconPin = EasyBind.subscribe(model.getChannelIcon(), icon -> {
            if (icon != null) {
                headline.setGraphic(icon);
                headline.setGraphicTextGap(8);
                icon.setStyle("-fx-cursor: hand;");
                icon.setOnMouseClicked(e -> controller.onToggleChannelInfo());
            }
        });
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        channelInfo.visibleProperty().unbind();
        channelInfo.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();
        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();

        helpButton.setOnAction(null);
        createOfferButton.setOnAction(null);
        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        chatUserOverviewRootSubscription.unsubscribe();
        channelIconPin.unsubscribe();
    }
}

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

package bisq.desktop.main.content.chat;

import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.SearchBox;
import bisq.i18n.Res;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class BaseChatView extends NavigationView<ScrollPane, BaseChatModel, BaseChatController<?, ?>> {
    public final static double HEADER_HEIGHT = 61;

    protected final Label channelTitle = new Label();
    protected final Label channelDescription = new Label();
    protected final Label channelIcon = new Label();
    protected final VBox sideBar = new VBox();
    protected final VBox centerVBox = new VBox();
    protected final HBox titleHBox = new HBox(10);
    protected final HBox containerHBox = new HBox();
    protected final Pane channelSidebar, chatMessagesComponent;
    protected final SearchBox searchBox = new SearchBox();
    protected final DropdownMenu ellipsisMenu = new DropdownMenu("ellipsis-v-grey", "ellipsis-v-white", true);
    protected final DropdownMenu notificationsSettingsMenu = new DropdownMenu("icon-bell-grey", "icon-bell-white", true);
    protected DropdownBisqMenuItem helpButton, infoButton;
    private NotificationSettingMenuItem globalDefault, all, mention, off;
    protected Pane chatUserOverviewRoot;
    protected Subscription channelIconPin, chatUserOverviewRootSubscription, selectedNotificationSettingPin;

    public BaseChatView(BaseChatModel model,
                        BaseChatController<?, ?> controller,
                        Pane chatMessagesComponent,
                        Pane channelSidebar) {
        super(new ScrollPane(), model, controller);

        this.chatMessagesComponent = chatMessagesComponent;
        this.channelSidebar = channelSidebar;

        setUpEllipsisMenu();
        setupNotificationsSettingMenu();

        configTitleHBox();
        configCenterVBox();
        configSideBarVBox();
        configContainerHBox();

        root.setFitToWidth(true);
        root.setFitToHeight(true);
    }

    protected abstract void configTitleHBox();

    protected abstract void configCenterVBox();

    protected abstract void configSideBarVBox();

    protected abstract void configContainerHBox();

    @Override
    protected void onViewAttached() {
        channelTitle.textProperty().bind(model.getChannelTitle());
        channelDescription.textProperty().bind(model.getChannelDescription());
        channelSidebar.visibleProperty().bind(model.getChannelSidebarVisible());
        channelSidebar.managedProperty().bind(model.getChannelSidebarVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setLabelText(controller.getHelpButtonText());
            helpButton.setOnAction(e -> controller.onOpenHelp());
        }
        if (infoButton != null) {
            infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        }
        if (globalDefault != null) {
            globalDefault.setOnAction(e -> controller.onSetNotificationType(globalDefault.getType()));
        }
        if (all != null) {
            all.setOnAction(e -> controller.onSetNotificationType(all.getType()));
        }
        if (mention != null) {
            mention.setOnAction(e -> controller.onSetNotificationType(mention.getType()));
        }
        if (off != null) {
            off.setOnAction(e -> controller.onSetNotificationType(off.getType()));
        }

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

        channelIconPin = EasyBind.subscribe(model.getChannelIconId(), channelIconId -> {
            ImageView image = ImageUtil.getImageViewById(channelIconId);
            image.setScaleX(1.25);
            image.setScaleY(1.25);
            channelIcon.setGraphic(image);
        });

        selectedNotificationSettingPin = EasyBind.subscribe(model.getSelectedNotificationSetting(),
                this::applySelectedNotificationSetting);
    }

    @Override
    protected void onViewDetached() {
        channelTitle.textProperty().unbind();
        channelDescription.textProperty().unbind();
        channelSidebar.visibleProperty().unbind();
        channelSidebar.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setOnAction(null);
        }
        if (infoButton != null) {
            infoButton.setOnAction(null);
        }
        if (globalDefault != null) {
            globalDefault.dispose();
        }
        if (all != null) {
            all.dispose();
        }
        if (mention != null) {
            mention.dispose();
        }
        if (off != null) {
            off.dispose();
        }

        chatUserOverviewRootSubscription.unsubscribe();
        channelIconPin.unsubscribe();
        selectedNotificationSettingPin.unsubscribe();
    }

    private void setUpEllipsisMenu() {
        helpButton = new DropdownBisqMenuItem("icon-help-grey", "icon-help-white");
        infoButton = new DropdownBisqMenuItem("icon-info-grey", "icon-info-white",
                Res.get("chat.ellipsisMenu.channelInfo"));
        ellipsisMenu.addMenuItems(helpButton, infoButton);
        ellipsisMenu.setTooltip(Res.get("chat.ellipsisMenu.tooltip"));
    }

    private void setupNotificationsSettingMenu() {
        globalDefault = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.globalDefault"), ChatChannelNotificationType.GLOBAL_DEFAULT);
        all = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.all"), ChatChannelNotificationType.ALL);
        mention = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.mention"), ChatChannelNotificationType.MENTION);
        off = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.off"), ChatChannelNotificationType.OFF);
        notificationsSettingsMenu.addMenuItems(globalDefault, all, mention, off);
        notificationsSettingsMenu.setTooltip(Res.get("chat.notificationsSettingsMenu.tooltip"));
    }

    private void applySelectedNotificationSetting(ChatChannelNotificationType type) {
        globalDefault.updateSelection(type == globalDefault.getType());
        all.updateSelection(type == all.getType());
        mention.updateSelection(type == mention.getType());
        off.updateSelection(type == off.getType());
    }

    @Getter
    private static final class NotificationSettingMenuItem extends DropdownBisqMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final ChatChannelNotificationType type;

        private NotificationSettingMenuItem(String defaultIconId, String activeIconId, String text, ChatChannelNotificationType type) {
            super(defaultIconId, activeIconId, text);

            this.type = type;
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
            initialize();
        }

        public void initialize() {
        }

        public void dispose() {
            setOnAction(null);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }
    }
}

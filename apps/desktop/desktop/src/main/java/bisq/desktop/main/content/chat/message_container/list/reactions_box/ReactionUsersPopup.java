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

package bisq.desktop.main.content.chat.message_container.list.reactions_box;

import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ReactionItem;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

public class ReactionUsersPopup extends PopupControl {
    private static final int MAX_USERS_SHOWN = 99;
    private static final int MAX_USERS_SHOWN_AT_THE_SAME_TIME = 2; //7
    private static final double CELL_SIZE = 30;
    private static final double MARGIN = 5;
    private static final double LIST_VIEW_WIDTH = 150;

    private final BisqMenuItem owner;
    private final boolean isMyMessage;
    private final ObservableList<UserProfile> userProfileList = FXCollections.observableArrayList();
    private final ListView<UserProfile> userProfileListView = new ListView<>(userProfileList);
    private final VBox popupContent = new VBox();
    @Getter
    private final StackPane root = new StackPane();
    @Getter
    @Setter
    protected Node contentNode;

    public ReactionUsersPopup(ReactionItem reactionItem, BisqMenuItem owner, boolean isMyMessage) {
        this.owner = owner;
        this.isMyMessage = isMyMessage;
        userProfileList.setAll(reactionItem.getUsersByReactionDate().stream()
                .limit(MAX_USERS_SHOWN)
                .map(ReactionItem.UserWithReactionDate::getUserProfile)
                .collect(Collectors.toList()));
        userProfileListView.setCellFactory(getCellFactory());
        userProfileListView.setFixedCellSize(CELL_SIZE);
        userProfileListView.setMaxWidth(LIST_VIEW_WIDTH);
        userProfileListView.setPrefWidth(LIST_VIEW_WIDTH);
        userProfileListView.setMinWidth(LIST_VIEW_WIDTH);

        ImageView reactionIcon = ImageUtil.getImageViewById(reactionItem.getIconId());
        Label label = new Label(Res.get("chat.message.reactionPopup"), reactionIcon);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.setGraphicTextGap(5);
        popupContent.getStyleClass().add("reaction-users-popup");
        popupContent.getChildren().addAll(userProfileListView, label);
        setContentNode(popupContent);
        setAutoHide(true);
        initialize();
    }

    private void initialize() {
        double listViewPadding = 20;
        userProfileListView.prefHeightProperty().bind(userProfileListView.fixedCellSizeProperty()
                .multiply(Math.min(userProfileList.size(), MAX_USERS_SHOWN_AT_THE_SAME_TIME)).add(listViewPadding));

        owner.setOnMouseEntered(e -> {
            showPopup();
        });
        owner.setOnMouseExited(e -> {
           if (hasMouseExited(e.getScreenX(), e.getScreenY())) {
               hide();
           }
        });
        popupContent.setOnMouseExited(e -> {
            if (hasMouseExited(e.getScreenX(), e.getScreenY())) {
                hide();
            }
        });
    }

    private void showPopup() {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        double padding = 22;
        show(owner, isMyMessage ? bounds.getMinX() - LIST_VIEW_WIDTH - padding - MARGIN : bounds.getMaxX() + MARGIN,
                bounds.getMinY());
    }

    public void dispose() {
        userProfileListView.prefHeightProperty().unbind();

        owner.setOnMouseEntered(null);
        owner.setOnMouseExited(null);
        popupContent.setOnMouseExited(null);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ReactionPopupSkin(this);
    }

    private boolean hasMouseExited(double mouseX, double mouseY) {
        boolean inPopupBounds = getRoot().contains(mouseX - getX(), mouseY - getY());
        Bounds buttonBounds = owner.localToScreen(owner.getBoundsInLocal());
        boolean inAreaBetweenButtonAndPopup;

        if (isMyMessage) {
            inAreaBetweenButtonAndPopup = (mouseX >= buttonBounds.getMinX() - MARGIN)
                    && mouseX <= buttonBounds.getMinX()
                    && mouseY >= buttonBounds.getMinY()
                    && mouseY <= buttonBounds.getMaxY();
        } else {
            inAreaBetweenButtonAndPopup = mouseX >= buttonBounds.getMaxX()
                    && mouseX <= (buttonBounds.getMaxX() + MARGIN)
                    && mouseY >= buttonBounds.getMinY()
                    && mouseY <= buttonBounds.getMaxY();
        }
        return !(inPopupBounds || inAreaBetweenButtonAndPopup);
    }

    private Callback<ListView<UserProfile>, ListCell<UserProfile>> getCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<UserProfile> call(ListView<UserProfile> list) {
                return new ListCell<>() {
                    private final UserProfileIcon userProfileIcon = new UserProfileIcon(CELL_SIZE - 10);
                    private final Label userNickname = new Label();
                    final HBox hBox = new HBox(10);

                    {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        hBox.setFillHeight(true);
                        userNickname.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");
                    }

                    @Override
                    public void updateItem(UserProfile item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            userProfileIcon.setUserProfile(item);
                            userProfileIcon.hideLastSeenDot();
                            userNickname.setText(StringUtils.truncate(item.getNickName(), 15));
                            hBox.getChildren().setAll(userProfileIcon, userNickname);
                            setGraphic(hBox);
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    @Getter
    private static final class ReactionPopupSkin implements Skin<ReactionUsersPopup> {
        private final ReactionUsersPopup skinnable;

        public ReactionPopupSkin(final ReactionUsersPopup popup) {
            this.skinnable = popup;
            popup.getRoot().getChildren().add(popup.getContentNode());
            Bindings.bindContent(popup.getRoot().getStyleClass(), popup.getStyleClass());
        }

        @Override
        public ReactionUsersPopup getSkinnable() {
            return skinnable;
        }

        @Override
        public Node getNode() {
            return skinnable.getRoot();
        }

        @Override
        public void dispose() {
            skinnable.dispose();
        }
    }
}

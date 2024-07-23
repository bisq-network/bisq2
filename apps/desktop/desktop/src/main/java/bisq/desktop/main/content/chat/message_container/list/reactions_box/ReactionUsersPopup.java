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
import bisq.user.profile.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Callback;

import java.util.stream.Collectors;

public class ReactionUsersPopup extends Popup {
    private static final int MAX_USERS_SHOWN = 99;

    private final ReactionItem reactionItem;
    private final BisqMenuItem owner;
    private final ImageView reactionIcon;
    private final ObservableList<UserProfile> userProfileList = FXCollections.observableArrayList();
    private final ListView<UserProfile> userProfileListView = new ListView<>(userProfileList);
    private final VBox popupContent = new VBox();

    public ReactionUsersPopup(ReactionItem reactionItem, BisqMenuItem owner) {
        this.reactionItem = reactionItem;
        this.owner = owner;
        reactionIcon = ImageUtil.getImageViewById(reactionItem.getIconId());
        userProfileList.setAll(reactionItem.getUsers().stream().limit(MAX_USERS_SHOWN).collect(Collectors.toList()));
        userProfileListView.setCellFactory(getCellFactory());
        initialize();

        popupContent.getChildren().addAll(userProfileListView, reactionIcon);
        getContent().add(popupContent);
    }

    private void initialize() {
        owner.setOnMouseEntered(e -> {
            Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
            show(owner, bounds.getMaxX(), bounds.getMinY());
        });
        owner.setOnMouseExited(e -> {
           if (!popupContent.contains(e.getScreenX() - getX(), e.getScreenY() - getY())) {
               hide();
           }
        });
        popupContent.setOnMouseExited(e -> hide());
    }

    public void dispose() {
        owner.setOnMouseEntered(null);
        owner.setOnMouseExited(null);
        popupContent.setOnMouseExited(null);
    }

    private Callback<ListView<UserProfile>, ListCell<UserProfile>> getCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<UserProfile> call(ListView<UserProfile> list) {
                return new ListCell<>() {
                    private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
                    private final VBox userProfileIconVbox = new VBox(userProfileIcon);
                    private final Label userNickname = new Label();
                    final HBox hBox = new HBox(10);

                    {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        hBox.setFillHeight(true);
//                        hBox.setPadding(new Insets(0, 10, 0, 0));
                        hBox.setCursor(Cursor.HAND);
                    }

                    @Override
                    public void updateItem(UserProfile item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            userProfileIcon.setUserProfile(item);
                            userNickname.setText(StringUtils.truncate(item.getNickName(), 28));

                            //hBox.setOnMouseClicked(); // open user profile on the side
                            hBox.getChildren().setAll(userProfileIconVbox, userNickname);
                            setGraphic(hBox);
                        } else {
                            //hBox.setOnMouseClicked(null);
                            setGraphic(null);
                        }
                    }

                };
            }
        };
    }
}

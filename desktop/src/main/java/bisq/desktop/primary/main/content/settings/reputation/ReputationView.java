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

package bisq.desktop.primary.main.content.settings.reputation;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.primary.main.content.components.ChatUserIcon;
import bisq.desktop.primary.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class ReputationView extends View<VBox, ReputationModel, ReputationController> {
    private final Button burnBsqButton, bsqBondButton, accountAgeButton, signedAccountButton;
    private final Hyperlink learnMore;
    private final BisqTableView<ListItem> tableView;

    public ReputationView(ReputationModel model,
                          ReputationController controller) {
        super(new VBox(20), model, controller);

        Label headlineLabel = new Label(Res.get("reputation.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-5");

        Label infoLabel = new Label(Res.get("reputation.info"));
        infoLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");
        infoLabel.setMinHeight(220);

        burnBsqButton = new Button(Res.get("reputation.burnBsq"));
        bsqBondButton = new Button(Res.get("reputation.bond"));
        accountAgeButton = new Button(Res.get("reputation.accountAge"));
        signedAccountButton = new Button(Res.get("reputation.signedAccount"));

        learnMore = new Hyperlink(Res.get("reputation.learnMore"));

        HBox buttons = new HBox(20, burnBsqButton, bsqBondButton, accountAgeButton, signedAccountButton);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(learnMore, new Insets(0, 0, 10, 0));
        VBox vBox = new VBox(16, headlineLabel, infoLabel, learnMore, buttons);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(30));
        vBox.setAlignment(Pos.TOP_LEFT);

        Label tableHeadline = new Label(Res.get("reputation.table.headline"));
        tableHeadline.getStyleClass().add("bisq-content-headline-label");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("create-offer-table-view");
        tableView.setMinHeight(300);
        // Triggers to fill the available height
        tableView.setPrefHeight(2000);
        configTableView();

        VBox.setMargin(vBox, new Insets(30, 0, 20, 0));
        VBox.setVgrow(vBox, Priority.SOMETIMES);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(vBox, tableHeadline, tableView);
    }


    @Override
    protected void onViewAttached() {

        burnBsqButton.setOnAction(e -> controller.onBurnBsq());
        bsqBondButton.setOnAction(e -> controller.onBsqBond());
        accountAgeButton.setOnAction(e -> controller.onAccountAge());
        signedAccountButton.setOnAction(e -> controller.onSignedAccount());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        burnBsqButton.setOnAction(null);
        bsqBondButton.setOnAction(null);
        accountAgeButton.setOnAction(null);
        signedAccountButton.setOnAction(null);
        learnMore.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.userProfile"))
                .isFirst()
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.profileAge"))
                .isFirst()
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .build());
        BisqTableColumn<ListItem> scoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.reputationScore"))
                .comparator(Comparator.comparing(ListItem::getTotalScore).reversed())
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        tableView.getColumns().add(scoreColumn);
        tableView.getSortOrder().add(scoreColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.reputation"))
                .comparator(Comparator.comparing(ListItem::getTotalScore).reversed())
                .setCellFactory(getStarsCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("reputation.table.columns.details"))
                .setCellFactory(getDetailsCellFactory())
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final ChatUserIcon chatUserIcon = new ChatUserIcon(40);
            private final HBox hBox = new HBox(10, chatUserIcon, userName);

            {
                userName.setId("chat-user-name");

                chatUserIcon.setMinWidth(40);
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    Tooltip tooltip = new Tooltip(item.getUserName());
                    tooltip.setId("proof-of-burn-tooltip");
                    userName.setTooltip(tooltip);
                    chatUserIcon.setUserProfile(item.getUserProfile());
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getStarsCellFactory() {
        return column -> new TableCell<>() {
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    reputationScoreDisplay.applyReputationScore(item.getReputationScore());
                    setGraphic(reputationScoreDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getDetailsCellFactory() {
        return column -> new TableCell<>() {
            private final Hyperlink info = new Hyperlink(Res.get("reputation.table.columns.details.button"));

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    info.setOnAction(e -> controller.onShowDetails(item));
                    setGraphic(info);
                } else {
                    info.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode
    @Getter
    static class ListItem implements TableItem {
        private final UserProfile userProfile;
        private final ReputationScore reputationScore;
        private final String userName;
        private final String profileAgeString;
        private final long profileAge;
        private final long totalScore;
        private final String totalScoreString;

        ListItem(UserProfile userProfile, ReputationService reputationService) {
            this.userProfile = userProfile;
            userName = userProfile.getUserName();
            reputationScore = reputationService.findReputationScore(userProfile).orElse(ReputationScore.NONE);
            totalScore = reputationScore.getTotalScore();
            totalScoreString = String.valueOf(totalScore);
            profileAge = 0;
            profileAgeString = "n/a";
        }
    }
}

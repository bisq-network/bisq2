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

package bisq.desktop.main.content.user.reputation;

import bisq.common.monetary.Coin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReputationDetailsPopup extends VBox {
    private final BisqTableView<ReputationDetailsPopup.ListItem> tableView;

    public ReputationDetailsPopup(UserProfile userProfile,
                                  ReputationScore reputationScore,
                                  ReputationService reputationService) {

        ObservableList<ReputationDetailsPopup.ListItem> listItems = FXCollections.observableArrayList();

        ProofOfBurnService proofOfBurnService = reputationService.getProofOfBurnService();
        Optional.ofNullable(proofOfBurnService.getDataSetByHash().get(userProfile.getProofOfBurnKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BURNED_BSQ,
                                data.getTime(),
                                proofOfBurnService.calculateScore(data),
                                data.getAmount()))
                        .collect(Collectors.toList())));

        BondedReputationService bondedReputationService = reputationService.getBondedReputationService();
        Optional.ofNullable(bondedReputationService.getDataSetByHash().get(userProfile.getBondedReputationKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BSQ_BOND,
                                data.getTime(),
                                bondedReputationService.calculateScore(data),
                                Optional.of(data.getAmount()),
                                Optional.of(data.getLockTime())))
                        .collect(Collectors.toList())));

        AccountAgeService accountAgeService = reputationService.getAccountAgeService();
        Optional.ofNullable(accountAgeService.getDataSetByHash().get(userProfile.getAccountAgeKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BISQ1_ACCOUNT_AGE,
                                data.getDate(),
                                accountAgeService.calculateScore(data)))
                        .collect(Collectors.toList())));
        SignedWitnessService signedWitnessService = reputationService.getSignedWitnessService();
        Optional.ofNullable(signedWitnessService.getDataSetByHash().get(userProfile.getSignedWitnessKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
                                data.getWitnessSignDate(),
                                signedWitnessService.calculateScore(data)))
                        .collect(Collectors.toList())));

        ProfileAgeService profileAgeService = reputationService.getProfileAgeService();
        Optional.ofNullable(profileAgeService.getDataSetByHash().get(userProfile.getProfileAgeKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.PROFILE_AGE,
                                data.getDate(),
                                profileAgeService.calculateScore(data)))
                        .collect(Collectors.toList())));

        SortedList<ReputationDetailsPopup.ListItem> sortedList = new SortedList<>(listItems);
        tableView = new BisqTableView<>(sortedList);
        setPrefHeight(500);
        setPrefWidth(1000);
        configTableView();

        UserProfileIcon userProfileIcon = new UserProfileIcon(40);
        userProfileIcon.setUserProfile(userProfile);

        Label userName = new Label(userProfile.getNickName());
        userName.setId("chat-user-name");
        Tooltip tooltip = new BisqTooltip(userProfile.getUserName());
        tooltip.getStyleClass().add("medium-dark-tooltip");
        userName.setTooltip(tooltip);

        HBox row1 = new HBox(20, userProfileIcon, userName);
        row1.setAlignment(Pos.CENTER_LEFT);

        MaterialTextField totalScore = new MaterialTextField(Res.get("user.reputation.totalScore"));
        totalScore.setEditable(false);
        totalScore.setText(String.valueOf(reputationScore.getTotalScore()));
        totalScore.setMaxWidth(400);

        MaterialTextField ranking = new MaterialTextField(Res.get("user.reputation.ranking"));
        ranking.setEditable(false);
        ranking.setText(String.valueOf(reputationScore.getRanking()));
        ranking.setMaxWidth(400);

        HBox row2 = new HBox(20, totalScore, ranking);

        setSpacing(15);
        setPadding(new Insets(0, 0, 0, 20));
        VBox.setMargin(tableView, new Insets(-10, 0, 0, 0));
        getChildren().addAll(row1, row2, tableView);
        UIThread.runOnNextRenderFrame(this::requestFocus);
    }

    private void configTableView() {
        BisqTableColumn<ListItem> dateColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.date"))
                .isFirst()
                .minWidth(110)
                .comparator(Comparator.comparing(ListItem::getDate))
                .valueSupplier(ListItem::getDateString)
                .build();
        tableView.getColumns().add(dateColumn);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ReputationDetailsPopup.ListItem>()
                .title(Res.get("user.reputation.details.table.columns.source"))
                .isFirst()
                .comparator(Comparator.comparing(ReputationDetailsPopup.ListItem::getReputationSource))
                .valueSupplier(ReputationDetailsPopup.ListItem::getSourceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReputationDetailsPopup.ListItem>()
                .title(Res.get("user.reputation.details.table.columns.score"))
                .comparator(Comparator.comparing(ReputationDetailsPopup.ListItem::getScore))
                .valueSupplier(ReputationDetailsPopup.ListItem::getScoreString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReputationDetailsPopup.ListItem>()
                .title(Res.get("temporal.age"))
                .comparator(Comparator.comparing(ReputationDetailsPopup.ListItem::getAge))
                .valueSupplier(ReputationDetailsPopup.ListItem::getAgeString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReputationDetailsPopup.ListItem>()
                .title(Res.get("offer.amount"))
                .comparator(Comparator.comparing(ReputationDetailsPopup.ListItem::getAmount))
                .valueSupplier(ReputationDetailsPopup.ListItem::getAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReputationDetailsPopup.ListItem>()
                .title(Res.get("user.reputation.details.table.columns.lockTime"))
                .comparator(Comparator.comparing(ReputationDetailsPopup.ListItem::getLockTime))
                .valueSupplier(ReputationDetailsPopup.ListItem::getLockTimeString)
                .build());
    }

    @EqualsAndHashCode
    @Getter
    static class ListItem implements TableItem {
        private final ReputationSource reputationSource;
        private final long date;
        private final long age;
        private final long amount;
        private final long score;
        private final long lockTime;

        private final String dateString;
        private final String sourceString;
        private final String ageString;
        private final String amountString;
        private final String scoreString;
        private final String lockTimeString;

        public ListItem(ReputationSource reputationSource, long date, long score) {
            this(reputationSource, date, score, Optional.empty(), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource, long date, long score, long amount) {
            this(reputationSource, date, score, Optional.of(amount), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource, long date, long score, Optional<Long> optionalAmount, Optional<Long> optionalLockTime) {
            this.reputationSource = reputationSource;
            this.date = date;
            this.amount = optionalAmount.orElse(0L);
            this.score = score;
            this.lockTime = optionalLockTime.orElse(0L);
            age = TimeFormatter.getAgeInDays(date);

            dateString = DateFormatter.formatDateTime(date);
            sourceString = Res.get("user.reputation.source." + reputationSource.name());
            ageString = TimeFormatter.formatAgeInDays(date);
            amountString = optionalAmount.map(amount -> AmountFormatter.formatAmountWithCode(Coin.fromValue(amount, "BSQ"))).orElse("-");
            scoreString = String.valueOf(score);
            lockTimeString = optionalLockTime.map(String::valueOf).orElse("-");
        }
    }
}
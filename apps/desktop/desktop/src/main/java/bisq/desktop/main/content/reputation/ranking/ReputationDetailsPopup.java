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

package bisq.desktop.main.content.reputation.ranking;

import bisq.common.monetary.Coin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.DateTableItem;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.AccountAgeService;
import bisq.user.reputation.BondedReputationService;
import bisq.user.reputation.ProfileAgeService;
import bisq.user.reputation.ProofOfBurnService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import bisq.user.reputation.SignedWitnessService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Comparator;
import java.util.Optional;

public class ReputationDetailsPopup extends VBox {
    private final BisqTableView<ListItem> tableView;
    private final UserProfileDisplay userProfileDisplay;

    public ReputationDetailsPopup(UserProfile userProfile,
                                  ReputationScore reputationScore,
                                  ReputationService reputationService) {

        ObservableList<ListItem> listItems = FXCollections.observableArrayList();

        ProofOfBurnService proofOfBurnService = reputationService.getProofOfBurnService();
        Optional.ofNullable(proofOfBurnService.getDataSetByHash().get(userProfile.getProofOfBurnKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BURNED_BSQ,
                                data.getBlockTime(),
                                proofOfBurnService.calculateScore(data),
                                data.getAmount()))
                        .toList()));

        BondedReputationService bondedReputationService = reputationService.getBondedReputationService();
        Optional.ofNullable(bondedReputationService.getDataSetByHash().get(userProfile.getBondedReputationKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BSQ_BOND,
                                data.getBlockTime(),
                                bondedReputationService.calculateScore(data),
                                Optional.of(data.getAmount()),
                                Optional.of(data.getLockTime())))
                        .toList()));

        AccountAgeService accountAgeService = reputationService.getAccountAgeService();
        Optional.ofNullable(accountAgeService.getDataSetByHash().get(userProfile.getAccountAgeKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BISQ1_ACCOUNT_AGE,
                                data.getDate(),
                                accountAgeService.calculateScore(data)))
                        .toList()));
        SignedWitnessService signedWitnessService = reputationService.getSignedWitnessService();
        Optional.ofNullable(signedWitnessService.getDataSetByHash().get(userProfile.getSignedWitnessKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
                                data.getWitnessSignDate(),
                                signedWitnessService.calculateScore(data)))
                        .toList()));

        ProfileAgeService profileAgeService = reputationService.getProfileAgeService();
        Optional.ofNullable(profileAgeService.getDataSetByHash().get(userProfile.getProfileAgeKey()))
                .ifPresent(dataSet -> listItems.addAll(dataSet.stream()
                        .map(data -> new ListItem(ReputationSource.PROFILE_AGE,
                                data.getDate(),
                                profileAgeService.calculateScore(data)))
                        .toList()));

        tableView = new BisqTableView<>(listItems);
        setPrefHeight(500);
        setPrefWidth(1000);
        configTableView();

        userProfileDisplay = new UserProfileDisplay(userProfile, reputationScore);

        MaterialTextField totalScore = new MaterialTextField(Res.get("reputation.totalScore"));
        totalScore.setEditable(false);
        totalScore.setText(String.valueOf(reputationScore.getTotalScore()));
        totalScore.setMaxWidth(400);

        MaterialTextField ranking = new MaterialTextField(Res.get("reputation.ranking"));
        ranking.setEditable(false);
        ranking.setText(reputationScore.getRankingAsString());
        ranking.setMaxWidth(400);

        HBox row2 = new HBox(20, totalScore, ranking);

        setSpacing(15);
        setPadding(new Insets(0, 0, 0, 20));
        VBox.setMargin(tableView, new Insets(-10, 0, 0, 0));
        getChildren().addAll(userProfileDisplay, row2, tableView);
        UIThread.runOnNextRenderFrame(this::requestFocus);
    }

    public void initialize() {
        tableView.initialize();
    }

    public void dispose() {
        tableView.dispose();
        userProfileDisplay.dispose();
    }

    private void configTableView() {
        tableView.getColumns().add(DateColumnUtil.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.source"))
                .left()
                .comparator(Comparator.comparing(ListItem::getReputationSource))
                .valueSupplier(ListItem::getSourceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.score"))
                .comparator(Comparator.comparing(ListItem::getScore))
                .valueSupplier(ListItem::getScoreString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.age"))
                .comparator(Comparator.comparing(ListItem::getAge))
                .valueSupplier(ListItem::getAgeString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("offer.amount"))
                .comparator(Comparator.comparing(ListItem::getAmount))
                .valueSupplier(ListItem::getAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.lockTime"))
                .comparator(Comparator.comparing(ListItem::getLockTime))
                .valueSupplier(ListItem::getLockTimeString)
                .build());
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class ListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final ReputationSource reputationSource;
        @EqualsAndHashCode.Include
        private final long date, score, amount, lockTime;

        private final long age;
        private final String dateString, timeString, sourceString, ageString, amountString, scoreString, lockTimeString;

        public ListItem(ReputationSource reputationSource, long blockTime, long score) {
            this(reputationSource, blockTime, score, Optional.empty(), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource, long blockTime, long score, long amount) {
            this(reputationSource, blockTime, score, Optional.of(amount), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource,
                        long blockTime,
                        long score,
                        Optional<Long> optionalAmount,
                        Optional<Long> optionalLockTime) {
            this.reputationSource = reputationSource;
            this.date = blockTime;
            this.score = score;
            this.amount = optionalAmount.orElse(0L);
            this.lockTime = optionalLockTime.orElse(0L);

            dateString = DateFormatter.formatDate(blockTime);
            timeString = DateFormatter.formatTime(blockTime);
            age = TimeFormatter.getAgeInDays(blockTime);
            ageString = TimeFormatter.formatAgeInDays(blockTime);
            sourceString = reputationSource.getDisplayString();
            amountString = optionalAmount.map(amount -> AmountFormatter.formatAmountWithCode(Coin.fromValue(amount, "BSQ"))).orElse("-");
            scoreString = String.valueOf(score);
            lockTimeString = optionalLockTime.map(String::valueOf).orElse("-");
        }
    }
}
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

import bisq.bisq_easy.NavigationTarget;
import bisq.common.data.Pair;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import javafx.scene.control.Toggle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ReputationRankingController implements Controller {
    @Getter
    private final ReputationRankingView view;
    private final ReputationService reputationService;
    private final UserProfileService userProfileService;
    private final ReputationRankingModel model;
    private Pin userProfileByIdPin, proofOfBurnScoreChangedFlagPin,
            bondedReputationScoreChangedFlagPin, signedWitnessScoreChangedFlagPin,
            accountAgeScoreChangedFlagPin;
    private Subscription filterMenuItemTogglePin;

    public ReputationRankingController(ServiceProvider serviceProvider) {
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        reputationService = serviceProvider.getUserService().getReputationService();

        model = new ReputationRankingModel();

        model.getFilterItems().add(getShowAllFilterMenuItem());
        addFilterMenuItem(ReputationSource.BURNED_BSQ);
        addFilterMenuItem(ReputationSource.BSQ_BOND);
        addFilterMenuItem(ReputationSource.BISQ1_ACCOUNT_AGE);
        addFilterMenuItem(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS);
        addFilterMenuItem(ReputationSource.PROFILE_AGE);

        view = new ReputationRankingView(model, this);
    }

    @Override
    public void onActivate() {
        model.getFilterMenuItemToggleGroup().selectToggle(getShowAllFilterMenuItem());

        filterMenuItemTogglePin = EasyBind.subscribe(model.getFilterMenuItemToggleGroup().selectedToggleProperty(), this::updateFilter);

        userProfileByIdPin = userProfileService.getUserProfileById().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, UserProfile userProfile) {
                UIThread.run(() -> {
                    ReputationRankingView.ListItem listItem = new ReputationRankingView.ListItem(userProfile,
                            reputationService,
                            ReputationRankingController.this,
                            model.getFilterMenuItemToggleGroup(),
                            userProfileService);
                    model.getListItems().add(listItem);
                });
            }

            @Override
            public void putAll(Map<? extends String, ? extends UserProfile> map) {
                UIThread.run(() -> {
                    HashSet<UserProfile> userProfilesClone = new HashSet<>(map.values());
                    CatHash.pruneOutdatedProfileIcons(userProfilesClone);

                    HashSet<? extends UserProfile> clone = new HashSet<>(map.values());
                    List<ReputationRankingView.ListItem> listItems = clone.stream()
                            .map(userProfile -> new ReputationRankingView.ListItem(userProfile,
                                    reputationService,
                                    ReputationRankingController.this,
                                    model.getFilterMenuItemToggleGroup(),
                                    userProfileService))
                            .collect(Collectors.toList());
                    model.getListItems().setAll(listItems);
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String) {
                    UIThread.run(() -> {
                        Optional<ReputationRankingView.ListItem> toRemove = model.getListItems().stream()
                                .filter(e -> e.getUserProfile().getId().equals(key))
                                .findAny();
                        toRemove.ifPresent(listItem -> model.getListItems().remove(listItem));
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> model.getListItems().clear());
            }
        });

        proofOfBurnScoreChangedFlagPin = reputationService.getProofOfBurnService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        bondedReputationScoreChangedFlagPin = reputationService.getBondedReputationService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        accountAgeScoreChangedFlagPin = reputationService.getAccountAgeService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        signedWitnessScoreChangedFlagPin = reputationService.getSignedWitnessService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
    }

    @Override
    public void onDeactivate() {
        userProfileByIdPin.unbind();
        proofOfBurnScoreChangedFlagPin.unbind();
        bondedReputationScoreChangedFlagPin.unbind();
        accountAgeScoreChangedFlagPin.unbind();
        signedWitnessScoreChangedFlagPin.unbind();

        filterMenuItemTogglePin.unsubscribe();
        model.getListItems().forEach(ReputationRankingView.ListItem::dispose);
        model.getListItems().clear();
    }

    void onOpenProfileCard(UserProfile userProfile) {
        Navigation.navigateTo(NavigationTarget.PROFILE_CARD_REPUTATION,
                new ProfileCardController.InitData(userProfile));
    }

    void applySearchPredicate(String searchText) {
        String string = searchText == null ? "" : searchText.toLowerCase();
        model.setSearchStringPredicate(item ->
                StringUtils.isEmpty(string) ||
                        item.getUserName().toLowerCase().contains(string) ||
                        item.getUserProfile().getNym().toLowerCase().contains(string) ||
                        item.getTotalScoreString().contains(string) ||
                        item.getProfileAgeString().contains(string) ||
                        item.getValueAsStringProperty().get().toLowerCase().contains(string));
        applyPredicates();
    }

    Optional<ReputationSource> resolveReputationSource(Toggle toggle) {
        return RichTableView.FilterMenuItem.fromToggle(toggle)
                .flatMap(selectedFilterMenuItem -> selectedFilterMenuItem.getData()
                        .filter(data -> data instanceof ReputationSource)
                        .map(data -> (ReputationSource) data));
    }

    void openProfileCard(UserProfile userProfile) {
        Navigation.navigateTo(NavigationTarget.PROFILE_CARD,
                new ProfileCardController.InitData(userProfile));
    }

    private void updateScore(Pair<String, Long> userProfileIdScorePair) {
        if (userProfileIdScorePair == null) {
            return;
        }
        String userProfileId = userProfileIdScorePair.getFirst();
        UIThread.run(() -> {
            model.getListItems().stream().filter(e -> e.getUserProfile().getId().equals(userProfileId))
                    .forEach(item -> item.applyReputationScore(userProfileId));
            model.getScoreChangeTrigger().set(!model.getScoreChangeTrigger().get());
        });
    }

    private void addFilterMenuItem(ReputationSource reputationSource) {
        RichTableView.FilterMenuItem<ReputationRankingView.ListItem> filterMenuItem = new RichTableView.FilterMenuItem<>(
                model.getFilterMenuItemToggleGroup(),
                reputationSource.getDisplayString(),
                Optional.of(reputationSource),
                item -> item.getReputationSources().contains(reputationSource));
        model.getFilterItems().add(filterMenuItem);
    }

    private void updateFilter(Toggle selectedToggle) {
        model.getSelectedReputationSource().set(resolveReputationSource(selectedToggle).orElse(null));

        RichTableView.FilterMenuItem.fromToggle(selectedToggle)
                .ifPresentOrElse(selectedFilterMenuItem -> {
                    model.getValueColumnVisible().set(selectedFilterMenuItem.getData().isPresent());
                    model.getFilteredValueTitle().set(selectedFilterMenuItem.getTitle().toUpperCase());
                    UIThread.runOnNextRenderFrame(() -> {
                        model.setFilterItemPredicate(item -> selectedFilterMenuItem.getFilter().test(item));
                        applyPredicates();
                    });

                }, () -> {
                    model.setFilterItemPredicate(item -> true);
                    applyPredicates();
                });
    }

    private void applyPredicates() {
        model.getFilteredList().setPredicate(item ->
                model.getFilterItemPredicate().test(item) &&
                        model.getSearchStringPredicate().test(item)
        );
    }

    private RichTableView.FilterMenuItem<ReputationRankingView.ListItem> getShowAllFilterMenuItem() {
        return RichTableView.FilterMenuItem.getShowAllFilterMenuItem(model.getFilterMenuItemToggleGroup());
    }
}

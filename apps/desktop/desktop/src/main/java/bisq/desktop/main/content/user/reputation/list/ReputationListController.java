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

package bisq.desktop.main.content.user.reputation.list;

import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.StandardTable;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import javafx.scene.control.Toggle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ReputationListController implements Controller {
    @Getter
    private final ReputationListView view;
    private final ReputationService reputationService;
    private final UserProfileService userProfileService;
    private final ReputationListModel model;
    private Pin userProfileByIdPin, proofOfBurnScoreChangedFlagPin,
            bondedReputationScoreChangedFlagPin, signedWitnessScoreChangedFlagPin,
            accountAgeScoreChangedFlagPin;
    private Subscription filterMenuItemTogglePin;
    @Nullable
    private ReputationDetailsPopup reputationDetailsPopup;

    public ReputationListController(ServiceProvider serviceProvider) {
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        reputationService = serviceProvider.getUserService().getReputationService();

        model = new ReputationListModel();

        model.getFilterItems().add(getShowAllFilterMenuItem());
        addFilterMenuItem(ReputationSource.BURNED_BSQ);
        addFilterMenuItem(ReputationSource.BSQ_BOND);
        addFilterMenuItem(ReputationSource.BISQ1_ACCOUNT_AGE);
        addFilterMenuItem(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS);
        addFilterMenuItem(ReputationSource.PROFILE_AGE);

        view = new ReputationListView(model, this);
    }

    @Override
    public void onActivate() {
        model.getFilterMenuItemToggleGroup().selectToggle(getShowAllFilterMenuItem());

        filterMenuItemTogglePin = EasyBind.subscribe(model.getFilterMenuItemToggleGroup().selectedToggleProperty(), this::updateFilter);

        userProfileByIdPin = userProfileService.getUserProfileById().addObserver(new HashMapObserver<>() {
            @Override
            public void put(String key, UserProfile userProfile) {
                UIThread.run(() -> {
                    ReputationListView.ListItem listItem = new ReputationListView.ListItem(userProfile,
                            reputationService,
                            ReputationListController.this,
                            model.getFilterMenuItemToggleGroup());
                    model.getListItems().add(listItem);
                });
            }

            @Override
            public void putAll(Map<? extends String, ? extends UserProfile> map) {
                UIThread.run(() -> {
                    List<ReputationListView.ListItem> listItems = map.values().stream()
                            .map(userProfile -> new ReputationListView.ListItem(userProfile,
                                    reputationService,
                                    ReputationListController.this,
                                    model.getFilterMenuItemToggleGroup()))
                            .collect(Collectors.toList());
                    model.getListItems().setAll(listItems);
                });
            }

            @Override
            public void remove(Object key) {
                if (key instanceof String) {
                    UIThread.run(() -> {
                        Optional<ReputationListView.ListItem> toRemove = model.getListItems().stream()
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

        proofOfBurnScoreChangedFlagPin = reputationService.getProofOfBurnService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        bondedReputationScoreChangedFlagPin = reputationService.getBondedReputationService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        accountAgeScoreChangedFlagPin = reputationService.getAccountAgeService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        signedWitnessScoreChangedFlagPin = reputationService.getSignedWitnessService().getUserProfileIdOfUpdatedScore()
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
        if (reputationDetailsPopup != null) {
            reputationDetailsPopup.dispose();
            reputationDetailsPopup = null;
        }
        model.getListItems().forEach(ReputationListView.ListItem::dispose);
    }

    void onShowDetails(ReputationListView.ListItem item) {
        if (reputationDetailsPopup != null) {
            reputationDetailsPopup.dispose();
        }
        reputationDetailsPopup = new ReputationDetailsPopup(item.getUserProfile(), item.getReputationScore(), reputationService);
        reputationDetailsPopup.initialize();
        new Popup().headline(Res.get("user.reputation.table.columns.details.popup.headline"))
                .content(reputationDetailsPopup)
                .width(1000)
                .onClose(() -> {
                    reputationDetailsPopup.dispose();
                    reputationDetailsPopup = null;
                })
                .show();
    }

    Optional<ReputationSource> resolveReputationSource(Toggle toggle) {
        return StandardTable.FilterMenuItem.fromToggle(toggle)
                .flatMap(selectedFilterMenuItem -> selectedFilterMenuItem.getData()
                        .filter(data -> data instanceof ReputationSource)
                        .map(data -> (ReputationSource) data));
    }

    private void updateScore(String userProfileId) {
        UIThread.run(() -> {
            model.getListItems().stream().filter(e -> e.getUserProfile().getId().equals(userProfileId))
                    .forEach(item -> item.applyReputationScore(userProfileId));
            // Enforce update in view by setting to null first
            model.getUserProfileIdOfScoreUpdate().set(null);
            model.getUserProfileIdOfScoreUpdate().set(userProfileId);
        });
    }

    private void addFilterMenuItem(ReputationSource reputationSource) {
        StandardTable.FilterMenuItem<ReputationListView.ListItem> filterMenuItem = new StandardTable.FilterMenuItem<>(
                model.getFilterMenuItemToggleGroup(),
                reputationSource.getDisplayString(),
                Optional.of(reputationSource),
                item -> item.getReputationSources().contains(reputationSource));
        model.getFilterItems().add(filterMenuItem);
    }

    private void updateFilter(Toggle selectedToggle) {
        StandardTable.FilterMenuItem.fromToggle(selectedToggle).ifPresent(selectedFilterMenuItem -> {
            model.getValueColumnVisible().set(selectedFilterMenuItem.getData().isPresent());
            model.getFilteredValueTitle().set(selectedFilterMenuItem.getTitle().toUpperCase());
            model.getFilteredList().setPredicate(item ->
                    selectedFilterMenuItem.getFilter().test(item));
        });
        model.getSelectedReputationSource().set(resolveReputationSource(selectedToggle).orElse(null));
    }

    private StandardTable.FilterMenuItem<ReputationListView.ListItem> getShowAllFilterMenuItem() {
        return StandardTable.FilterMenuItem.getShowAllFilterMenuItem(model.getFilterMenuItemToggleGroup());
    }
}

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

import bisq.bisq_easy.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class ReputationController implements Controller {
    @Getter
    private final ReputationView view;
    private final ReputationService reputationService;
    private final UserProfileService userProfileService;
    private final ReputationModel model;
    private Pin getNumUserProfilesPin, proofOfBurnScoreChangedFlagPin,
            bondedReputationScoreChangedFlagPin, signedWitnessScoreChangedFlagPin,
            accountAgeScoreChangedFlagPin;

    public ReputationController(ServiceProvider serviceProvider) {
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        reputationService = serviceProvider.getUserService().getReputationService();

        model = new ReputationModel();
        view = new ReputationView(model, this);
    }

    @Override
    public void onActivate() {
        getNumUserProfilesPin = userProfileService.getNumUserProfiles()
                .addObserver(numUserProfiles -> UIThread.run(() -> model.getListItems().setAll(userProfileService.getUserProfiles().stream()
                        .map(userProfile -> new ReputationView.ListItem(userProfile, reputationService))
                        .collect(Collectors.toList()))));
        proofOfBurnScoreChangedFlagPin = reputationService.getProofOfBurnService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        bondedReputationScoreChangedFlagPin = reputationService.getBondedReputationService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        accountAgeScoreChangedFlagPin = reputationService.getAccountAgeService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);
        signedWitnessScoreChangedFlagPin = reputationService.getSignedWitnessService().getUserProfileIdOfUpdatedScore()
                .addObserver(this::updateScore);

        model.getFilteredList().setPredicate(item -> item.getTotalScore() > 0);
    }

    @Override
    public void onDeactivate() {
        getNumUserProfilesPin.unbind();
        proofOfBurnScoreChangedFlagPin.unbind();
        bondedReputationScoreChangedFlagPin.unbind();
        accountAgeScoreChangedFlagPin.unbind();
        signedWitnessScoreChangedFlagPin.unbind();
    }

    public void onBurnBsq() {
        Navigation.navigateTo(NavigationTarget.BURN_BSQ);
    }

    public void onBsqBond() {
        Navigation.navigateTo(NavigationTarget.BSQ_BOND);
    }

    public void onAccountAge() {
        Navigation.navigateTo(NavigationTarget.ACCOUNT_AGE);
    }

    public void onSignedAccount() {
        Navigation.navigateTo(NavigationTarget.SIGNED_WITNESS);
    }

    public void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }

    public void onShowDetails(ReputationView.ListItem item) {
        new Popup().headline(Res.get("user.reputation.table.columns.details.popup.headline"))
                .content(new ReputationDetailsPopup(item.getUserProfile(), item.getReputationScore(), reputationService))
                .width(1000)
                .show();
    }

    private void updateScore(String userProfileId) {
        UIThread.run(() -> {
            model.getListItems().stream().filter(e -> e.getUserProfile().getId().equals(userProfileId))
                    .forEach(item -> item.requestReputationScore(userProfileId));
            // Enforce update in view by setting to null first
            model.getUserProfileIdOfScoreUpdate().set(null);
            model.getUserProfileIdOfScoreUpdate().set(userProfileId);
        });
    }
}

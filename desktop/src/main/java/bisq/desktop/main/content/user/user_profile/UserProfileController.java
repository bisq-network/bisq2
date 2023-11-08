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

package bisq.desktop.main.content.user.user_profile;

import bisq.bisq_easy.BisqEasyService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.DataService;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ProfileAgeService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static bisq.desktop.common.view.NavigationTarget.CREATE_PROFILE_STEP1;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class UserProfileController implements Controller {
    private final UserProfileModel model;
    @Getter
    private final UserProfileView view;
    private final UserIdentityService userIdentityService;
    private final ReputationService reputationService;
    private final ProfileAgeService profileAgeService;
    private final BisqEasyService bisqEasyService;
    private Pin userProfilesPin, selectedUserProfilePin, reputationChangedPin;

    public UserProfileController(ServiceProvider serviceProvider) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        profileAgeService = reputationService.getProfileAgeService();
        bisqEasyService = serviceProvider.getBisqEasyService();

        model = new UserProfileModel();
        view = new UserProfileView(model, this);
    }

    @Override
    public void onActivate() {
        userProfilesPin = FxBindings.<UserIdentity, UserIdentity>bind(model.getUserIdentities())
                .to(userIdentityService.getUserIdentities());

        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                userIdentity -> {
                    if (userIdentity == null) {
                        return;
                    }
                    UIThread.run(() -> {
                        model.getSelectedUserIdentity().set(userIdentity);

                        UserProfile userProfile = userIdentity.getUserProfile();
                        model.getNickName().set(userProfile.getNickName());
                        model.getNymId().set(userProfile.getNym());
                        model.getProfileId().set(userProfile.getId());
                        model.getRoboHash().set(RoboHash.getImage(userProfile.getPubKeyHash()));
                        model.getStatement().set(userProfile.getStatement());
                        model.getTerms().set(userProfile.getTerms());

                        model.getProfileAge().set(profileAgeService.getProfileAge(userIdentity.getUserProfile())
                                .map(TimeFormatter::formatAgeInDays)
                                .orElse(Res.get("data.na")));
                    });
                }
        );
        reputationChangedPin = reputationService.getChangedUserProfileScore().addObserver(userProfileId -> UIThread.run(this::applyReputationScore));
    }

    @Override
    public void onDeactivate() {
        userProfilesPin.unbind();
        selectedUserProfilePin.unbind();
        reputationChangedPin.unbind();
        model.getSelectedUserIdentity().set(null);
    }

    public void onSelected(UserIdentity userIdentity) {
        if (userIdentity != null) {
            userIdentityService.selectChatUserIdentity(userIdentity);
            applyReputationScore();
        }
    }

    public void resetSelection() {
        model.getSelectedUserIdentity().set(null);
        model.getNickName().set("");
        model.getNymId().set("");
        model.getProfileId().set("");
        model.getRoboHash().set(null);
        model.getStatement().set("");
        model.getTerms().set("");
        model.getProfileAge().set("");
        model.getReputationScore().set(null);
        model.getReputationScoreValue().set(null);
    }

    public void onAddNewChatUser() {
        Navigation.navigateTo(CREATE_PROFILE_STEP1);
    }

    public void onSave() {
        var userIdentity = userIdentityService.getSelectedUserIdentity();
        if (userIdentity == null) {
            // This should never happen as the combobox selection is validated before getting here
            new Popup().invalid(Res.get("user.userProfile.save.popup.noSelectedProfile")).show();
            return;
        }
        if (noNewChangesToBeSaved(userIdentity)) {
            new Popup().invalid(Res.get("user.userProfile.save.popup.noChangesToBeSaved")).show();
            return;
        }
        userIdentityService.editUserProfile(model.getSelectedUserIdentity().get(), model.getTerms().get(), model.getStatement().get())
                .thenAccept(result -> {
                    UIThread.runOnNextRenderFrame(() -> {
                        UserIdentity value = userIdentityService.getSelectedUserIdentity();
                        model.getSelectedUserIdentity().set(value);
                    });
                });
    }

    private boolean noNewChangesToBeSaved(UserIdentity userIdentity) {
        var userProfile = userIdentity.getUserProfile();
        var statement = model.getStatement().get();
        var terms = model.getTerms().get();
        return statement.equals(userProfile.getStatement()) && terms.equals(userProfile.getTerms());
    }

    public void onDeleteProfile() {
        String profileName = checkNotNull(userIdentityService.getSelectedUserIdentity()).getUserName();
        new Popup().warning(Res.get("user.userProfile.deleteProfile.popup.warning", profileName))
                .onAction(this::doDeleteProfile)
                .actionButtonText(Res.get("user.userProfile.deleteProfile.popup.warning.yes"))
                .closeButtonText(Res.get("action.cancel"))
                .show();
    }

    private CompletableFuture<DataService.BroadCastDataResult> doDeleteProfile() {
        if (bisqEasyService.isDeleteUserIdentityProhibited(userIdentityService.getSelectedUserIdentity())) {
            new Popup().warning(Res.get("user.userProfile.deleteProfile.cannotDelete"))
                    .closeButtonText(Res.get("confirmation.ok"))
                    .show();
            return CompletableFuture.completedFuture(null);
        }

        return bisqEasyService.deleteUserIdentity(userIdentityService.getSelectedUserIdentity())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        UIThread.run(() -> new Popup().error(throwable).show());
                    } else {
                        if (!model.getUserIdentities().isEmpty()) {
                            UIThread.runOnNextRenderFrame(() -> {
                                UserIdentity value = model.getUserIdentities().get(0);
                                model.getSelectedUserIdentity().set(value);
                            });
                        }
                    }
                });
    }

    private void applyReputationScore() {
        if (model.getSelectedUserIdentity().get() == null) {
            return;
        }
        ReputationScore reputationScore = reputationService.getReputationScore(model.getSelectedUserIdentity().get().getUserProfile());
        model.getReputationScoreValue().set(String.valueOf(reputationScore.getTotalScore()));
        model.getReputationScore().set(reputationScore);
    }
}

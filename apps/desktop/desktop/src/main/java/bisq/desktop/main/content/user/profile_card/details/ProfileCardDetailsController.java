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

package bisq.desktop.main.content.user.profile_card.details;

import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class ProfileCardDetailsController implements Controller {
    @Getter
    private final ProfileCardDetailsView view;
    private final ProfileCardDetailsModel model;
    private final ReputationService reputationService;
    private Pin reputationChangedPin;
    private UIScheduler livenessUpdateScheduler;

    public ProfileCardDetailsController(ServiceProvider serviceProvider) {
        model = new ProfileCardDetailsModel();

        view = new ProfileCardDetailsView(model, this);
        reputationService = serviceProvider.getUserService().getReputationService();
    }

    public void setUserProfile(UserProfile userProfile) {
        model.setUserProfile(userProfile);
        model.setNickName(userProfile.getNickName());
        model.setBotId(userProfile.getNym());
        model.setUserId(userProfile.getId());
        model.setTransportAddress(userProfile.getAddressByTransportDisplayString());
        model.setProfileAge(reputationService.getProfileAgeService().getProfileAge(userProfile)
                .map(TimeFormatter::formatAgeInDaysAndYears)
                .orElse(Res.get("data.na")));
        model.setStatement(StringUtils.toOptional(userProfile.getStatement()));
    }

    @Override
    public void onActivate() {
        UserProfile userProfile = model.getUserProfile();
        reputationChangedPin = reputationService.getChangedUserProfileScore().addObserver(userProfileId -> UIThread.run(() -> {
            ReputationScore reputationScore = reputationService.getReputationScore(userProfile);
            model.getTotalReputationScore().set(String.valueOf(reputationScore.getTotalScore()));
        }));

        if (livenessUpdateScheduler != null) {
            livenessUpdateScheduler.stop();
        }
        livenessUpdateScheduler = UIScheduler.run(() -> {
                    long publishDate = userProfile.getPublishDate();
                    if (publishDate == 0) {
                        model.getLastUserActivity().set(Res.get("data.na"));
                    } else {
                        long age = Math.max(0, System.currentTimeMillis() - publishDate);
                        String formattedAge = TimeFormatter.formatAge(age);
                        model.getLastUserActivity().set(Res.get("user.userProfile.livenessState.ageDisplay", formattedAge));
                    }
                })
                .periodically(0, 1, TimeUnit.SECONDS);

    }

    @Override
    public void onDeactivate() {
        reputationChangedPin.unbind();
        if (livenessUpdateScheduler != null) {
            livenessUpdateScheduler.stop();
            livenessUpdateScheduler = null;
        }
    }
}

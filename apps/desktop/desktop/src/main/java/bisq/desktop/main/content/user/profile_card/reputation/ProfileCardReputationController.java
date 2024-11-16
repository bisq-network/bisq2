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

package bisq.desktop.main.content.user.profile_card.reputation;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.AccountAgeService;
import bisq.user.reputation.BondedReputationService;
import bisq.user.reputation.ProfileAgeService;
import bisq.user.reputation.ProofOfBurnService;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import bisq.user.reputation.SignedWitnessService;
import lombok.Getter;

import java.util.Optional;

public class ProfileCardReputationController implements Controller {
    @Getter
    private final ProfileCardReputationView view;
    private final ProfileCardReputationModel model;
    private final ReputationService reputationService;

    public ProfileCardReputationController(ServiceProvider serviceProvider) {
        model = new ProfileCardReputationModel();
        view = new ProfileCardReputationView(model, this);
        reputationService = serviceProvider.getUserService().getReputationService();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void updateUserProfileData(UserProfile userProfile) {
        model.getListItems().clear();

        ProofOfBurnService proofOfBurnService = reputationService.getProofOfBurnService();
        Optional.ofNullable(proofOfBurnService.getDataSetByHash().get(userProfile.getProofOfBurnKey()))
                .ifPresent(dataSet -> model.getListItems().addAll(dataSet.stream()
                        .map(data -> new ProfileCardReputationView.ListItem(ReputationSource.BURNED_BSQ,
                                data.getBlockTime(),
                                proofOfBurnService.calculateScore(data),
                                data.getAmount()))
                        .toList()));

        BondedReputationService bondedReputationService = reputationService.getBondedReputationService();
        Optional.ofNullable(bondedReputationService.getDataSetByHash().get(userProfile.getBondedReputationKey()))
                .ifPresent(dataSet -> model.getListItems().addAll(dataSet.stream()
                        .map(data -> new ProfileCardReputationView.ListItem(ReputationSource.BSQ_BOND,
                                data.getBlockTime(),
                                bondedReputationService.calculateScore(data),
                                Optional.of(data.getAmount()),
                                Optional.of(data.getLockTime())))
                        .toList()));

        AccountAgeService accountAgeService = reputationService.getAccountAgeService();
        Optional.ofNullable(accountAgeService.getDataSetByHash().get(userProfile.getAccountAgeKey()))
                .ifPresent(dataSet -> model.getListItems().addAll(dataSet.stream()
                        .map(data -> new ProfileCardReputationView.ListItem(ReputationSource.BISQ1_ACCOUNT_AGE,
                                data.getDate(),
                                accountAgeService.calculateScore(data)))
                        .toList()));

        SignedWitnessService signedWitnessService = reputationService.getSignedWitnessService();
        Optional.ofNullable(signedWitnessService.getDataSetByHash().get(userProfile.getSignedWitnessKey()))
                .ifPresent(dataSet -> model.getListItems().addAll(dataSet.stream()
                        .map(data -> new ProfileCardReputationView.ListItem(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
                                data.getWitnessSignDate(),
                                signedWitnessService.calculateScore(data)))
                        .toList()));

        ProfileAgeService profileAgeService = reputationService.getProfileAgeService();
        Optional.ofNullable(profileAgeService.getDataSetByHash().get(userProfile.getProfileAgeKey()))
                .ifPresent(dataSet -> model.getListItems().addAll(dataSet.stream()
                        .map(data -> new ProfileCardReputationView.ListItem(ReputationSource.PROFILE_AGE,
                                data.getDate(),
                                profileAgeService.calculateScore(data)))
                        .toList()));
    }
}

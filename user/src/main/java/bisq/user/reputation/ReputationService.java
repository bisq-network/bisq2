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

package bisq.user.reputation;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Slf4j
public class ReputationService implements Service {
    private final ProofOfBurnService proofOfBurnService;
    private final BondedReputationService bondedReputationService;
    private final AccountAgeService accountAgeService;
    private final SignedWitnessService signedWitnessService;
    private final Observable<String> changedUserProfileScore = new Observable<>();
    private final Map<String, Long> scoreByUserProfileId = new ConcurrentHashMap<>();
    private final ProfileAgeService profileAgeService;

    public ReputationService(PersistenceService persistenceService,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService,
                             BannedUserService bannedUserService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        proofOfBurnService = new ProofOfBurnService(networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                authorizedBondedRolesService);
        bondedReputationService = new BondedReputationService(networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                authorizedBondedRolesService);
        accountAgeService = new AccountAgeService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                authorizedBondedRolesService);
        signedWitnessService = new SignedWitnessService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                authorizedBondedRolesService);
        profileAgeService = new ProfileAgeService(persistenceService,
                networkService,
                userIdentityService,
                userProfileService,
                bannedUserService,
                authorizedBondedRolesService);

        proofOfBurnService.getUserProfileIdOfUpdatedScore().addObserver(this::onUserProfileScoreChanged);
        bondedReputationService.getUserProfileIdOfUpdatedScore().addObserver(this::onUserProfileScoreChanged);
        accountAgeService.getUserProfileIdOfUpdatedScore().addObserver(this::onUserProfileScoreChanged);
        signedWitnessService.getUserProfileIdOfUpdatedScore().addObserver(this::onUserProfileScoreChanged);
        profileAgeService.getUserProfileIdOfUpdatedScore().addObserver(this::onUserProfileScoreChanged);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return proofOfBurnService.initialize()
                .thenCompose(r -> bondedReputationService.initialize())
                .thenCompose(r -> accountAgeService.initialize())
                .thenCompose(r -> signedWitnessService.initialize())
                .thenCompose(r -> profileAgeService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        return proofOfBurnService.shutdown()
                .thenCompose(r -> bondedReputationService.shutdown())
                .thenCompose(r -> accountAgeService.shutdown())
                .thenCompose(r -> signedWitnessService.shutdown())
                .thenCompose(r -> profileAgeService.shutdown());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ReputationScore getReputationScore(UserProfile userProfile) {
        return findReputationScore(userProfile).orElse(ReputationScore.NONE);
    }

    public Optional<ReputationScore> findReputationScore(UserProfile userProfile) {
        return findReputationScore(userProfile.getId());
    }

    public Optional<ReputationScore> findReputationScore(String userProfileId) {
        if (!scoreByUserProfileId.containsKey(userProfileId)) {
            return Optional.empty();
        }
        long score = scoreByUserProfileId.get(userProfileId);
        double fiveSystemScore = getFiveSystemScore(score);
        int index = getIndex(score, scoreByUserProfileId.values());
        int rank = scoreByUserProfileId.size() - index;
        double relativeRanking = (index + 1) / (double) scoreByUserProfileId.size();
        return Optional.of(new ReputationScore(score, fiveSystemScore, rank, relativeRanking));
    }

    private void onUserProfileScoreChanged(String userProfileId) {
        if (userProfileId == null) {
            return;
        }
        long score = proofOfBurnService.getScore(userProfileId) +
                bondedReputationService.getScore(userProfileId) +
                accountAgeService.getScore(userProfileId) +
                signedWitnessService.getScore(userProfileId) +
                profileAgeService.getScore(userProfileId);
        scoreByUserProfileId.put(userProfileId, score);
        changedUserProfileScore.set(userProfileId);
    }

    @VisibleForTesting
    static double getFiveSystemScore(long candidateScore) {
        if (candidateScore <= 1_199) {
            return 0;
        } else if (candidateScore <= 4_999) {
            return 0.5;
        } else if (candidateScore <= 14_999) {
            return 1;
        } else if (candidateScore <= 19_999) {
            return 1.5;
        } else if (candidateScore <= 24_999) {
            return 2;
        } else if (candidateScore <= 29_999) {
            return 2.5;
        } else if (candidateScore <= 34_999) {
            return 3;
        } else if (candidateScore <= 39_999) {
            return 3.5;
        } else if (candidateScore <= 59_999) {
            return 4;
        } else if (candidateScore <= 99_999) {
            return 4.5;
        } else {
            return 5;
        }
    }

    @VisibleForTesting
    static int getIndex(long candidateScore, Collection<Long> scores) {
        List<Long> list = new ArrayList<>(scores);
        Collections.sort(list);
        return list.indexOf(candidateScore);
    }
}
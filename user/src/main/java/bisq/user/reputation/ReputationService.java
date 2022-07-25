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

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.oracle.daobridge.DaoBridgeService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ReputationService implements Service {
    private final ProofOfBurnService proofOfBurnService;
    @Getter
    private final BondedReputationService bondedReputationService;
    private final Map<String, Long> scoreByUserProfileId = new ConcurrentHashMap<>();
    @Getter
    protected final Observable<String> changedUserProfileScore = new Observable<>();
    @Getter
    private final AccountAgeService accountAgeService;

    public ReputationService(String baseDir,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService,
                             DaoBridgeService daoBridgeService) {
        proofOfBurnService = new ProofOfBurnService(networkService,
                userIdentityService,
                userProfileService);
        bondedReputationService = new BondedReputationService(networkService,
                userIdentityService,
                userProfileService);
        accountAgeService = new AccountAgeService(baseDir,
                networkService,
                userIdentityService,
                userProfileService,
                daoBridgeService);

        proofOfBurnService.getChangedUserProfileScore().addObserver(this::onUserProfileScoreChanged);
        bondedReputationService.getChangedUserProfileScore().addObserver(this::onUserProfileScoreChanged);
        accountAgeService.getChangedUserProfileScore().addObserver(this::onUserProfileScoreChanged);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return proofOfBurnService.initialize()
                .thenCompose(r -> bondedReputationService.initialize())
                .thenCompose(r -> accountAgeService.initialize());
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return proofOfBurnService.shutdown()
                .thenCompose(r -> bondedReputationService.shutdown())
                .thenCompose(r -> accountAgeService.shutdown());
    }

    public ReputationScore getReputationScore(UserProfile userProfile) {
        return findReputationScore(userProfile).orElse(ReputationScore.NONE);
    }

    public Optional<ReputationScore> findReputationScore(UserProfile userProfile) {
        String userProfileId = userProfile.getId();
        if (!scoreByUserProfileId.containsKey(userProfileId)) {
            return Optional.empty();
        }
        long score = scoreByUserProfileId.get(userProfileId);
        double relativeScore = getRelativeScore(score, scoreByUserProfileId.values());
        int index = getIndex(score, scoreByUserProfileId.values());
        int rank = scoreByUserProfileId.size() - index;
        double relativeRanking = (index + 1) / (double) scoreByUserProfileId.size();
        return Optional.of(new ReputationScore(score, relativeScore, rank, relativeRanking));
    }

    private void onUserProfileScoreChanged(String userProfileId) {
        if (userProfileId == null) {
            return;
        }
        long score = proofOfBurnService.getScore(userProfileId) +
                bondedReputationService.getScore(userProfileId) +
                accountAgeService.getScore(userProfileId);
        scoreByUserProfileId.put(userProfileId, score);
        changedUserProfileScore.set(userProfileId);
    }

    @VisibleForTesting
    static double getRelativeScore(long candidateScore, Collection<Long> scores) {
        long bestScore = scores.stream().max(Comparator.comparing(Long::longValue)).orElse(0L);
        return bestScore > 0 ? candidateScore / (double) bestScore : 0;
    }

    @VisibleForTesting
    static int getIndex(long candidateScore, Collection<Long> scores) {
        List<Long> list = new ArrayList<>(scores);
        Collections.sort(list);
        return list.indexOf(candidateScore);
    }
}
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

package bisq.burningman.fee;

import bisq.burningman.AuthorizedBurningmanListByBlock;
import bisq.burningman.BurningmanService;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FeeReceiverService implements Service {
    static final String LEGACY_BURNING_MAN_BTC_FEES_ADDRESS = "38bZBj5peYS3Husdz7AH3gEUiUbYRD951t";

    private final BurningmanService burningmanService;
    private final List<AuthorizedBurningmanListByBlock> authorizedBurningmanListByBlockList = new ArrayList<>();
    private Pin authorizedBurningmanDataSetPin;

    public FeeReceiverService(BurningmanService burningmanService) {
        this.burningmanService = burningmanService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        authorizedBurningmanDataSetPin = burningmanService.getAuthorizedBurningmanListByBlockSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedBurningmanListByBlock data) {
                authorizedBurningmanListByBlockList.add(data);
                authorizedBurningmanListByBlockList.sort(Comparator.comparing(AuthorizedBurningmanListByBlock::getBlockHeight));
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedBurningmanListByBlock data) {
                    authorizedBurningmanListByBlockList.remove(data);
                }
            }

            @Override
            public void clear() {
                authorizedBurningmanListByBlockList.clear();
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (authorizedBurningmanDataSetPin != null) {
            authorizedBurningmanDataSetPin.unbind();
            authorizedBurningmanDataSetPin = null;
        }
        authorizedBurningmanListByBlockList.clear();
        return CompletableFuture.completedFuture(true);
    }

    public String getAddress() {

        if (authorizedBurningmanListByBlockList.isEmpty()) {
            log.warn("AuthorizedBurningmanListByBlockList is empty. We use fallback to LEGACY_BURNING_MAN_BTC_FEES_ADDRESS: {}",
                    LEGACY_BURNING_MAN_BTC_FEES_ADDRESS);
            return LEGACY_BURNING_MAN_BTC_FEES_ADDRESS;
        }

        // It might be that we do not reach 100% if some entries had a cappedBurnAmountShare.
        // In that case we fill up the gap to 100% with the legacy BM.
        // cappedBurnAmountShare is a % value represented as double. Smallest supported value is 0.01% -> 0.0001.
        // By multiplying it with 10000 and using Math.floor we limit the candidate to 0.01%.
        // Entries with 0 will be ignored in the selection method, so we do not need to filter them out.
        int ceiling = 10000;
      /*  List<Long> amountList = activeBurningManCandidates.stream()
                .map(BurningManCandidate::getCappedBurnAmountShare)
                .map(cappedBurnAmountShare -> (long) Math.floor(cappedBurnAmountShare * ceiling))
                .collect(Collectors.toList());
        long sum = amountList.stream().mapToLong(e -> e).sum();
        // If we have not reached the 100% we fill the missing gap with the legacy BM
        if (sum < ceiling) {
            amountList.add(ceiling - sum);
        }

        int winnerIndex = getRandomIndex(amountList, new Random());
        if (winnerIndex == activeBurningManCandidates.size()) {
            // If we have filled up the missing gap to 100% with the legacy BM we would get an index out of bounds of
            // the burningManCandidates as we added for the legacy BM an entry at the end.
            return LEGACY_BURNING_MAN_BTC_FEES_ADDRESS;
        }
        return activeBurningManCandidates.get(winnerIndex).getReceiverAddress()
                .orElse(LEGACY_BURNING_MAN_BTC_FEES_ADDRESS);*/

        return null;
    }

    @VisibleForTesting
    static int getRandomIndex(List<Long> weights, Random random) {
        long sum = weights.stream().mapToLong(n -> n).sum();
        if (sum == 0) {
            return -1;
        }
        long target = random.longs(0, sum).findFirst().orElseThrow() + 1;
        return findIndex(weights, target);
    }

    @VisibleForTesting
    static int findIndex(List<Long> weights, long target) {
        int currentRange = 0;
        for (int i = 0; i < weights.size(); i++) {
            currentRange += weights.get(i);
            if (currentRange >= target) {
                return i;
            }
        }
        return 0;
    }
}

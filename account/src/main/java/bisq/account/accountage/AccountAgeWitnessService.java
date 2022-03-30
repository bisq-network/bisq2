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

package bisq.account.accountage;

import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Todo add support for open timestamp to ensure that data which got added was not backdated
 * We should be compatible with bisq 1 data so users do not loose their account age when the migrate to bisq 2
 */
@Slf4j
public class AccountAgeWitnessService implements DataService.Listener {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final boolean doTestPublish = false;

    public AccountAgeWitnessService(NetworkService networkService, IdentityService identityService) {
        this.networkService = networkService;
        this.identityService = identityService;

        networkService.addDataServiceListener(this);
    }

    public CompletableFuture<Boolean> initialize() {
        if (doTestPublish) {
            return publishAppendOnlyData(DigestUtil.hash("test".getBytes(StandardCharsets.UTF_8)));
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    // Will be called at account creation
    public CompletableFuture<Boolean> publishAppendOnlyData(byte[] hash) {
        AccountAgeWitnessData accountAgeWitnessData = new AccountAgeWitnessData(hash, new Date().getTime());
        try {
            return identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                    .thenCompose(identity -> networkService.publishAppendOnlyData(accountAgeWitnessData,
                            identity.getNodeIdAndKeyPair()))
                    .thenApply(broadCastDataResult -> true);
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public void onAppendOnlyDataAdded(AppendOnlyData appendOnlyData) {
        if (appendOnlyData instanceof AccountAgeWitnessData accountAgeWitnessData) {
            log.info("onAccountAgeWitnessDataAdded {}", accountAgeWitnessData);
        }
    }
}
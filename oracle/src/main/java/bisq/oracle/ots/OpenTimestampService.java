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

package bisq.oracle.ots;


import bisq.common.data.ByteArray;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.identity.IdentityService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import com.eternitywall.ots.DetachedTimestampFile;
import com.eternitywall.ots.OpenTimestamps;
import com.eternitywall.ots.Timestamp;
import com.eternitywall.ots.VerifyResult;
import com.eternitywall.ots.exceptions.DeserializationException;
import com.eternitywall.ots.op.OpSHA256;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * todo
 * add identity selection strategy. E.g. one identity per domain ID or one identity per context
 * type (e.g. fiat trades) or one global identity...
 * Add support for userName mapping with identity (not sure if should be done here or in social module)
 */
@Slf4j
public class OpenTimestampService implements PersistenceClient<OpenTimestampStore> {

    @Getter
    private final OpenTimestampStore persistableStore = new OpenTimestampStore();
    @Getter
    private final Persistence<OpenTimestampStore> persistence;

    private IdentityService identityService;
    @Getter
    private final List<String> calendars;

    public record Config(List<String> calendars) {
        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            return new Config(typeSafeConfig.getStringList("calendars"));
        }
    }

    public OpenTimestampService(IdentityService identityService, PersistenceService persistenceService, Config config) {
        this.identityService = identityService;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        calendars = config.calendars();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        Scheduler.run(this::maybeCreateOrUpgradeTimestampsOfActiveIdentities)
                .periodically(1, TimeUnit.HOURS)
                .name("Manage-timestamps");
        CompletableFuture.runAsync(this::maybeCreateOrUpgradeTimestampsOfActiveIdentities,
                ExecutorFactory.newSingleThreadExecutor("Manage-timestamps"));
        return CompletableFuture.completedFuture(true);
    }


    /**
     * @return Creation date of identity. If we have a verified OTS date we use that, otherwise we return current time.
     */
    public long getCreationDate(ByteArray pubKeyHash) {
        long verifiedOtsDate = getVerifiedOtsDate(pubKeyHash);
        return verifiedOtsDate > 0 ? verifiedOtsDate : System.currentTimeMillis();
    }

    /**
     * @return Verified OTS date or null if no timestamp exists or if it is not completed.
     */
    public long getVerifiedOtsDate(ByteArray pubKeyHash) {
        return Optional.ofNullable(persistableStore.getTimestampByPubKeyHash().get(pubKeyHash))
                .map(timestamp -> getTimestampDate(pubKeyHash.getBytes(), timestamp))
                .orElse(0L);
    }

    /**
     * Check for given identity if we have a completed timestamp. Request or upgrade timestamp if required.
     */
    public void maybeCreateOrUpgradeTimestamp(ByteArray pubKeyHash) {
        Optional.ofNullable(persistableStore.getTimestampByPubKeyHash().get(pubKeyHash))
                .ifPresentOrElse(timestamp -> {
                    if (isTimestampComplete(timestamp)) {
                        log.info("Timestamp of pubKeyHash {} is already complete", pubKeyHash);
                    } else {
                        // Existing timestamp is not completed yet, we request an upgrade.
                        requestTimestampUpgrade(pubKeyHash, timestamp);
                    }
                }, () -> {
                    // If we don't had already a timestamp created we request one  
                    requestTimestamp(pubKeyHash);
                });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void maybeCreateOrUpgradeTimestampsOfActiveIdentities() {
        identityService.getActiveIdentityByDomainId().values().stream()
                .map(identity -> new ByteArray(identity.pubKey().hash()))
                .forEach(this::maybeCreateOrUpgradeTimestamp);
    }

    private void requestTimestampUpgrade(ByteArray data, ByteArray timestamp) {
        log.info("Request upgrade for timestamp of data {} from calendars {}", data, calendars);
        long ts = System.currentTimeMillis();
        // Blocking call to the ots server
        doRequestUpgrade(timestamp)
                .ifPresent(upgradedTimestamp -> {
                    persistableStore.getTimestampByPubKeyHash().put(data, upgradedTimestamp);
                    persist();
                    log.info("Upgrade request for timestamp of data {} completed after {} ms. isTimestampComplete={}",
                            data,
                            System.currentTimeMillis() - ts,
                            isTimestampComplete(timestamp));
                });
    }

    private void requestTimestamp(ByteArray data) {
        log.info("Request timestamp of data {} from calendars {}", data, calendars);
        long ts = System.currentTimeMillis();
        // Blocking call to the ots server. Takes about 2 sec on clear net
        doRequestTimestamp(data)
                .ifPresent(timestamp -> {
                    log.info("Response for timestamp of data {} received after {} ms. Timestamp: {} ",
                            data, System.currentTimeMillis() - ts, timestamp);
                    persistableStore.getTimestampByPubKeyHash().put(data, timestamp);
                    persist();
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private OTS library methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Optional<ByteArray> doRequestTimestamp(ByteArray data) {
        try {
            byte[] hash = new OpSHA256().call(data.getBytes());
            DetachedTimestampFile ots = DetachedTimestampFile.from(new OpSHA256(), hash);
            //This step is needed to actually stamp the DetachedTimestamp ots
            Timestamp stampResult = OpenTimestamps.stamp(ots, calendars, null);
            log.info("The timestamp proof has been created. Detached TimestampFile: {}", OpenTimestamps.info(ots));
            return Optional.of(new ByteArray(ots.serialize()));
        } catch (Exception e) {
            log.error("Requesting timestamp failed", e);
            return Optional.empty();
        }
    }

    private Optional<ByteArray> doRequestUpgrade(ByteArray ots) {
        try {
            DetachedTimestampFile deserializedOts = DetachedTimestampFile.deserialize(ots.getBytes());
            try {
                // The upgrade method changes the deserializedOts as side effect!
                // It is blocking call to the ots server
                // todo Assuming the if change happened the upgrade does not change the ots, so when we return the ots
                // it is irrelevant if it was upgraded or not.
                boolean timestampChanged = OpenTimestamps.upgrade(deserializedOts);
                if (!timestampChanged) {
                    log.warn("Timestamp has not changed in upgrade call. Clients calling that method should check if " +
                            "the timestamp was not already completed.");
                }
                return Optional.of(new ByteArray(deserializedOts.serialize()));
            } catch (Exception e) {
                log.error("Upgrading timestamp failed", e);
                return Optional.empty();
            }

        } catch (DeserializationException e) {
            log.error("Deserializing timestamp failed", e);
            return Optional.empty();
        }
    }

    private Map<VerifyResult.Chains, VerifyResult> getVerifyResultByChains(byte[] data, ByteArray ots) throws Exception {
        DetachedTimestampFile stamped = DetachedTimestampFile.from(new OpSHA256(), data);
        DetachedTimestampFile deserializedOts = DetachedTimestampFile.deserialize(ots.getBytes());
        return OpenTimestamps.verify(deserializedOts, stamped);
    }

    public long getTimestampDate(byte[] data, ByteArray ots) {
        try {
            Map<VerifyResult.Chains, VerifyResult> verifyResultByChains = getVerifyResultByChains(data, ots);
            return getTimestampDate(verifyResultByChains);
        } catch (Exception e) {
            log.error("getBestTimeStamp failed", e);
            return 0;
        }
    }

    private long getTimestampDate(Map<VerifyResult.Chains, VerifyResult> verifyResultByChains) throws Exception {
        if (isValid(verifyResultByChains)) {
            return verifyResultByChains.values().stream().mapToLong(e -> e.timestamp).min().orElse(0);
        } else {
            return 0;
        }
    }

    private boolean isValid(Map<VerifyResult.Chains, VerifyResult> verifyResultByChains) {
        return verifyResultByChains != null && !verifyResultByChains.isEmpty();
    }

    private boolean isTimestampComplete(ByteArray ots) {
        try {
            return DetachedTimestampFile.deserialize(ots.getBytes()).getTimestamp().isTimestampComplete();
        } catch (DeserializationException e) {
            log.error("Deserializing timestamp failed", e);
            return false;
        }
    }
}

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

package bisq.application.compat.v2_1_15;

import bisq.application.ResolverConfig;
import bisq.application.compat.CompatSupport;
import bisq.common.application.DevMode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import static bisq.application.compat.CompatSupport.NEW_REQUESTS_FILE;
import static bisq.application.compat.CompatSupport.RELAYED_COPIES_FILE;
import static bisq.application.compat.CompatSupport.applicationData;
import static bisq.application.compat.CompatSupport.flags;
import static bisq.application.compat.CompatSupport.parse;
import static bisq.application.compat.CompatSupport.proofOfWorkPayloadHash;
import static bisq.network.p2p.services.data.storage.SignedRequest.SIGNATURE_V1_CUTOFF_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 4 of the compatibility chain, with the code of this version. A node of v2.1.15 relayed the requests of the new
 * version in step 3. v2.1.15 knows signature V2 and keeps it, so nodes of this version accept the copies before and
 * after the cutoff date of signature V1 (docs/specifications/network/storage-request-signatures.md).
 */
@EnabledIfSystemProperty(named = CompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step4NewNodeProcessesRelayedCopiesTest {
    private static final long BEFORE_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime() - 1;
    private static final long AT_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime();

    // Copies which a node of v2.1.15 relays, in a natural order of arrival. The refresh request and the refreshed add
    // request need the earlier add requests of the user profile, which a fresh node does not hold.
    private static final List<String> RELAYED_REQUESTS = List.of(
            "addRole4", "addUserProfile", "addChat4", "removeChat4", "addMailbox4", "removeMailbox4");

    private static Properties updated;
    private static Properties relayed;

    @BeforeAll
    static void readTheRelayedCopies() throws Exception {
        DevMode.setDevMode(true);
        ResolverConfig.config();
        updated = CompatSupport.read(NEW_REQUESTS_FILE);
        relayed = CompatSupport.read(RELAYED_COPIES_FILE);
    }

    @Test
    void relayedCopiesKeepTheProofOfWorkPayloadAndSignatureV2() throws Exception {
        for (String name : CompatSupport.namesWithPrefix(updated, "request.")) {
            EnvelopePayloadMessage copy = relayedCopy(name);
            assertEquals(updated.getProperty("pow." + name), proofOfWorkPayloadHash(copy),
                    name + ": a node of this version would reject the proof of work of a relay of v2.1.15");
            assertTrue(((SignedRequest) copy).isSignatureV2Present(), name + ": v2.1.15 must keep signature V2");
        }
    }

    @Test
    void freshNodeAcceptsRelayedCopiesBeforeTheCutoff() throws Exception {
        assertFreshNodeAcceptsRelayedCopies("step4-fresh-node-before-cutoff", BEFORE_CUTOFF);
    }

    @Test
    void freshNodeAcceptsRelayedCopiesFromTheCutoffOn() throws Exception {
        assertFreshNodeAcceptsRelayedCopies("step4-fresh-node-at-cutoff", AT_CUTOFF);
    }

    private static void assertFreshNodeAcceptsRelayedCopies(String workDirName, long now) throws Exception {
        DataService node = new DataService(new PersistenceService(CompatSupport.newWorkDir(workDirName)), () -> now);
        for (String name : RELAYED_REQUESTS) {
            Set<String> result = flags(CompatSupport.process(node.getStorageService(), relayedCopy(name)));
            assertTrue(result.contains("success"), name + ": " + result);
        }
        assertEquals(List.of("AuthorizedBondedRole", "UserProfile"), applicationData(node));
        node.shutdown();
    }

    private static EnvelopePayloadMessage relayedCopy(String name) throws Exception {
        return parse(relayed.getProperty("relayed." + name));
    }
}

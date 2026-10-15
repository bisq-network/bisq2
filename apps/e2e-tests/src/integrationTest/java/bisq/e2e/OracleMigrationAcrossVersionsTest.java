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

package bisq.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The migration of the publisher of the data of an oracle node across the versions
 * (oracle-publisher-migration-across-versions; the part of staging-oracle-dry-run of the release test plan which needs
 * no Bisq 1 data). Oracle nodes of v2.1.13 published their data with the key pair of their network ID. Applications of
 * this checkout use data only from its owner, the authorized key (rule 11 of
 * docs/specifications/network/storage-ownership.md). So the oracle nodes update first and publish their data again with
 * the authorized key (rules 14 and 15). Nodes of v2.1.13 store one entry per data, and the new entry replaces the old
 * one because of its higher sequence number.
 * <ul>
 *     <li>The authorized data publisher acts as a root oracle node of v2.1.13. It publishes its data and the roles of
 *     two mediators, one of each version, with the key of its network ID. Nodes of v2.1.13 use the roles, and nodes of
 *     this checkout do not: a trade between two nodes of v2.1.13 has a mediator, a trade between two nodes of this
 *     checkout has none. Takes between the versions would fail, because maker and taker select different mediators.</li>
 *     <li>The oracle node runs the migration of this checkout. Then makers and takers of both versions select the same
 *     mediators, also nodes which start after the migration.</li>
 *     <li>The oracle node removes the role of one mediator with its authorized key. Both versions then select only the
 *     other mediator.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class OracleMigrationAcrossVersionsTest {
    private static final String ORACLE = "oracle";
    private static final String MEDIATOR_OLD = "mediator-old";
    private static final String MEDIATOR_NEW = "mediator-new";
    // Registrations of v2.1.13 have no protocol version, which counts as the legacy version
    private static final int LEGACY_REGISTRATION = 1;
    // The role of the oracle node and the roles of the two mediators
    private static final int ROLES = 3;
    // A node logs the change of its store before it notifies the service of the roles
    private static final Duration ROLE_APPLIED = Duration.ofSeconds(3);

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("oracle-migration-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void bothVersionsUseTheRolesOfAnOracleAfterItsMigration() throws Exception {
        List<Integer> seeds = List.of(network.oldSeed.getP2pPort(), network.newSeed.getP2pPort());
        ApiClient oracle = network.startAuthorizedDataPublisher(ORACLE);
        ApiClient mediatorOld = network.startExtraApiNode(MEDIATOR_OLD, false, seeds);
        ApiClient mediatorNew = network.startExtraApiNode(MEDIATOR_NEW, true, seeds);
        String mediatorOldId = mediatorOld.userProfileId();
        String mediatorNewId = mediatorNew.userProfileId();

        // The oracle node of v2.1.13 publishes its data and the roles of the mediators with the key of its network ID
        network.runPublisherCommand(ORACLE, "oracle-legacy");
        for (String mediatorId : List.of(mediatorNewId, mediatorOldId)) {
            MixedVersionNetwork.await("the oracle knows the profile of the mediator", Trades.STEP_TIMEOUT,
                    () -> oracle.knowsUserProfile(mediatorId));
            network.runPublisherCommand(ORACLE, "mediator-legacy " + mediatorId + " " + LEGACY_REGISTRATION);
        }
        awaitRolesOnNodes(List.of(network.oldNode.getName(), network.newNode.getName(), MEDIATOR_OLD, MEDIATOR_NEW),
                ROLES);

        // Nodes of v2.1.13 use these roles, nodes of this version do not. A mediator does not mediate its own trade.
        String tradeOfOldNodes = Mediators.takeAndCompareMediators(network, network.oldApi, mediatorOld, "SELL");
        assertEquals(mediatorNewId, Mediators.mediatorOf(network.oldApi, tradeOfOldNodes),
                "Nodes of v2.1.13 must use the roles which the oracle node of v2.1.13 published");
        String tradeOfNewNodes = Mediators.takeAndCompareMediators(network, network.newApi, mediatorNew, "SELL");
        assertEquals("", Mediators.mediatorOf(network.newApi, tradeOfNewNodes),
                "Nodes of this version must not use roles which the authorized key did not publish");

        // The oracle node runs the migration. Each node then stores the roles of the authorized key as well: nodes of
        // v2.1.13 replace their entries, nodes of this version store new entries.
        network.runPublisherCommand(ORACLE, "migrate");
        awaitRolesOnNodes(List.of(network.oldNode.getName(), network.newNode.getName()), 2 * ROLES);
        Mediators.assertEachMediatorIsSelected(network, Set.of(mediatorOldId, mediatorNewId), network.oldApi,
                network.newApi);

        // Nodes which start after the migration
        ApiClient lateOld = network.startExtraApiNode("late-old", false, seeds);
        ApiClient lateNew = network.startExtraApiNode("late-new", true, seeds);
        awaitRolesOnNodes(List.of("late-old", "late-new"), ROLES);
        Mediators.assertEachMediatorIsSelected(network, Set.of(mediatorOldId, mediatorNewId), lateOld, lateNew);

        // The oracle node removes a role with its authorized key, and both versions apply the removal. The migration
        // removed entries as well, so the test counts the removals before.
        List<String> traders = List.of(network.oldNode.getName(), network.newNode.getName());
        Map<String, Long> removedBefore = new HashMap<>();
        traders.forEach(nodeName -> removedBefore.put(nodeName,
                network.countLogLines(nodeName, Mediators.ROLE_REMOVED)));
        network.runPublisherCommand(ORACLE, "remove-mediator " + mediatorNewId);
        for (String nodeName : traders) {
            Mediators.awaitLogLines(network, nodeName, Mediators.ROLE_REMOVED, removedBefore.get(nodeName) + 1,
                    nodeName + " removes the role of the new mediator");
        }
        Thread.sleep(ROLE_APPLIED.toMillis());
        Mediators.assertOnlyMediatorIsSelected(network, mediatorOldId, network.oldApi, network.newApi);
    }

    private void awaitRolesOnNodes(List<String> nodeNames, int roles) throws InterruptedException {
        for (String nodeName : nodeNames) {
            Mediators.awaitLogLines(network, nodeName, Mediators.ROLE_ADDED, roles,
                    nodeName + " stores " + roles + " roles");
        }
        Thread.sleep(ROLE_APPLIED.toMillis());
    }
}

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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A user of the released version edits the profile. The released version removes the profile with the old values by a
 * removal with only signature V1. Nodes of this checkout refuse such a removal of a user profile and keep the old
 * profile. Nodes of the released version accept it, and then reject the add request of the old profile, whose sequence
 * number is lower. A node of this checkout does not send them this add request in its inventory responses (rule 12 of
 * docs/specifications/network/inventory-hash-set-filter.md). Before that rule, every response contained it again.
 * <p>
 * The node of the released version logs the number of items of each type of every inventory response which it
 * receives. After a restart, it requests the inventory of its peers with a filter which lists the removal.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "bisq.e2e.newTree", matches = ".+")
class ProfileEditAcrossVersionsTest {
    private static final Duration PROPAGATION_TIMEOUT = Duration.ofMinutes(2);
    private static final String USER_PROFILE_ITEMS = "item(s) of AddAuthenticatedDataRequest.UserProfile";

    private MixedVersionNetwork network;

    @BeforeAll
    void startNetwork() throws Exception {
        network = new MixedVersionNetwork("profile-edit-across-versions");
    }

    @AfterAll
    void stopNetwork() throws Exception {
        if (network != null) {
            network.close();
        }
    }

    @Test
    void nodesOfThisCheckoutDoNotSendTheOldProfileAgain() throws Exception {
        String profileId = network.oldApi.userProfileId();
        MixedVersionNetwork.await("the new node knows the profile of the old node", PROPAGATION_TIMEOUT,
                () -> network.newApi.knowsUserProfile(profileId));

        network.oldApi.editUserProfile(profileId, "edited terms", "edited statement");
        MixedVersionNetwork.await("the new node shows the edited profile", PROPAGATION_TIMEOUT,
                () -> network.newApi.userProfile(profileId).path("statement").asText().equals("edited statement"));

        // With the first restart, the old node gets the requests which it missed, for example a profile which a node
        // published again while the old node was not connected. It stores them, so they are not in the later responses.
        restartOldNodeAndAwaitInventoryOfNewSeed();
        restartOldNodeAndAwaitInventoryOfNewSeed();

        List<String> reports = new ArrayList<>(inventoryReportsFrom(network.newSeed.getP2pPort()));
        reports.addAll(inventoryReportsFrom(network.newNode.getP2pPort()));
        assertFalse(reports.stream().anyMatch(report -> report.contains(USER_PROFILE_ITEMS)),
                "A node of this checkout sent a user profile to the old node:\n" + String.join("\n", reports));
    }

    private void restartOldNodeAndAwaitInventoryOfNewSeed() throws Exception {
        network.stopOldNode();
        network.startOldNode();
        MixedVersionNetwork.await("the old node received the inventory of the seed node of this checkout",
                PROPAGATION_TIMEOUT,
                () -> !inventoryReportsFrom(network.newSeed.getP2pPort()).isEmpty());
    }

    // The reports which the old node logged since its last start about the inventory responses of the peer. A report
    // ends with its second line of '#' characters.
    private List<String> inventoryReportsFrom(int peerPort) {
        List<String> lines;
        try {
            lines = network.logOfLastSession(network.oldNode.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String header = "of inventory data from: 127.0.0.1:" + peerPort + " ";
        List<String> reports = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains(header)) {
                continue;
            }
            StringBuilder report = new StringBuilder(lines.get(i));
            int numDelimiters = 0;
            for (int j = i + 1; j < lines.size() && numDelimiters < 2; j++) {
                if (lines.get(j).startsWith("#####")) {
                    numDelimiters++;
                }
                report.append('\n').append(lines.get(j));
            }
            reports.add(report.toString());
        }
        return reports;
    }
}

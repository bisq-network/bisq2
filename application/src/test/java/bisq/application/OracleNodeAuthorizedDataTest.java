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

package bisq.application;

import bisq.account.timestamp.AuthorizedAccountTimestamp;
import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.oracle.OracleNodeAuthorizedData;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData;
import bisq.burningman.AuthorizedBurningmanListByBlock;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.banned.BannedUserProfileData;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.data.AuthorizedTimestampData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * While only static oracle nodes are allowed, the network storage accepts the data which oracle nodes authorize only
 * from an oracle node key (rule 16 of docs/specifications/bonded-roles/authorized-data.md). The types declare this by
 * implementing OracleNodeAuthorizedData. Each authorized type must be listed here, so that a new type which oracle
 * nodes authorize cannot miss the requirement.
 */
class OracleNodeAuthorizedDataTest {
    // The services which use these types check them with BondedRoleType.ORACLE_NODE
    private static final Set<Class<?>> AUTHORIZED_BY_ORACLE_NODES = Set.of(AuthorizedOracleNode.class,
            AuthorizedBondedRole.class,
            AuthorizedMarketPriceData.class,
            AuthorizedBurningmanListByBlock.class,
            AuthorizedAccountTimestamp.class,
            AuthorizedAccountAgeData.class,
            AuthorizedBondedReputationData.class,
            AuthorizedProofOfBurnData.class,
            AuthorizedSignedWitnessData.class,
            AuthorizedTimestampData.class);
    // Security managers, release managers and moderators can hold a role without a static key (rule 3)
    private static final Set<Class<?>> AUTHORIZED_BY_OTHER_ROLES = Set.of(AuthorizedAlertData.class,
            AuthorizedDifficultyAdjustmentData.class,
            AuthorizedMinRequiredReputationScoreData.class,
            ReleaseNotification.class,
            BannedUserProfileData.class);

    @Test
    void everyAuthorizedTypeIsListed() throws Exception {
        Set<Class<?>> authorizedTypes = StorableTypes.find().stream()
                .filter(AuthorizedDistributedData.class::isAssignableFrom)
                .collect(Collectors.toSet());
        Set<Class<?>> listedTypes = new HashSet<>(AUTHORIZED_BY_ORACLE_NODES);
        listedTypes.addAll(AUTHORIZED_BY_OTHER_ROLES);

        assertEquals(listedTypes, authorizedTypes);
    }

    @Test
    void onlyTypesWhichOracleNodesAuthorizeRequireTheOracleNodeKeys() {
        AUTHORIZED_BY_ORACLE_NODES.forEach(type ->
                assertTrue(OracleNodeAuthorizedData.class.isAssignableFrom(type), type.getName()));
        AUTHORIZED_BY_OTHER_ROLES.forEach(type ->
                assertFalse(OracleNodeAuthorizedData.class.isAssignableFrom(type), type.getName()));
    }

    // The requirement does not depend on the static flag, which the publisher sets
    @Test
    void requirementFollowsTheRestrictionToStaticOracleNodes() {
        AuthorizedTimestampData timestamp = new AuthorizedTimestampData("a".repeat(40), 1_700_000_000_000L, false);

        assertEquals(AuthorizedBondedRolesService.ONLY_STATIC_ORACLE_NODES, timestamp.isStaticPublicKeyRequired());
        assertEquals(AuthorizedPubKeys.getOracleNodePubKeys(), timestamp.getAuthorizedPublicKeys());
    }
}

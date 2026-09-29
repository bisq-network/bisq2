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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.bonded_roles.release.AppType;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A security manager bans a bonded role with an alert. The role can arrive after the alert, for example with an
 * inventory response, or when an oracle node publishes it again (finding F12.10 of the review
 * 2026-09-29_16-18-18-fable-5.1).
 */
class AlertServiceBanTest {
    @Test
    void roleWhichArrivesAfterItsBanAlertIsBanned() throws GeneralSecurityException {
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.empty());
        AuthorizedBondedRolesService authorizedBondedRolesService = new AuthorizedBondedRolesService(networkService,
                false);
        AlertService alertService = new AlertService(authorizedBondedRolesService);
        KeyPair securityManagerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authorizedBondedRolesService.getBondedRoles().add(new BondedRole(createRole("a".repeat(40),
                Hex.encode(securityManagerKeyPair.getPublic().getEncoded()),
                BondedRoleType.SECURITY_MANAGER)));
        AuthorizedBondedRole mediatorRole = createRole("b".repeat(40), "public-key", BondedRoleType.MEDIATOR);
        AuthorizedAlertData banAlert = new AuthorizedAlertData("id",
                System.currentTimeMillis(),
                AlertType.BAN,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Optional.empty(),
                Optional.of(mediatorRole),
                "a".repeat(40),
                false,
                Optional.empty(),
                AppType.DESKTOP);
        alertService.onAuthorizedDataAdded(new AuthorizedData(banAlert,
                Optional.of(SignatureUtil.sign(banAlert.serializeForHash(), securityManagerKeyPair.getPrivate())),
                securityManagerKeyPair.getPublic()));

        // As AuthorizedBondedRolesService does: it adds the role and then notifies its listeners
        BondedRole bondedRole = new BondedRole(mediatorRole);
        authorizedBondedRolesService.getBondedRoles().add(bondedRole);
        KeyPair oracleKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        alertService.onAuthorizedDataAdded(new AuthorizedData(mediatorRole,
                Optional.of(SignatureUtil.sign(mediatorRole.serializeForHash(), oracleKeyPair.getPrivate())),
                oracleKeyPair.getPublic()));

        assertTrue(bondedRole.isBanned());
    }

    private static AuthorizedBondedRole createRole(String profileId, String authorizedPublicKey, BondedRoleType type) {
        return new AuthorizedBondedRole(profileId,
                authorizedPublicKey,
                type,
                "user " + profileId.charAt(0),
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);
    }
}

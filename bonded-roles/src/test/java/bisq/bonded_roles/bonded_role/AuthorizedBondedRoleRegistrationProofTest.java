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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static bisq.bonded_roles.registration.BondedRoleRegistrationRequestTest.createNetworkId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AuthorizedBondedRoleRegistrationProofTest {
    private static final String PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";
    private static final String PROPOSAL_TX_ID = "a".repeat(64);
    private static final String LOCKUP_TX_ID = "b".repeat(64);
    private static final String GOLDEN_PUBLIC_KEY =
            "3056301006072a8648ce3d020106052b8104000a03420004351cf590bd033522" +
                    "c857b2026c93ab5a0ad3888ac3a2cfc8301fbf67990d1c2c36eb26ae3b686" +
                    "d7262f544a9dba2491772ae0d2bc0b1067a72896a98525c052e";
    private static final String LEGACY_VERSION_ONE_HASH_SERIALIZATION =
            "0a28303132333435363738396162636465663031323334353637383961626364" +
                    "65663031323334353637120a7075626c69632d6b657918012209626f6e642d" +
                    "757365722a097369676e61747572653a81010a190a170a05434c454152120e" +
                    "0a093132372e302e302e3110a41312640a583056301006072a8648ce3d0201" +
                    "06052b8104000a03420004351cf590bd033522c857b2026c93ab5a0ad3888a" +
                    "c3a2cfc8301fbf67990d1c2c36eb26ae3b686d7262f544a9dba2491772ae" +
                    "0d2bc0b1067a72896a98525c052e1208746573742d6b6579";

    @Test
    void legacyAuthorizedRoleKeepsVersionOneHashShapeAndEmptyProof() {
        AuthorizedBondedRole legacy = new AuthorizedBondedRole(PROFILE_ID,
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false);

        AuthorizedBondedRole roundTrip = AuthorizedBondedRole.fromProto(legacy.toProto(false)
                .toBuilder()
                .clearRegistrationProtocolVersion()
                .build());

        assertThat(legacy.getVersion()).isEqualTo(1);
        assertThat(roundTrip.getRegistrationProtocolVersion()).isEqualTo(BondedRoleRegistrationProtocol.LEGACY_VERSION);
        assertThat(roundTrip.getProposalTxId()).isEmpty();
        assertThat(roundTrip.getLockupTxId()).isEmpty();
        assertThat(roundTrip.serializeForHash()).isEqualTo(legacy.serializeForHash());
    }

    @Test
    void legacyHashSerializationMatchesTheDeployedVersionOneSchema() {
        AuthorizedBondedRole legacy = new AuthorizedBondedRole(PROFILE_ID,
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                Optional.empty(),
                createGoldenNetworkId(),
                Optional.of(createGoldenOracleNode()),
                true);

        assertThat(Hex.encode(legacy.serializeForHash())).isEqualTo(LEGACY_VERSION_ONE_HASH_SERIALIZATION);
    }

    @Test
    void versionTwoAuthorizedRoleSignsTheExactRegistrationBinding() {
        AuthorizedBondedRole first = createBoundRole(LOCKUP_TX_ID);
        AuthorizedBondedRole second = createBoundRole("c".repeat(64));

        AuthorizedBondedRole roundTrip = AuthorizedBondedRole.fromProto(first.toProto(false));

        assertThat(first.getVersion()).isEqualTo(2);
        assertThat(roundTrip).isEqualTo(first);
        assertThat(first.serializeForHash()).isNotEqualTo(second.serializeForHash());
    }

    @Test
    void domainValidationRejectsAProtocolVersionWithoutTheMatchingNetworkDataVersion() {
        bisq.bonded_roles.protobuf.AuthorizedBondedRole malformed = createBoundRole(LOCKUP_TX_ID)
                .toProto(false)
                .toBuilder()
                .setVersion(1)
                .build();

        AuthorizedBondedRole resolved = AuthorizedBondedRole.fromProto(malformed);

        assertThatIllegalArgumentException().isThrownBy(resolved::verifyRegistration);
    }

    @Test
    void futureDataVersionRemainsResolvableWithoutApplyingKnownVersionMappings() {
        bisq.bonded_roles.protobuf.AuthorizedBondedRole future = createBoundRole(LOCKUP_TX_ID)
                .toProto(false)
                .toBuilder()
                .setVersion(3)
                .build();

        AuthorizedBondedRole resolved = AuthorizedBondedRole.fromProto(future);

        assertThat(resolved.getVersion()).isEqualTo(3);
        assertThatCode(resolved::verifyRegistration).doesNotThrowAnyException();
    }

    @Test
    void networkDataRejectsNonHexTransactionIds() {
        assertThatIllegalArgumentException().isThrownBy(() -> createBoundRole("z".repeat(64)));
    }

    private static AuthorizedBondedRole createBoundRole(String lockupTxId) {
        return new AuthorizedBondedRole(PROFILE_ID,
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                Optional.empty(),
                createNetworkId(),
                Optional.empty(),
                false,
                BondedRoleRegistrationProtocol.CURRENT_VERSION,
                PROPOSAL_TX_ID,
                lockupTxId);
    }

    private static NetworkId createGoldenNetworkId() {
        var addressMap = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(1234)));
        return new NetworkId(addressMap,
                new PubKey(KeyGeneration.getPublicKeyFromHex(GOLDEN_PUBLIC_KEY), "test-key"));
    }

    private static AuthorizedOracleNode createGoldenOracleNode() {
        return new AuthorizedOracleNode(createGoldenNetworkId(),
                "f".repeat(40),
                "oracle-public-key",
                "oracle",
                "oracle-signature",
                true);
    }
}

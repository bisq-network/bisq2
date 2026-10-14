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

package bisq.support.mediation.bisq_easy;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyMediationRequestServiceTest {

    private static final String OFFER_ID = "offer-test-123";

    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private UserProfileService userProfileService;
    private BisqEasyMediationRequestService service;

    private String makerProfileId;
    private String takerProfileId;

    @BeforeEach
    void set_up() {
        NetworkService networkService = mock(NetworkService.class);
        ChatService chatService = mock(ChatService.class);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(channelService);

        UserService userService = mock(UserService.class);
        userProfileService = mock(UserProfileService.class);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);

        BondedRolesService bondedRolesService = mock(BondedRolesService.class);
        authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(bondedRolesService.getAuthorizedBondedRolesService()).thenReturn(authorizedBondedRolesService);

        service = new BisqEasyMediationRequestService(networkService, chatService, userService, bondedRolesService);

        makerProfileId = padId("maker");
        takerProfileId = padId("taker");
    }

    @Test
    @DisplayName("selectMediator returns empty when no mediators are registered")
    void select_mediator_returns_empty_when_no_mediators() {
        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).thenReturn(Stream.empty());

        Optional<UserProfile> result = service.selectMediator(makerProfileId, takerProfileId, OFFER_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("selectMediator excludes maker and taker from candidates")
    void select_mediator_excludes_maker_and_taker() {
        AuthorizedBondedRole makerRole = createRole(makerProfileId);
        AuthorizedBondedRole takerRole = createRole(takerProfileId);
        RoleWithId mediator = createRoleWithId();

        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream())
                .thenReturn(Stream.of(makerRole, takerRole, mediator.role));

        UserProfile mediatorProfile = createUserProfile(6001);
        when(userProfileService.findUserProfile(mediator.profileId)).thenReturn(Optional.of(mediatorProfile));

        Optional<UserProfile> result = service.selectMediator(makerProfileId, takerProfileId, OFFER_ID);

        assertTrue(result.isPresent());
        assertEquals(mediatorProfile, result.get());
    }

    @Test
    @DisplayName("selectMediator returns empty when all mediators are maker or taker")
    void select_mediator_returns_empty_when_all_excluded() {
        AuthorizedBondedRole makerRole = createRole(makerProfileId);
        AuthorizedBondedRole takerRole = createRole(takerProfileId);

        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream())
                .thenReturn(Stream.of(makerRole, takerRole));

        Optional<UserProfile> result = service.selectMediator(makerProfileId, takerProfileId, OFFER_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("selectMediator returns empty when selected profile is not found in user profiles")
    void select_mediator_returns_empty_when_profile_not_found() {
        RoleWithId role = createRoleWithId();

        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream()).thenReturn(Stream.of(role.role));
        when(userProfileService.findUserProfile(anyString())).thenReturn(Optional.empty());

        Optional<UserProfile> result = service.selectMediator(makerProfileId, takerProfileId, OFFER_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("selectMediator with explicit candidate set returns deterministic result")
    void select_mediator_with_explicit_set_returns_deterministic_result() {
        RoleWithId a = createRoleWithId();
        RoleWithId b = createRoleWithId();
        RoleWithId c = createRoleWithId();
        Set<AuthorizedBondedRole> candidates = Set.of(a.role, b.role, c.role);

        UserProfile profileA = createUserProfile(7001);
        UserProfile profileB = createUserProfile(7002);
        UserProfile profileC = createUserProfile(7003);
        when(userProfileService.findUserProfile(a.profileId)).thenReturn(Optional.of(profileA));
        when(userProfileService.findUserProfile(b.profileId)).thenReturn(Optional.of(profileB));
        when(userProfileService.findUserProfile(c.profileId)).thenReturn(Optional.of(profileC));

        Optional<UserProfile> first = service.selectMediator(candidates, makerProfileId, takerProfileId, OFFER_ID);
        Optional<UserProfile> second = service.selectMediator(candidates, makerProfileId, takerProfileId, OFFER_ID);

        assertTrue(first.isPresent());
        assertEquals(first.get(), second.get());
    }

    @Test
    @DisplayName("selectMediator only includes MEDIATOR bond types")
    void select_mediator_only_includes_mediator_bond_types() {
        RoleWithId mediator = createRoleWithId();
        AuthorizedBondedRole oracleRole = createRoleWithType(padId("oracl"), BondedRoleType.ORACLE_NODE);

        when(authorizedBondedRolesService.getAuthorizedBondedRoleStream())
                .thenReturn(Stream.of(mediator.role, oracleRole));

        UserProfile mediatorProfile = createUserProfile(8001);
        when(userProfileService.findUserProfile(mediator.profileId)).thenReturn(Optional.of(mediatorProfile));

        Optional<UserProfile> result = service.selectMediator(makerProfileId, takerProfileId, OFFER_ID);
        assertTrue(result.isPresent());
        assertEquals(mediatorProfile, result.get());
    }

    private record RoleWithId(AuthorizedBondedRole role, String profileId) {}

    private static RoleWithId createRoleWithId() {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + System.nanoTime());
        String profileId = pubKey.getId();
        AuthorizedBondedRole role = buildRole(profileId, kp, pubKey, BondedRoleType.MEDIATOR);
        return new RoleWithId(role, profileId);
    }

    private static AuthorizedBondedRole createRole(String profileId) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + profileId);
        return buildRole(profileId, kp, pubKey, BondedRoleType.MEDIATOR);
    }

    private static AuthorizedBondedRole createRoleWithType(String profileId, BondedRoleType type) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + profileId);
        return buildRole(profileId, kp, pubKey, type);
    }

    private static AuthorizedBondedRole buildRole(String profileId, KeyPair kp, PubKey pubKey, BondedRoleType type) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(9000))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        return new AuthorizedBondedRole(
                profileId,
                Hex.encode(kp.getPublic().getEncoded()),
                type,
                profileId.substring(0, 10) + "-bond",
                "dummySig+base64sig00000000000000000000000000000000000000000000000=",
                Optional.of(addresses),
                networkId,
                Optional.empty(),
                false
        );
    }

    private static UserProfile createUserProfile(int port) {
        KeyPair kp = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(kp.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static String padId(String prefix) {
        return (prefix + "0".repeat(40)).substring(0, 40);
    }
}

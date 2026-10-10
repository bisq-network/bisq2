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

package bisq.user.identity;

import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The store skips a persisted user identity which it cannot parse. If that identity was the selected one, the store
 * selects another one. A caller can pass an older copy of a user identity to select, and the store then selects its own
 * user identity with the same ID. Users share their log files, so the log must not contain the private data of the
 * profiles.
 */
@Isolated("Captures the log of UserIdentityStore")
class UserIdentityStoreLoggingTest {
    private static final String PRIVATE_VALUE = "private-sentinel";

    private final Logger logger = (Logger) LoggerFactory.getLogger(UserIdentityStore.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void skippedSelectedIdentityIsReplacedWithoutLoggingPrivateData() throws UnresolvableProtobufException {
        UserIdentity remaining = createUserIdentity(20_001);
        UserIdentity selected = createUserIdentity(20_002);
        // The nickname is longer than the limit, so the store skips this identity when it loads
        bisq.user.protobuf.UserIdentity invalidSelected = selected.toProto(false).toBuilder()
                .setUserProfile(selected.getUserProfile().toProto(false).toBuilder()
                        .setNickName("x".repeat(UserProfile.MAX_LENGTH_NICK_NAME + 1)))
                .build();
        bisq.user.protobuf.UserIdentityStore proto = bisq.user.protobuf.UserIdentityStore.newBuilder()
                .addUserIdentities(remaining.toProto(false))
                .addUserIdentities(invalidSelected)
                .setSelectedUserIdentityId(selected.getId())
                .build();

        UserIdentityStore store = UserIdentityStore.fromProto(proto);

        assertEquals(remaining.getId(), store.toProto(false).getSelectedUserIdentityId());
        assertTrue(appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("Could not find the selected user identity")));
        appender.list.forEach(event -> {
            String message = event.getFormattedMessage();
            assertFalse(message.contains(PRIVATE_VALUE), message);
            assertFalse(message.contains(remaining.getId()), message);
        });
    }

    // A channel keeps the copy of its user identity from before a profile edit. The copy has the same ID, but
    // UserIdentity equality includes the terms and the statement of the profile (rule 2 of
    // docs/specifications/user/identity-by-key.md).
    @Test
    void staleCopyOfAUserIdentitySelectsTheStoredUserIdentity() throws UnresolvableProtobufException {
        UserIdentity selected = createUserIdentity(20_003);
        UserIdentity other = createUserIdentity(20_004);
        UserIdentityStore store = createStore(selected, other);
        UserIdentity staleCopy = new UserIdentity(other.getIdentity(),
                UserProfile.forEdit(other.getUserProfile(), PRIVATE_VALUE + " old terms", PRIVATE_VALUE + " old statement"));

        store.setSelectedUserIdentity(staleCopy);

        UserIdentity storedOther = store.getUserIdentities().stream()
                .filter(userIdentity -> userIdentity.getId().equals(other.getId()))
                .findAny()
                .orElseThrow();
        assertSame(storedOther, store.getSelectedUserIdentity());
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void unknownUserIdentityIsIgnoredWithoutLoggingPrivateData() throws UnresolvableProtobufException {
        UserIdentity selected = createUserIdentity(20_006);
        UserIdentity other = createUserIdentity(20_007);
        UserIdentityStore store = createStore(selected, other);
        UserIdentity unknown = createUserIdentity(20_008);

        store.setSelectedUserIdentity(unknown);

        assertEquals(selected.getId(), store.toProto(false).getSelectedUserIdentityId());
        assertTrue(appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("Could not find the given user identity")));
        assertNoPrivateDataLogged(selected, other, unknown);
    }

    @Test
    void selectionInAnEmptyStoreLogsNoPrivateData() {
        UserIdentity userIdentity = createUserIdentity(20_005);

        new UserIdentityStore().setSelectedUserIdentity(userIdentity);

        assertTrue(appender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("userIdentities must not be empty")));
        assertNoPrivateDataLogged(userIdentity);
    }

    private static UserIdentityStore createStore(UserIdentity selected,
                                                 UserIdentity other) throws UnresolvableProtobufException {
        return UserIdentityStore.fromProto(bisq.user.protobuf.UserIdentityStore.newBuilder()
                .addUserIdentities(selected.toProto(false))
                .addUserIdentities(other.toProto(false))
                .setSelectedUserIdentityId(selected.getId())
                .build());
    }

    private void assertNoPrivateDataLogged(UserIdentity... userIdentities) {
        appender.list.forEach(event -> {
            String message = event.getFormattedMessage();
            assertFalse(message.contains(PRIVATE_VALUE), message);
            for (UserIdentity userIdentity : userIdentities) {
                assertFalse(message.contains(userIdentity.getId()), message);
            }
        });
    }

    private static UserIdentity createUserIdentity(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        UserProfile userProfile = new UserProfile(1,
                PRIVATE_VALUE + " nickname " + port,
                proofOfWork,
                0,
                networkId,
                PRIVATE_VALUE + " terms",
                PRIVATE_VALUE + " statement",
                "1.0.0");
        KeyBundle keyBundle = new KeyBundle("key-" + port,
                keyPair,
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new UserIdentity(new Identity("tag-" + port, networkId, keyBundle), userProfile);
    }
}

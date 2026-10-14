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

package bisq.user.profile;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.contact_list.ContactListService;
import bisq.user.identity.UserIdentityService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The payload of the proof of work of a user profile must be the hash of the profile key. Nodes reject a profile with
 * another payload when they parse it, so they do not store, relay, persist or log it. ProofOfWork alone accepts a
 * payload of up to 10 000 000 bytes. See docs/specifications/user/user-profile-proof-of-work.md.
 */
@Isolated("Captures the log of UserProfileService")
class UserProfilePayloadTest {
    private static final HashCashProofOfWorkService PROOF_OF_WORK_SERVICE = new HashCashProofOfWorkService();
    private static final String NICK_NAME = "nick-sentinel";

    private final Logger logger = (Logger) LoggerFactory.getLogger(UserProfileService.class);
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
    void profileWithTheKeyHashAsPayloadIsParsed() throws UnresolvableProtobufException {
        NetworkId networkId = createNetworkId();
        UserProfile userProfile = createUserProfile(networkId, proofOfWork(networkId.getPubKeyHash()));

        assertEquals(userProfile, UserProfile.fromProto(userProfile.toProto(false)));
    }

    @Test
    void profileWithAnotherPayloadIsRejectedAtCreation() {
        NetworkId networkId = createNetworkId();
        ProofOfWork proofOfWork = proofOfWork(new byte[20]);

        assertThrows(IllegalArgumentException.class, () -> createUserProfile(networkId, proofOfWork));
    }

    @Test
    void profileWithLargePayloadIsRejectedAtParsing() {
        bisq.user.protobuf.UserProfile proto = withPayload(new byte[1_000_000]);

        assertThrows(UnresolvableProtobufException.class, () -> UserProfile.fromProto(proto));
    }

    // The rule compares the whole payload, so the key hash with appended bytes is another payload
    @Test
    void profileWithKeyHashAndAppendedBytesIsRejectedAtParsing() {
        NetworkId networkId = createNetworkId();
        byte[] keyHashWithAppendedBytes = Arrays.copyOf(networkId.getPubKeyHash(), 1000);
        bisq.user.protobuf.UserProfile proto = createUserProfile(networkId, proofOfWork(networkId.getPubKeyHash()))
                .toProto(false).toBuilder()
                .setProofOfWork(proofOfWork(keyHashWithAppendedBytes).toProto(false))
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> UserProfile.fromProto(proto));
    }

    // Users share their log files, so the log names only the ID of a profile which the service ignores
    @Test
    void profileWithInvalidWorkIsLoggedOnlyWithItsId() {
        UserProfileService service = createService();
        NetworkId networkId = createNetworkId();
        UserProfile userProfile = createUserProfile(networkId, invalidWork(networkId.getPubKeyHash()));

        service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(userProfile));

        assertFalse(service.findUserProfile(userProfile.getId()).isPresent());
        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains(userProfile.getId()))
                .toList();
        assertTrue(messages.stream().anyMatch(message -> message.contains("Proof of work verification")), messages.toString());
        messages.forEach(message -> assertFalse(message.contains(NICK_NAME), message));
    }

    private static bisq.user.protobuf.UserProfile withPayload(byte[] payload) {
        NetworkId networkId = createNetworkId();
        return createUserProfile(networkId, proofOfWork(networkId.getPubKeyHash()))
                .toProto(false).toBuilder()
                .setProofOfWork(proofOfWork(networkId.getPubKeyHash()).toProto(false).toBuilder()
                        .setPayload(ByteString.copyFrom(payload)))
                .build();
    }

    // Work which claims the minimum difficulty but does not reach it. Almost every solution fails; the loop makes the
    // test independent of the random key.
    private static ProofOfWork invalidWork(byte[] payload) {
        for (int i = 0; ; i++) {
            ProofOfWork candidate = new ProofOfWork(payload, i, null, UserIdentityService.MINT_NYM_DIFFICULTY,
                    new byte[]{(byte) i, (byte) (i >> 8)}, 0);
            if (!PROOF_OF_WORK_SERVICE.verify(candidate)) {
                return candidate;
            }
        }
    }

    private static ProofOfWork proofOfWork(byte[] payload) {
        return new ProofOfWork(payload, 0, null, 1, new byte[72], 0);
    }

    private static NetworkId createNetworkId() {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key"));
    }

    private static UserProfile createUserProfile(NetworkId networkId, ProofOfWork proofOfWork) {
        return new UserProfile(1, NICK_NAME, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static UserProfileService createService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<UserProfileStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(UserProfileStore.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.SETTINGS), any(UserProfileStore.class)))
                .thenReturn(persistence);
        SecurityService securityService = mock(SecurityService.class);
        when(securityService.getHashCashProofOfWorkService()).thenReturn(PROOF_OF_WORK_SERVICE);
        return new UserProfileService(persistenceService,
                securityService,
                mock(NetworkService.class),
                mock(ContactListService.class));
    }
}

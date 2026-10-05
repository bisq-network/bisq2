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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.TimeUnit;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The storage layer takes the MetaData of mailbox data and mailbox remove requests from the StorableTypesRegistry. The
 * MetaData in the message comes from the sender or a relay, and the signature does not cover it. Its values must
 * not change the expiry, the priority or the cost factor.
 */
class MailboxMetaDataTest {
    private static final String CLASS_NAME = AckMessage.class.getSimpleName();
    // Values a sender or relay could choose: long time to live, highest priority, no map size cost
    private static final MetaData FORGED_META_DATA = new MetaData(MetaData.TTL_100_DAYS,
            MetaData.HIGHEST_PRIORITY,
            CLASS_NAME,
            0);
    // AckMessage has a time to live of 5 days
    private static final long SIX_DAYS = TimeUnit.DAYS.toMillis(6);

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    // A MailboxMessage type which declares a longer time to live than MailboxData.MAX_TTL. The storage layer never
    // instantiates the type, so an interface is sufficient.
    private interface LongLivedMailboxMessage extends MailboxMessage {
        MetaData META_DATA = new MetaData(TimeUnit.DAYS.toMillis(30), LongLivedMailboxMessage.class.getSimpleName());
    }

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        StorableTypesRegistry.register(LongLivedMailboxMessage.class, LongLivedMailboxMessage.META_DATA);
    }

    @Test
    void addMailboxRequestUsesRegisteredMetaData() throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest request = withForgedMetaData(createAddMailboxRequest());

        // The signature does not cover metaData, so a relay can change it
        assertFalse(request.isSignatureInvalid());
        assertSame(AckMessage.META_DATA, request.getMetaData());
        assertEquals(AckMessage.META_DATA.getPriority(), request.getPriority());
        assertEquals(MessageCostPolicy.getCostFactor(createAddMailboxRequest()), MessageCostPolicy.getCostFactor(request));
    }

    @Test
    void addMailboxRequestExpiresAfterTtlOfRegisteredMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest.Builder builder = withForgedMetaData(createAddMailboxRequest())
                .toValueProto(false).toBuilder();
        builder.getMailboxSequentialDataBuilder().setCreated(System.currentTimeMillis() - SIX_DAYS);

        assertTrue(AddMailboxRequest.fromProto(builder.build()).isExpired());
    }

    @Test
    void removeMailboxRequestUsesRegisteredMetaData() throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveMailboxRequest request = createRemoveMailboxRequest();
        //noinspection deprecation
        bisq.network.protobuf.RemoveMailboxRequest.Builder forgedBuilder = request.toValueProto(false).toBuilder()
                .setMetaData(FORGED_META_DATA.toProto(false))
                .setCreated(System.currentTimeMillis() - SIX_DAYS);
        // Signature V2 covers created. Signature V1 covers neither metaData nor created, and before the cutoff date of
        // signature V1 a relay can remove signature V2.
        assertTrue(RemoveMailboxRequest.fromProto(forgedBuilder.build()).isSignatureInvalid());
        RemoveMailboxRequest forgedRequest = RemoveMailboxRequest.fromProto(forgedBuilder.clearSignatureV2().build());

        assertFalse(forgedRequest.isSignatureInvalid(BEFORE_CUTOFF));
        assertSame(AckMessage.META_DATA, forgedRequest.getMetaData());
        assertEquals(MessageCostPolicy.getCostFactor(request), MessageCostPolicy.getCostFactor(forgedRequest));
        assertTrue(forgedRequest.isExpired());
    }

    @Test
    void timeToLiveOfMailboxRequestsIsLimitedByMaxTtl() throws GeneralSecurityException, UnresolvableProtobufException {
        long twentyDays = TimeUnit.DAYS.toMillis(20);
        MailboxData mailboxData = new MailboxData(LongLivedMailboxMessage.META_DATA,
                createConfidentialMessage(senderKeyPair));
        bisq.network.protobuf.AddMailboxRequest.Builder builder = AddMailboxRequest.from(mailboxData,
                senderKeyPair,
                receiverKeyPair.getPublic()).toValueProto(false).toBuilder();
        builder.getMailboxSequentialDataBuilder().setCreated(System.currentTimeMillis() - twentyDays);
        AddMailboxRequest addRequest = AddMailboxRequest.fromProto(builder.build());
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.fromProto(
                RemoveMailboxRequest.from(mailboxData, receiverKeyPair).toValueProto(false).toBuilder()
                        .setCreated(System.currentTimeMillis() - twentyDays)
                        .build());

        assertEquals(MailboxData.MAX_TTL, addRequest.getTtl());
        assertTrue(addRequest.isExpired());
        assertEquals(MailboxData.MAX_TTL, removeRequest.getTtl());
        assertTrue(removeRequest.isExpired());
    }

    @Test
    void typeWhichIsNotRegisteredHasNoMetaDataButACostFactor()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest request = createAddMailboxRequest();
        bisq.network.protobuf.AddMailboxRequest.Builder builder = request.toValueProto(false).toBuilder();
        // A node of a later version fills className and the className of metaData with the same name
        //noinspection deprecation
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder()
                .setClassName("NotRegisteredMailboxMessage")
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "NotRegisteredMailboxMessage").toProto(false));
        AddMailboxRequest requestOfUnknownType = AddMailboxRequest.fromProto(builder.build());

        assertEquals("NotRegisteredMailboxMessage", requestOfUnknownType.getClassName());
        assertThrows(IllegalStateException.class, requestOfUnknownType::getMetaData);
        // The proof of work check must not fail for types added in later versions. Such data is not stored.
        double confidentialMessageImpact = MessageCostPolicy.getCostFactor(request.getMailboxSequentialData()
                .getMailboxData().getConfidentialMessage());
        assertEquals(Math.max(0.25, confidentialMessageImpact), MessageCostPolicy.getCostFactor(requestOfUnknownType));
    }

    // Nodes of earlier versions compute expiry, priority and cost factor from the field metaData. A relay sends them the
    // registered MetaData. The hash does not cover the field, so the signature and the storage key stay the same.
    @Test
    void relayedMailboxRequestsCarryTheRegisteredMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest addRequest = createAddMailboxRequest();
        AddMailboxRequest forgedAddRequest = withForgedMetaData(addRequest);
        //noinspection deprecation
        assertEquals(AckMessage.META_DATA.toProto(false),
                forgedAddRequest.toValueProto(false).getMailboxSequentialData().getMailboxData().getMetaData());
        assertArrayEquals(addRequest.serializeForHash(), forgedAddRequest.serializeForHash());

        RemoveMailboxRequest removeRequest = createRemoveMailboxRequest();
        //noinspection deprecation
        RemoveMailboxRequest forgedRemoveRequest = RemoveMailboxRequest.fromProto(removeRequest.toValueProto(false)
                .toBuilder()
                .setMetaData(FORGED_META_DATA.toProto(false))
                .build());
        //noinspection deprecation
        assertEquals(AckMessage.META_DATA.toProto(false), forgedRemoveRequest.toValueProto(false).getMetaData());
        assertArrayEquals(removeRequest.serializeForHash(), forgedRemoveRequest.serializeForHash());
    }

    @Test
    void mailboxDataKeepsTheReceivedMetaDataIfTheTypeIsNotRegistered() throws UnresolvableProtobufException {
        MetaData metaDataOfUnknownType = new MetaData(MetaData.TTL_10_DAYS, "NotRegisteredMailboxMessage");
        MailboxData mailboxData = MailboxData.fromProto(
                new MailboxData(metaDataOfUnknownType, createConfidentialMessage(senderKeyPair)).toProto(false));

        //noinspection deprecation
        assertEquals(metaDataOfUnknownType.toProto(false), mailboxData.toProto(false).getMetaData());
    }

    // No version keeps the received metaData, and no hash contains it
    @Test
    void mailboxDataOfEveryVersionCarriesTheRegisteredMetaData() throws UnresolvableProtobufException {
        MailboxData registered = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        for (int version = 1; version <= 3; version++) {
            bisq.network.protobuf.MailboxData forgedProto = new MailboxData(FORGED_META_DATA,
                    registered.getConfidentialMessage()).toProto(false).toBuilder()
                    .setVersion(version)
                    .build();

            MailboxData mailboxData = MailboxData.fromProto(forgedProto);

            //noinspection deprecation
            assertEquals(AckMessage.META_DATA.toProto(false), mailboxData.toProto(false).getMetaData());
            assertArrayEquals(registered.serializeForHash(), mailboxData.serializeForHash());
        }
    }

    // Version 0 is the format of releases before the version field existed. Since v2.1.0, nodes send version 1.
    @Test
    void mailboxDataAndRemoveRequestOfVersion0AreRejected()
            throws GeneralSecurityException, UnresolvableProtobufException {
        bisq.network.protobuf.MailboxData mailboxDataProto = new MailboxData(AckMessage.META_DATA,
                createConfidentialMessage(senderKeyPair)).toProto(false).toBuilder()
                .setVersion(0)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> MailboxData.fromProto(mailboxDataProto));

        bisq.network.protobuf.RemoveMailboxRequest removeProto = createRemoveMailboxRequest().toValueProto(false)
                .toBuilder()
                .setVersion(0)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> RemoveMailboxRequest.fromProto(removeProto));
        assertThrows(UnresolvableProtobufException.class,
                () -> RemoveMailboxRequest.fromProto(removeProto.toBuilder().clearSignatureV2().build()));
    }

    // A remove request of nodes up to 2.1.13 has version 1 and no signature V2. The receiver uses the registered
    // MetaData and the policy value.
    @Test
    void removeRequestOfAnEarlierVersionUsesTheRegisteredMetaData()
            throws GeneralSecurityException, UnresolvableProtobufException {
        //noinspection deprecation
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.fromProto(createRemoveMailboxRequest()
                .toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .setMetaData(FORGED_META_DATA.toProto(false))
                .build());

        //noinspection deprecation
        assertEquals(AckMessage.META_DATA.toProto(false), removeRequest.toValueProto(false).getMetaData());
        assertEquals(0.1, MessageCostPolicy.getCostFactor(removeRequest));
    }

    private AddMailboxRequest createAddMailboxRequest() throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        return AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
    }

    private RemoveMailboxRequest createRemoveMailboxRequest()
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        return RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
    }

    private static AddMailboxRequest withForgedMetaData(AddMailboxRequest request)
            throws UnresolvableProtobufException {
        bisq.network.protobuf.AddMailboxRequest.Builder builder = request.toValueProto(false).toBuilder();
        //noinspection deprecation
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setMetaData(FORGED_META_DATA.toProto(false));
        return AddMailboxRequest.fromProto(builder.build());
    }
}

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

package bisq.chat.priv;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the de-duplication guard for the "peer is banned" system notice (#4769): a peer already
 * named in the trailing run of ban notices is not notified again.
 */
public class PrivateChatChannelServiceTest {
    private static final String NOTICE_BOB = Res.encode(PrivateChatChannelService.PEER_BANNED_NOTICE_KEY, "Bob");
    private static final String NOTICE_ALICE = Res.encode(PrivateChatChannelService.PEER_BANNED_NOTICE_KEY, "Alice");
    private static final String LEAVE = Res.encode("chat.privateChannel.message.leave", "Bob");

    @Test
    void emptyChannelIsNotYetNotified() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(List.of(), "Bob"));
    }

    @Test
    void noticeForThisPeerInTrailingRunMeansAlreadyNotified() {
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void aNoticeForAnotherPeerDoesNotSuppressThisPeer() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_ALICE, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void bothPeersNamedInTheRunAreEachAlreadyNotified() {
        var messages = List.of(
                message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE),
                message(2, NOTICE_ALICE, ChatMessageType.PROTOCOL_LOG_MESSAGE));
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(messages, "Bob"));
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(messages, "Alice"));
    }

    @Test
    void aRealMessageAfterTheNoticeResetsTheRun() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(List.of(
                message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE),
                message(2, "hi", ChatMessageType.TEXT)), "Bob"));
    }

    @Test
    void aDifferentSystemMessageIsNotABanNotice() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, LEAVE, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void aNormalTextMessageWithTheNoticeTextIsNotABanNotice() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_BOB, ChatMessageType.TEXT)), "Bob"));
    }

    private static ChatMessage message(long date, String text, ChatMessageType type) {
        return new TestChatMessage(date, text, type);
    }

    private static class TestChatMessage extends ChatMessage {
        private TestChatMessage(long date, String text, ChatMessageType type) {
            super("id", ChatChannelDomain.DISCUSSION, "channelId", "author",
                    Optional.ofNullable(text), Optional.<Citation>empty(), date, false, type);
        }

        @Override
        protected MetaData getMetaData() {
            return null;
        }

        @Override
        public <R extends ChatMessageReaction> ObservableSet<R> getChatMessageReactions() {
            return null;
        }

        @Override
        public boolean addChatMessageReaction(ChatMessageReaction reaction) {
            return false;
        }

        @Override
        public Message.Builder getBuilder(boolean serializeForHash) {
            return null;
        }
    }
}

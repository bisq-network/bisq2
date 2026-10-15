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


package bisq.api.chat.common;

import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.SubDomain;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static bisq.api.chat.common.PublicChatTestMocks.DISCUSSION_ID;
import static bisq.api.chat.common.PublicChatTestMocks.SUPPORT_ID;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannelService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A node upgraded from before v2.1.1 has one channel per domain like any other, but the one it kept
 * may be a channel that was consolidated away: the store deserializes into a set, and a channel's
 * {@code equals} is its migrated id, so only one of the old ones survives and which one is the order
 * they were written in. It answers {@code discussion.bisq} either way, so the API has to serve it.
 * <p>
 * Built from real channels rather than mocks on purpose. The property these tests exist to guard is
 * that {@link CommonPublicChatChannel#getId()} answers the migrated id — stubbing the getter would
 * assert the stub, and would stay green if that override were dropped.
 */
class PublicChatChannelsTest {
    @Test
    void aDeprecatedSurvivorIsStillServedUnderItsMigratedId() {
        CommonPublicChatChannel survivor = channel(SubDomain.DISCUSSION_BITCOIN);

        assertThat(survivor.getId()).isEqualTo(DISCUSSION_ID);
        assertThat(channels(survivor).findChannel(DISCUSSION_ID)).contains(survivor);
    }

    @Test
    void aDeprecatedSurvivorIsResolvedToTheServiceOfItsDomain() {
        CommonPublicChatChannel survivor = channel(SubDomain.DISCUSSION_BITCOIN);
        CommonPublicChatChannelService service = mockChannelService(survivor);
        PublicChatChannels channels = new PublicChatChannels(Map.of(
                ChatChannelDomain.DISCUSSION, service,
                ChatChannelDomain.SUPPORT, mockChannelService(channel(SubDomain.SUPPORT_SUPPORT))));

        assertThat(channels.serviceOf(survivor)).isSameAs(service);
    }

    @Test
    void bothDomainsAreServed() {
        CommonPublicChatChannel discussion = channel(SubDomain.DISCUSSION_BISQ);
        CommonPublicChatChannel support = channel(SubDomain.SUPPORT_SUPPORT);
        PublicChatChannels channels = channels(discussion, support);

        assertThat(channels.getChannels()).containsExactlyInAnyOrder(discussion, support);
        assertThat(channels.findChannel(SUPPORT_ID)).contains(support);
    }

    @Test
    void anIdOfNoChannelResolvesToNothing() {
        assertThat(channels().findChannel("no.such.channel")).isEmpty();
    }

    /** {@code ChatService.shutdown} empties the map this is built from; the copy keeps the API serving. */
    @Test
    void theChannelsSurviveTheSourceMapBeingEmptied() {
        Map<ChatChannelDomain, CommonPublicChatChannelService> source = new HashMap<>(Map.of(
                ChatChannelDomain.DISCUSSION, mockChannelService(channel(SubDomain.DISCUSSION_BISQ)),
                ChatChannelDomain.SUPPORT, mockChannelService(channel(SubDomain.SUPPORT_SUPPORT))));
        PublicChatChannels channels = new PublicChatChannels(source);

        source.clear();

        assertThat(channels.findChannel(DISCUSSION_ID)).isPresent();
        assertThat(channels.findChannel(SUPPORT_ID)).isPresent();
    }

    /**
     * Unreachable today — {@code ChatService} registers exactly DISCUSSION and SUPPORT, and every
     * channel's domain migrates into one of those two. Asserted anyway so the invariant the javadoc
     * states is executable: without it {@code serviceOf} hands back the {@code EnumMap}'s bare null and
     * the endpoints turn it into a 500 with nothing in it to explain why.
     */
    @Test
    void aChannelOfNoRegisteredDomainFailsLoudly() {
        PublicChatChannels channels = new PublicChatChannels(Map.of(
                ChatChannelDomain.DISCUSSION, mockChannelService(channel(SubDomain.DISCUSSION_BISQ))));

        assertThatThrownBy(() -> channels.serviceOf(channel(SubDomain.SUPPORT_SUPPORT)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SUPPORT");
    }

    /** The two channels the API serves, each behind the service its domain owns. */
    private static PublicChatChannels channels() {
        return channels(channel(SubDomain.DISCUSSION_BISQ), channel(SubDomain.SUPPORT_SUPPORT));
    }

    private static PublicChatChannels channels(CommonPublicChatChannel discussionChannel) {
        return channels(discussionChannel, channel(SubDomain.SUPPORT_SUPPORT));
    }

    private static PublicChatChannels channels(CommonPublicChatChannel discussionChannel,
                                               CommonPublicChatChannel supportChannel) {
        return new PublicChatChannels(Map.of(
                ChatChannelDomain.DISCUSSION, mockChannelService(discussionChannel),
                ChatChannelDomain.SUPPORT, mockChannelService(supportChannel)));
    }

    private static CommonPublicChatChannel channel(SubDomain subDomain) {
        return new CommonPublicChatChannel(subDomain.getChatChannelDomain(), subDomain);
    }
}

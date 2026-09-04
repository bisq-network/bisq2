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

import bisq.chat.Citation;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bisq.api.chat.common.PublicChatTestMocks.mockMessage;
import static bisq.api.chat.common.PublicChatTestMocks.mockReaction;
import static bisq.api.chat.common.PublicChatTestMocks.knownProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicChatDtoFactoryTest {
    private static final String AUTHOR = "author";
    private static final String OTHER = "other";

    private UserProfileService userProfileService;
    private BannedUserService bannedUserService;
    private PublicChatDtoFactory factory;

    @BeforeEach
    void setUp() {
        userProfileService = mock(UserProfileService.class);
        bannedUserService = mock(BannedUserService.class);
        knownProfile(userProfileService, AUTHOR);
        knownProfile(userProfileService, OTHER);
        factory = new PublicChatDtoFactory(userProfileService, bannedUserService);
    }

    @Test
    void aMessageFromAResolvableUnbannedAuthorIsVisible() {
        assertThat(factory.isVisible(mockMessage("m", AUTHOR, 1))).isTrue();
    }

    @Test
    void aMessageFromABannedAuthorIsHidden() {
        when(bannedUserService.isUserProfileBanned(AUTHOR)).thenReturn(true);

        assertThat(factory.isVisible(mockMessage("m", AUTHOR, 1))).isFalse();
    }

    @Test
    void aMessageWhoseAuthorCannotBeResolvedIsHidden() {
        when(userProfileService.findUserProfile(anyString())).thenReturn(Optional.empty());

        assertThat(factory.isVisible(mockMessage("m", AUTHOR, 1))).isFalse();
    }

    @Test
    void anExpiredMessageIsHidden() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        when(message.isExpired()).thenReturn(true);

        assertThat(factory.isVisible(message)).isFalse();
    }

    /** Ignoring is the client's business: un-ignoring has to bring the messages back without the node. */
    @Test
    void aMessageFromAnIgnoredAuthorStaysVisible() {
        when(userProfileService.isChatUserIgnored(AUTHOR)).thenReturn(true);

        assertThat(factory.isVisible(mockMessage("m", AUTHOR, 1))).isTrue();
    }

    @Test
    void visibleMessagesComeNewestFirstWithTheIdAsTiebreak() {
        CommonPublicChatMessage oldest = mockMessage("z", AUTHOR, 100);
        CommonPublicChatMessage tieA = mockMessage("a", AUTHOR, 200);
        CommonPublicChatMessage tieB = mockMessage("b", AUTHOR, 200);
        CommonPublicChatMessage hidden = mockMessage("hidden", "unknown-author", 300);

        assertThat(factory.visibleMessagesNewestFirst(List.of(oldest, tieA, hidden, tieB)))
                .containsExactly(tieB, tieA, oldest);
    }

    @Test
    void aReactionIsHiddenWhenItsOwnSenderOrTheMessageAuthorIsBanned() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        CommonPublicChatMessageReaction reaction = mockReaction("r", OTHER, "m", 0);
        assertThat(factory.isVisible(message, reaction)).isTrue();

        when(bannedUserService.isUserProfileBanned(OTHER)).thenReturn(true);
        assertThat(factory.isVisible(message, reaction)).isFalse();

        when(bannedUserService.isUserProfileBanned(OTHER)).thenReturn(false);
        when(bannedUserService.isUserProfileBanned(AUTHOR)).thenReturn(true);
        assertThat(factory.isVisible(message, reaction)).isFalse();
    }

    /** The two topics must never disagree, so the embedded reactions use the reactions topic's filter. */
    @Test
    void theEmbeddedReactionsAreFilteredLikeTheReactionsTopic() {
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>(Set.of(
                mockReaction("reaction-visible", OTHER, "m", 0),
                mockReaction("reaction-banned", "banned", "m", 0),
                mockReaction("reaction-unresolved", "unknown", "m", 0)));
        knownProfile(userProfileService, "banned");
        when(bannedUserService.isUserProfileBanned("banned")).thenReturn(true);
        when(userProfileService.findUserProfile("unknown")).thenReturn(Optional.empty());

        var dto = factory.toDto(mockMessage("m", AUTHOR, 1, reactions));

        assertThat(dto.chatMessageReactions()).extracting(r -> r.id()).containsExactly("reaction-visible");
    }

    @Test
    void theCitationAuthorIsResolvedWhenPossible() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        Citation citation = citationBy(OTHER);
        when(message.getCitation()).thenReturn(Optional.of(citation));

        var dto = factory.toDto(message);

        assertThat(dto.citationAuthorUserProfile().orElseThrow().nickName()).isEqualTo("nick-" + OTHER);
    }

    /**
     * The two authors are resolved on different terms, on purpose: the message's own author with
     * {@code resolve}, which throws, because there is no message to show without it; the citation's with
     * {@code findUserProfile}, which does not, because a quote whose author was pruned is still a quote.
     * Resolving both the strict way would cost the whole message for a name.
     */
    @Test
    void aMessageWhoseCitationAuthorCannotBeResolvedIsStillMapped() {
        CommonPublicChatMessage message = mockMessage("m", AUTHOR, 1);
        Citation citation = citationBy("pruned");
        when(message.getCitation()).thenReturn(Optional.of(citation));
        when(userProfileService.findUserProfile("pruned")).thenReturn(Optional.empty());

        var dto = factory.toDto(message);

        assertThat(dto.messageId()).isEqualTo("m");
        assertThat(dto.citation().orElseThrow().text()).isEqualTo("quoted");
        assertThat(dto.citationAuthorUserProfile()).isEmpty();
    }

    /** Built into a local by both callers before it is stubbed onto the message, see {@code knownProfile}. */
    private static Citation citationBy(String authorUserProfileId) {
        Citation citation = mock(Citation.class);
        when(citation.getAuthorUserProfileId()).thenReturn(authorUserProfileId);
        when(citation.getText()).thenReturn("quoted");
        when(citation.getChatMessageId()).thenReturn(Optional.empty());
        return citation;
    }
}

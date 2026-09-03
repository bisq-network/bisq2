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

import bisq.api.dto.DtoMappings;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.dto.chat.common.CommonPublicChatMessageReactionDto;
import bisq.api.dto.mappings.chat.common.CommonPublicChatMessageDtoMapping;
import bisq.api.dto.mappings.chat.common.CommonPublicChatMessageReactionDtoMapping;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What the API shows of the public chat channels, and how. Shared by the REST endpoint and the
 * websocket topics so that the history, the subscribe snapshot, the live stream and the reactions
 * topic can never disagree about which messages and reactions exist.
 * <p>
 * Hidden: messages whose author cannot be resolved (there is no profile to show), messages from banned
 * authors, and expired messages. Bisq 2 rejects banned authors on the inbound path and prunes expired
 * data every few minutes, so both filters only cover the window in between — the same one desktop
 * re-checks in its list view. Ignored users are kept on purpose: the client hides them against its own
 * ignore list, so un-ignoring brings the messages back without the node having to re-send anything.
 */
@Slf4j
public class PublicChatDtoFactory {
    private static final Comparator<CommonPublicChatMessage> NEWEST_FIRST =
            Comparator.comparingLong(CommonPublicChatMessage::getDate).reversed()
                    .thenComparing(CommonPublicChatMessage::getId, Comparator.reverseOrder());

    private final UserProfileService userProfileService;
    private final BannedUserService bannedUserService;

    public PublicChatDtoFactory(UserProfileService userProfileService, BannedUserService bannedUserService) {
        this.userProfileService = userProfileService;
        this.bannedUserService = bannedUserService;
    }

    /**
     * Every visible message, newest first. The history is delivered whole, not paged: the mobile client
     * searches messages locally, so a partial load would make its search lie about older messages, and
     * the P2P store's 10-day TTL keeps the set bounded. If the channel's traffic ever outgrows this,
     * the compatible extension is a {@code since} timestamp delta, not an offset.
     */
    public List<CommonPublicChatMessage> visibleMessagesNewestFirst(Collection<CommonPublicChatMessage> messages) {
        return messages.stream()
                .filter(this::isVisible)
                .sorted(NEWEST_FIRST)
                .toList();
    }

    /** {@link #visibleMessagesNewestFirst} mapped for delivery, skipping what {@link #findDto} could not map. */
    public List<CommonPublicChatMessageDto> visibleMessageDtosNewestFirst(Collection<CommonPublicChatMessage> messages) {
        return visibleMessagesNewestFirst(messages).stream()
                .map(this::findDto)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Stricter than desktop, which filters on the author — banned or ignored — and leaves the TTL to
     * pruning. The expiry check covers the window before the message is pruned, which is up to ten
     * minutes: {@code PruneExpiredEntriesService} sweeps the network store on that period, and the
     * channel's own {@code ChatChannelService#removeExpiredMessages} runs on channel selection, so on a
     * headless node it never runs at all. Without the check a client that keeps the whole history
     * locally would be handed messages that vanish from its next snapshot.
     * <p>
     * Ignored authors are not filtered at either level — see {@code PublicChatRestApi#getMessages},
     * which says the client filters those against {@code GET /user-profiles/ignored}.
     */
    public boolean isVisible(CommonPublicChatMessage message) {
        return !message.isExpired() && isVisibleAuthor(message.getAuthorUserProfileId());
    }

    /**
     * Checked at both levels: the message's author decides whether the reaction has a message to sit on
     * at all, the reaction's own sender whether it is shown on one that is visible.
     */
    public boolean isVisible(CommonPublicChatMessage message, CommonPublicChatMessageReaction reaction) {
        return isVisible(message) && isVisibleAuthor(reaction.getUserProfileId());
    }

    /**
     * Whether the one thing keeping {@link #isVisible(CommonPublicChatMessage)} false is that the
     * author's profile has not arrived yet. That is the state worth waiting out: on a fresh node the
     * P2P store routinely delivers a channel's messages before the profiles of their authors, and a
     * profile that is merely late will land. A banned author or an expired message will not come back,
     * so neither is worth parking for.
     */
    public boolean awaitsAuthorProfile(CommonPublicChatMessage message) {
        return !message.isExpired()
                && !bannedUserService.isUserProfileBanned(message.getAuthorUserProfileId())
                && userProfileService.findUserProfile(message.getAuthorUserProfileId()).isEmpty();
    }

    /**
     * The reaction counterpart of {@link #awaitsAuthorProfile}: the message itself is visible and only
     * the reaction sender's profile is missing. A reaction skipped because the <em>message author</em>
     * is missing needs no parking here — when that author arrives, the messages topic replays the
     * message and {@link #toDto(CommonPublicChatMessage)} embeds every reaction that is visible by then.
     */
    public boolean awaitsSenderProfile(CommonPublicChatMessage message, CommonPublicChatMessageReaction reaction) {
        return isVisible(message)
                && !bannedUserService.isUserProfileBanned(reaction.getUserProfileId())
                && userProfileService.findUserProfile(reaction.getUserProfileId()).isEmpty();
    }

    /**
     * The embedded reactions are filtered like the reactions topic, so the two never disagree.
     *
     * @throws NoSuchElementException if the author cannot be resolved; callers check {@link #isVisible}
     *                                first, except on removal, where an unresolvable author means the
     *                                message was never sent and its removal need not be either
     */
    public CommonPublicChatMessageDto toDto(CommonPublicChatMessage message) {
        UserProfile author = resolve(message.getAuthorUserProfileId());
        Optional<UserProfileDto> citationAuthor = message.getCitation()
                .flatMap(citation -> userProfileService.findUserProfile(citation.getAuthorUserProfileId()))
                .map(DtoMappings.UserProfileMapping::fromBisq2Model);
        Set<CommonPublicChatMessageReactionDto> reactions = reactionsOf(message)
                .filter(reaction -> isVisibleAuthor(reaction.getUserProfileId()))
                .map(this::toDto)
                .collect(Collectors.toSet());
        return CommonPublicChatMessageDtoMapping.fromBisq2Model(message, author, citationAuthor, reactions);
    }

    /**
     * {@link #toDto(CommonPublicChatMessage)} guarded for bulk mapping: the profile store is pruned
     * concurrently, so an author can vanish between the visibility check and the mapping, and one lost
     * author must cost one message rather than the whole response.
     */
    public Optional<CommonPublicChatMessageDto> findDto(CommonPublicChatMessage message) {
        try {
            return Optional.of(toDto(message));
        } catch (Exception e) {
            log.warn("Skipping chat message {}: it could not be mapped", message.getId(), e);
            return Optional.empty();
        }
    }

    /** @throws NoSuchElementException if the sender cannot be resolved, see {@link #toDto(CommonPublicChatMessage)} */
    public CommonPublicChatMessageReactionDto toDto(CommonPublicChatMessageReaction reaction) {
        return CommonPublicChatMessageReactionDtoMapping.fromBisq2Model(reaction, resolve(reaction.getUserProfileId()));
    }

    /** {@link #toDto(CommonPublicChatMessageReaction)} guarded for bulk mapping, see {@link #findDto(CommonPublicChatMessage)}. */
    public Optional<CommonPublicChatMessageReactionDto> findDto(CommonPublicChatMessageReaction reaction) {
        try {
            return Optional.of(toDto(reaction));
        } catch (Exception e) {
            log.warn("Skipping chat message reaction {}: it could not be mapped", reaction.getId(), e);
            return Optional.empty();
        }
    }

    /**
     * The reaction set is declared over the base type (a Lombok getter on {@code PublicChatMessage}); on a
     * public message only public reactions ever land in it.
     */
    public static Stream<CommonPublicChatMessageReaction> reactionsOf(CommonPublicChatMessage message) {
        return message.getChatMessageReactions().stream()
                .filter(CommonPublicChatMessageReaction.class::isInstance)
                .map(CommonPublicChatMessageReaction.class::cast);
    }

    private boolean isVisibleAuthor(String userProfileId) {
        return !bannedUserService.isUserProfileBanned(userProfileId)
                && userProfileService.findUserProfile(userProfileId).isPresent();
    }

    private UserProfile resolve(String userProfileId) {
        return userProfileService.findUserProfile(userProfileId)
                .orElseThrow(() -> new NoSuchElementException("No user profile found for " + userProfileId));
    }
}

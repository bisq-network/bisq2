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

package bisq.chat.common;

import bisq.chat.ChatChannelDomain;
import bisq.common.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Getter
public enum SubDomain {
    // We keep the titles of DISCUSSION_BISQ and SUPPORT_SUPPORT as otherwise we would break backward compatibility.
    // We avoid using the title field from the Channel anymore to avoid potential bugs with untyped values.
    DISCUSSION_BISQ(ChatChannelDomain.DISCUSSION, "bisq"),
    SUPPORT_SUPPORT(ChatChannelDomain.SUPPORT, "support"),

    // All those entries are not used anymore. If received from old peers, we migrate them to the give fallback value.
    @Deprecated DISCUSSION_BITCOIN(ChatChannelDomain.DISCUSSION, "bitcoin", DISCUSSION_BISQ),
    @Deprecated DISCUSSION_MARKETS(ChatChannelDomain.DISCUSSION, "markets", DISCUSSION_BISQ),
    @Deprecated DISCUSSION_OFF_TOPIC(ChatChannelDomain.DISCUSSION, "offTopic", DISCUSSION_BISQ),

    @Deprecated EVENTS_CONFERENCES(ChatChannelDomain.EVENTS, "conferences", DISCUSSION_BISQ),
    @Deprecated EVENTS_MEETUPS(ChatChannelDomain.EVENTS, "meetups", DISCUSSION_BISQ),
    @Deprecated EVENTS_PODCASTS(ChatChannelDomain.EVENTS, "podcasts", DISCUSSION_BISQ),
    @Deprecated EVENTS_TRADE_EVENTS(ChatChannelDomain.EVENTS, "tradeEvents", DISCUSSION_BISQ),

    @Deprecated SUPPORT_QUESTIONS(ChatChannelDomain.SUPPORT, "questions", SUPPORT_SUPPORT),
    @Deprecated SUPPORT_REPORTS(ChatChannelDomain.SUPPORT, "reports", SUPPORT_SUPPORT);

    public static SubDomain from(ChatChannelDomain chatChannelDomain, String channelTitle) {
        try {
            String name = chatChannelDomain.name().toUpperCase() + "_" + StringUtils.camelCaseToSnakeCase(channelTitle).toUpperCase();
            return valueOf(name);
        } catch (Exception e) {
            log.error("Cannot resolve ChatChannelSubDomain from chatChannelDomain {} and channelTitle={}.",
                    chatChannelDomain, channelTitle, e);
            throw e;
        }
    }

    public static SubDomain from(String channelId) {
        try {
            String[] tokens = channelId.split("\\.");
            String name = tokens[0].toUpperCase();
            ChatChannelDomain chatChannelDomain = ChatChannelDomain.valueOf(name);
            String channelTitle = tokens[1];
            return from(chatChannelDomain, channelTitle);
        } catch (Exception e) {
            log.error("Cannot resolve ChatChannelSubDomain from channelId {}.", channelId, e);
            throw e;
        }
    }

    private final ChatChannelDomain chatChannelDomain;
    private final String title;
    private final Optional<SubDomain> fallback;
    private final String channelId;

    SubDomain(ChatChannelDomain chatChannelDomain, String title) {
        this(chatChannelDomain, title, null);
    }

    SubDomain(ChatChannelDomain chatChannelDomain, String title, SubDomain fallback) {
        this.chatChannelDomain = chatChannelDomain;
        this.title = title;
        this.fallback = Optional.ofNullable(fallback);
        this.channelId = chatChannelDomain.name().toLowerCase() + "." + title;
    }

    public SubDomain migrate() {
        return fallback.orElse(this);
    }

    public boolean isDeprecated() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> field.getName().equals(name()))
                .peek(field -> field.setAccessible(true))
                .anyMatch(field -> field.isAnnotationPresent(Deprecated.class));
    }
}

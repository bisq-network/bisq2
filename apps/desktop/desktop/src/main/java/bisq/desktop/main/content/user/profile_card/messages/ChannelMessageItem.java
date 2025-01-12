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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.chat.Citation;
import bisq.chat.pub.PublicChatMessage;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;

import static bisq.desktop.main.content.chat.message_container.ChatMessageContainerView.EDITED_POST_FIX;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChannelMessageItem {
    @EqualsAndHashCode.Include
    private final PublicChatMessage publicChatMessage;

    private final UserProfile senderUserProfile;
    private final String message;
    private final String dateTime;
    private final Optional<Citation> citation;

    public ChannelMessageItem(PublicChatMessage publicChatMessage,
                              UserProfile senderUserProfile) {
        this.publicChatMessage = publicChatMessage;
        this.senderUserProfile = senderUserProfile;

        String editPostFix = publicChatMessage.isWasEdited() ? EDITED_POST_FIX : "";
        message = publicChatMessage.getText().orElse(Res.get("data.na")) + editPostFix;
        dateTime = DateFormatter.formatDateTime(new Date(publicChatMessage.getDate()),
                DateFormat.MEDIUM, DateFormat.SHORT, true, " " + Res.get("temporal.at") + " ");
        citation = publicChatMessage.getCitation();

        initialize();
    }

    private void initialize() {
    }

    public void dispose() {
    }
}

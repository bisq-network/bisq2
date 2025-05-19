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

package bisq.settings;

import lombok.Getter;

/**
 * Enum for don't show again checkboxes. If possible use the enum and not local keys to have better control about
 * the entries. We do not persist the key, but use the name of the enum as string for the key.
 */
public enum DontShowAgainKey {
    WELCOME("welcome"),
    CONFIRM_CLOSE_BISQ_EASY_TRADE,
    CONFIRM_CLOSE_MU_SIG_TRADE,
    OFFERALREADYTAKEN_WARN("offerAlreadyTaken.warn"),
    MEDIATOR_REMOVECASE_WARNING("mediator.removeCase.warning"),
    MEDIATOR_CLOSE_WARNING("mediator.close.warning"),
    MEDIATOR_LEAVECHANNEL_WARNING("mediator.leaveChannel.warning"),
    HYPERLINKS_OPEN_IN_BROWSER("hyperlinks.openInBrowser"),
    SEND_MSG_OFFER_ONLY_WARN("sendMsgOfferOnlyWarn"),
    SEND_OFFER_MSG_TEXT_ONLY_WARN("sendOfferMsgTextOnlyWarn");

    @Getter
    private final String key;

    DontShowAgainKey() {
        this.key = name();
    }

    DontShowAgainKey(String key) {
        this.key = key;
    }
}
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

package bisq.desktop.main.content.user.profile_card.my_notes;

import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.i18n.Res;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.profile.UserProfile;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ProfileCardMyNotesModel implements Model {
    private final String disclaimerText = Res.get("user.profileCard.myNotes.disclaimer");
    @Setter
    private UserProfile userProfile;
    @Setter
    private ContactListEntry contactListEntry;
    @Setter
    private String contactReasonAndDate = "";
    private final StringProperty tag = new SimpleStringProperty("");
    private final StringProperty trustScore = new SimpleStringProperty("");
    private final StringProperty notes = new SimpleStringProperty("");

    private final TextMaxLengthValidator tagMaxLengthValidator =
            new TextMaxLengthValidator(Res.get("user.profileCard.myNotes.transparentTextField.tag.maxLength",
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH),
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH);
    private final PercentageValidator trustScoreRangeValidator =
            new PercentageValidator(Res.get("user.profileCard.myNotes.transparentTextField.trustScore.range",
                    ContactListService.CONTACT_LIST_ENTRY_MIN_TRUST_SCORE * 100,
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TRUST_SCORE * 100),
                    ContactListService.CONTACT_LIST_ENTRY_MIN_TRUST_SCORE,
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TRUST_SCORE);
    private final TextMaxLengthValidator notesMaxLengthValidator =
            new TextMaxLengthValidator(Res.get("user.profileCard.myNotes.transparentTextField.notes.maxLength",
                    ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH),
                    ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH);
}

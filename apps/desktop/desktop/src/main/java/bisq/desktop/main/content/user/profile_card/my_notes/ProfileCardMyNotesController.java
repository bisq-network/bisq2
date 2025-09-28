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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.profile.UserProfile;
import lombok.Getter;

public class ProfileCardMyNotesController implements Controller {
    @Getter
    private final ProfileCardMyNotesView view;
    private final ProfileCardMyNotesModel model;
    private final ContactListService contactListService;

    public ProfileCardMyNotesController(ServiceProvider serviceProvider) {
        contactListService = serviceProvider.getUserService().getContactListService();

        model = new ProfileCardMyNotesModel();
        view = new ProfileCardMyNotesView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void setUserProfile(UserProfile userProfile) {
        model.setUserProfile(userProfile);
        contactListService.findContactListEntry(userProfile).ifPresentOrElse(contactListEntry -> {
                model.setContactListEntry(contactListEntry);
                model.getTag().set(contactListEntry.getTag().orElse(""));
                model.getTrustScore().set(getPercentageTrustScore(contactListEntry));
                model.getNotes().set(contactListEntry.getNotes().orElse(""));
                model.setContactReasonAndDate(getContactReasonAndDate(contactListEntry));
            },
            () -> {
                model.setContactListEntry(null);
                model.getTag().set("");
                model.getTrustScore().set("");
                model.getNotes().set("");
                model.setContactReasonAndDate("");
            });
    }

    void onSaveTag(String newTag) {
        UIThread.run(() -> {
            if (model.getContactListEntry() != null) {
                String trimmedNewTag = newTag.trim();
                contactListService.setTag(model.getContactListEntry(), trimmedNewTag);
                model.getTag().set(trimmedNewTag);
            }
        });
    }

    boolean onSaveTrustScore(String newTrustScore) {
        if (model.getContactListEntry() != null) {
            String trimmed = newTrustScore.trim().replace("%", "");
            try {
                double percent = Double.parseDouble(trimmed);
                double trustScore = percent / 100.0;
                ContactListEntry contactListEntry = model.getContactListEntry();
                contactListService.setTrustScore(contactListEntry, trustScore);
                model.getTrustScore().set(getPercentageTrustScore(contactListEntry));
                return true;
            } catch (NumberFormatException e) {
                model.getTrustScore().set(model.getTrustScore().get());
            }
        }
        return false;
    }

    void onSaveNotes(String newNotes) {
        UIThread.run(() -> {
            if (model.getContactListEntry() != null) {
                contactListService.setNotes(model.getContactListEntry(), newNotes);
                model.getNotes().set(newNotes);
            }
        });
    }

    private String getPercentageTrustScore(ContactListEntry contactListEntry) {
        return contactListEntry.getTrustScore()
                .map(PercentageFormatter::formatToPercentNoDecimalsWithSymbol)
                .orElse("");
    }

    private String getContactReasonAndDate(ContactListEntry contactListEntry) {
        String contactReason = contactListEntry.getContactReason().getDisplayString();
        String date = DateFormatter.formatDayMonthOrDayMonthYear(contactListEntry.getDate());
        return Res.get("user.profileCard.myNotes.transparentTextField.contactReason", contactReason, date);
    }
}

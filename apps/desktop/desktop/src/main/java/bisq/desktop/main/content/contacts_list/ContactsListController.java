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

package bisq.desktop.main.content.contacts_list;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.common.data.Pair;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.profile_card.ProfileCardController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ContactsListController implements Controller {
    @Getter
    private final ContactsListView view;
    private final ContactsListModel model;
    private final ReputationService reputationService;
    private final UserProfileService userProfileService;
    private final ContactListService contactListService;
    private final ChatService chatService;
    private Pin proofOfBurnScoreChangedFlagPin,
            bondedReputationScoreChangedFlagPin, signedWitnessScoreChangedFlagPin,
            accountAgeScoreChangedFlagPin, contactsListEntriesPin;

    public ContactsListController(ServiceProvider serviceProvider) {
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        reputationService = serviceProvider.getUserService().getReputationService();
        contactListService = serviceProvider.getUserService().getContactListService();
        chatService = serviceProvider.getChatService();

        model = new ContactsListModel();
        view = new ContactsListView(model, this);
    }

    @Override
    public void onActivate() {
        contactsListEntriesPin = contactListService.getContactListEntries().addObserver(new CollectionObserver<>() {
            @Override
            public void add(ContactListEntry contactListEntry) {
                UIThread.run(() -> {
                    ContactsListView.ListItem listItem = new ContactsListView.ListItem(contactListEntry,
                            reputationService,
                            ContactsListController.this,
                            userProfileService);
                    if (!model.getListItems().contains(listItem)) {
                        model.getListItems().add(listItem);
                    }
                });
            }

            @Override
            public void remove(Object element) {
                if (element instanceof ContactListEntry contactListEntry) {
                    UIThread.run(() -> {
                        Optional<ContactsListView.ListItem> toRemove = model.getListItems().stream()
                                .filter(e -> e.getUserProfile().getId().equals(contactListEntry.getUserProfile().getId()))
                                .findAny();
                        toRemove.ifPresent(item -> model.getListItems().remove(item));
                    });
                }
            }

            @Override
            public void clear() {
                UIThread.run(() -> model.getListItems().clear());
            }
        });

        proofOfBurnScoreChangedFlagPin = reputationService.getProofOfBurnService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        bondedReputationScoreChangedFlagPin = reputationService.getBondedReputationService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        accountAgeScoreChangedFlagPin = reputationService.getAccountAgeService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
        signedWitnessScoreChangedFlagPin = reputationService.getSignedWitnessService().getUserProfileIdScorePair()
                .addObserver(this::updateScore);
    }

    @Override
    public void onDeactivate() {
        contactsListEntriesPin.unbind();
        proofOfBurnScoreChangedFlagPin.unbind();
        bondedReputationScoreChangedFlagPin.unbind();
        accountAgeScoreChangedFlagPin.unbind();
        signedWitnessScoreChangedFlagPin.unbind();

        model.getListItems().forEach(ContactsListView.ListItem::dispose);
        model.getListItems().clear();
    }

    void onOpenPrivateChat(String userProfileId) {
        userProfileService.findUserProfile(userProfileId)
                .ifPresent(this::createAndSelectTwoPartyPrivateChatChannel);
    }

    void onShowMoreInfo(UserProfile userProfile) {
        // TODO: change to new page in ProfileCard
        Navigation.navigateTo(NavigationTarget.PROFILE_CARD,
                new ProfileCardController.InitData(userProfile));
    }

    void onRemoveContact(ContactListEntry contactListEntry) {
        new Popup().warning(Res.get("contactsList.table.columns.actionsMenu.removeContact"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(() -> doRemoveContact(contactListEntry))
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void applySearchPredicate(String searchText) {
        String string = searchText == null ? "" : searchText.toLowerCase();
        model.setSearchStringPredicate(item ->
                StringUtils.isEmpty(string) ||
                        item.getUserName().toLowerCase().contains(string) ||
                        item.getTag().toLowerCase().contains(string) ||
                        item.getTrustScore().toLowerCase().contains(string) ||
                        item.getUserProfile().getNym().toLowerCase().contains(string) ||
                        item.getTotalScoreString().toLowerCase().contains(string) ||
                        item.getProfileAgeString().toLowerCase().contains(string));
        applyPredicates();
    }

    private void doRemoveContact(ContactListEntry contactListEntry) {
        contactListService.removeContactListEntry(contactListEntry);
    }

    private void updateScore(Pair<String, Long> userProfileIdScorePair) {
        if (userProfileIdScorePair == null) {
            return;
        }
        String userProfileId = userProfileIdScorePair.getFirst();
        UIThread.run(() -> {
            model.getListItems().stream().filter(e -> e.getUserProfile().getId().equals(userProfileId))
                    .forEach(item -> item.applyReputationScore(userProfileId));
            model.getScoreChangeTrigger().set(!model.getScoreChangeTrigger().get());
        });
    }

    private void applyPredicates() {
        model.getFilteredList().setPredicate(item ->
                model.getFilterItemPredicate().test(item) &&
                        model.getSearchStringPredicate().test(item)
        );
    }

    private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
        // Private chats are all using the DISCUSSION ChatChannelDomain
        chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, peer)
                .ifPresent(channel -> Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE));
    }
}

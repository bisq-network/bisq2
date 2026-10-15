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

package bisq.desktop.main.content.authorized_role.mediator.bisq_easy;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeSelectionService;
import bisq.common.observable.Pin;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.support.mediation.bisq_easy.BisqEasyMediationCase;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequest;
import bisq.support.mediation.bisq_easy.BisqEasyMediatorService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.InvalidationListener;
import javafx.collections.transformation.SortedList;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.stream.Stream;

import static bisq.chat.ChatChannelDomain.BISQ_EASY_OPEN_TRADES;

@Slf4j
public class BisqEasyMediatorController implements Controller {
    @Getter
    protected final BisqEasyMediatorModel model;
    @Getter
    protected final BisqEasyMediatorView view;
    protected final ServiceProvider serviceProvider;
    protected final ChatService chatService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChatMessageContainerController chatMessageContainerController;

    private final BisqEasyOpenTradeSelectionService selectionService;
    private final BisqEasyMediationCaseHeader bisqEasyMediationCaseHeader;
    private final BisqEasyMediatorService bisqEasyMediatorService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final InvalidationListener itemListener;
    private Pin mediationCaseListItemPin, selectedChannelPin;
    private Subscription searchPredicatePin, closedCasesPredicatePin;

    public BisqEasyMediatorController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        BisqEasyOpenTradeChannelService channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesSelectionService();
        bisqEasyMediatorService = serviceProvider.getSupportService().getBisqEasyMediatorService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider, BISQ_EASY_OPEN_TRADES, e -> {
        });
        bisqEasyMediationCaseHeader = new BisqEasyMediationCaseHeader(serviceProvider, this::closeCaseHandler, this::reOpenCaseHandler);

        model = new BisqEasyMediatorModel();
        view = new BisqEasyMediatorView(model, this, bisqEasyMediationCaseHeader.getRoot(), chatMessageContainerController.getView().getRoot());

        itemListener = observable -> {
            // We need to set predicate when a new item gets added.
            // Delaying it a render frame as otherwise the list shows an empty row for unclear reasons.
            UIThread.runOnNextRenderFrame(() -> {
                model.getListItems().setPredicate(item -> model.getSearchPredicate().get().test(item) && model.getClosedCasesPredicate().get().test(item));
                updateEmptyState();
                if (model.getListItems().getFilteredList().size() == 1) {
                    selectItem(model.getListItems().getFilteredList().get(0));
                }
            });
        };
    }

    @Override
    public void onActivate() {
        applyFilteredListPredicate(model.getShowClosedCases().get());

        mediationCaseListItemPin = FxBindings.<BisqEasyMediationCase, BisqEasyMediationCaseListItem>bind(model.getListItems())
                .filter(mediationCase -> {
                    BisqEasyMediationRequest bisqEasyMediationRequest = mediationCase.getBisqEasyMediationRequest();
                    BisqEasyContract contract = bisqEasyMediationRequest.getContract();
                    Optional<UserProfile> mediatorFromContract = contract.getMediator();
                    if (mediatorFromContract.isEmpty()) {
                        return false;
                    }
                    Optional<UserIdentity> myOptionalUserIdentity = bisqEasyMediatorService.findMyMediatorUserIdentity(mediatorFromContract);
                    if (myOptionalUserIdentity.isEmpty()) {
                        return false;
                    }

                    BisqEasyOpenTradeChannel channel = findOrCreateChannel(bisqEasyMediationRequest, myOptionalUserIdentity.get());
                    if (channel.getMediator().isEmpty()) {
                        // In case we found an existing channel at mediatorFindOrCreatesChannel the mediator field could be empty
                        return false;
                    }
                    if (!isChannelMatchingContract(channel, contract)) {
                        log.error("Validation of channel properties and contract properties failed. " +
                                "channel={}; contract={}", channel, contract);
                        return false;
                    }
                    return true;
                })
                .map(mediationCase -> {
                    BisqEasyMediationRequest bisqEasyMediationRequest = mediationCase.getBisqEasyMediationRequest();
                    UserIdentity myUserIdentity = bisqEasyMediatorService.findMyMediatorUserIdentity(bisqEasyMediationRequest.getContract().getMediator()).orElseThrow();
                    BisqEasyOpenTradeChannel channel = findOrCreateChannel(bisqEasyMediationRequest, myUserIdentity);
                    return new BisqEasyMediationCaseListItem(serviceProvider, mediationCase, channel);
                })
                .to(bisqEasyMediatorService.getMediationCases());

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        searchPredicatePin = EasyBind.subscribe(model.getSearchPredicate(), searchPredicate -> updatePredicate());
        closedCasesPredicatePin = EasyBind.subscribe(model.getClosedCasesPredicate(), closedCasesPredicate -> updatePredicate());
        maybeSelectFirst();
        updateEmptyState();

        model.getListItems().addListener(itemListener);
    }

    @Override
    public void onDeactivate() {
        doCloseChatWindow();

        model.getListItems().removeListener(itemListener);
        model.getListItems().onDeactivate();
        model.reset();

        mediationCaseListItemPin.unbind();
        selectedChannelPin.unbind();
        searchPredicatePin.unsubscribe();
        closedCasesPredicatePin.unsubscribe();
    }

    void onSelectItem(BisqEasyMediationCaseListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else {
            selectItem(item);
        }
    }

    void onToggleClosedCases() {
        model.getShowClosedCases().set(!model.getShowClosedCases().get());
        applyShowClosedCasesChange();
    }


    void onToggleChatWindow() {
        if (model.getChatWindow().get() == null) {
            model.getChatWindow().set(new Stage());
        } else {
            doCloseChatWindow();
        }
    }

    void onCloseChatWindow() {
        doCloseChatWindow();
    }

    private void doCloseChatWindow() {
        if (model.getChatWindow().get() != null) {
            model.getChatWindow().get().hide();
        }
        model.getChatWindow().set(null);
    }

    private void closeCaseHandler() {
        applyShowClosedCasesChange();
    }

    private void reOpenCaseHandler() {
        applyShowClosedCasesChange();
    }

    private void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedItem().set(null);
                bisqEasyMediationCaseHeader.setMediationCaseListItem(null);
                bisqEasyMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                maybeSelectFirst();
                updateEmptyState();
            } else if (chatChannel instanceof BisqEasyOpenTradeChannel tradeChannel) {
                BisqEasyMediationCaseListItem selectedItem = model.getSelectedItem().get();
                if (selectedItem != null && selectedItem.getChannel().getId().equals(tradeChannel.getId())) {
                    showInHeader(selectedItem);
                } else {
                    findFirstItemOfChannel(tradeChannel).ifPresent(item -> {
                        model.getSelectedItem().set(item);
                        showInHeader(item);
                    });
                }
            }
        });
    }

    // All cases of a trade share the trade's channel, so the selected row, not the channel, decides which case
    // the header shows and acts on.
    private void selectItem(BisqEasyMediationCaseListItem item) {
        model.getSelectedItem().set(item);
        showInHeader(item);
        if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
        }
    }

    private void showInHeader(BisqEasyMediationCaseListItem item) {
        bisqEasyMediationCaseHeader.setMediationCaseListItem(item);
        bisqEasyMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
    }

    // A shown row in table order first, so that the choice among several cases of the channel is deterministic.
    private Optional<BisqEasyMediationCaseListItem> findFirstItemOfChannel(BisqEasyOpenTradeChannel channel) {
        return Stream.concat(model.getListItems().getSortedList().stream(), model.getListItems().stream())
                .filter(item -> item.getChannel().getId().equals(channel.getId()))
                .findFirst();
    }

    private void applyShowClosedCasesChange() {
        bisqEasyMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
        // Need a predicate change to trigger a list update
        applyFilteredListPredicate(!model.getShowClosedCases().get());
        applyFilteredListPredicate(model.getShowClosedCases().get());
        maybeSelectFirst();
    }

    private void updatePredicate() {
        model.getListItems().setPredicate(item -> model.getSearchPredicate().get().test(item) && model.getClosedCasesPredicate().get().test(item));
        maybeSelectFirst();
        updateEmptyState();
    }

    private void applyFilteredListPredicate(boolean showClosedCases) {
        if (showClosedCases) {
            model.getClosedCasesPredicate().set(item -> item.getBisqEasyMediationCase().isClosed());
        } else {
            model.getClosedCasesPredicate().set(item -> !item.getBisqEasyMediationCase().isClosed());
        }
    }

    private void updateEmptyState() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<BisqEasyMediationCaseListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            selectionService.getSelectedChannel().set(null);
            bisqEasyMediationCaseHeader.setMediationCaseListItem(null);
        } else {
            bisqEasyMediationCaseHeader.setMediationCaseListItem(model.getSelectedItem().get());
        }
    }

    private void maybeSelectFirst() {
        UIThread.runOnNextRenderFrame(() -> {
            if (!model.getListItems().getFilteredList().isEmpty()) {
                selectItem(model.getListItems().getSortedList().get(0));
            }
        });
    }

    // The case list shows a mediation case only if the mediator's channel of the trade matches the contract of the
    // mediation request. The caller has checked that both have a mediator. The mediator's node creates the channel with
    // the mediator's current profile, while the contract holds the profile which the taker saw when it took the offer.
    // A profile can change its terms, its statement and the addresses of its network ID, so the mediators are compared
    // by their profile IDs, the hash of their key. See docs/specifications/user/identity-by-key.md.
    // All cases of a trade share its channel, whose offer is a display copy from the first accepted request. Each case
    // shows its own contract, so the offer is not compared, and a case whose contract has a differing offer is listed
    // as well (docs/specifications/support/mediation-request-validation.md).
    @VisibleForTesting
    static boolean isChannelMatchingContract(BisqEasyOpenTradeChannel channel, BisqEasyContract contract) {
        return channel.getTraders().size() == 2 &&
                channel.getMediator().map(UserProfile::getId).equals(contract.getMediator().map(UserProfile::getId));
    }

    private BisqEasyOpenTradeChannel findOrCreateChannel(BisqEasyMediationRequest bisqEasyMediationRequest,
                                                         UserIdentity myUserIdentity) {
        BisqEasyContract contract = bisqEasyMediationRequest.getContract();
        return bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                bisqEasyMediationRequest.getTradeId(),
                contract.getOffer(),
                myUserIdentity,
                bisqEasyMediationRequest.getRequester(),
                bisqEasyMediationRequest.getPeer());
    }
}

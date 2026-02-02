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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeSelectionService;
import bisq.common.observable.Pin;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.InvalidationListener;
import javafx.collections.transformation.SortedList;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

import static bisq.chat.ChatChannelDomain.MU_SIG_OPEN_TRADES;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MuSigMediatorController implements Controller {
    @Getter
    protected final MuSigMediatorModel model;
    @Getter
    protected final MuSigMediatorView view;
    protected final ServiceProvider serviceProvider;
    protected final ChatService chatService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChatMessageContainerController chatMessageContainerController;

    private final MuSigOpenTradeSelectionService selectionService;
    private final MuSigMediationCaseHeader muSigMediationCaseHeader;
    private final MuSigMediatorService muSigMediatorService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final InvalidationListener itemListener;
    private Pin mediationCaseListItemPin, selectedChannelPin;
    private Subscription searchPredicatePin, closedCasesPredicatePin;

    public MuSigMediatorController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        MuSigOpenTradeChannelService channelService = chatService.getMuSigOpenTradeChannelService();
        selectionService = chatService.getMuSigOpenTradesSelectionService();
        muSigMediatorService = serviceProvider.getSupportService().getMuSigMediatorService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider, MU_SIG_OPEN_TRADES, e -> {
        });
        muSigMediationCaseHeader = new MuSigMediationCaseHeader(serviceProvider, this::closeCaseHandler, this::reOpenCaseHandler);

        model = new MuSigMediatorModel();
        view = new MuSigMediatorView(model, this, muSigMediationCaseHeader.getRoot(), chatMessageContainerController.getView().getRoot());

        itemListener = observable -> {
            // We need to set predicate when a new item gets added.
            // Delaying it a render frame as otherwise the list shows an empty row for unclear reasons.
            UIThread.runOnNextRenderFrame(() -> {
                model.getListItems().setPredicate(item -> model.getSearchPredicate().get().test(item) && model.getClosedCasesPredicate().get().test(item));
                updateEmptyState();
                if (model.getListItems().getFilteredList().size() == 1) {
                    selectionService.selectChannel(model.getListItems().getFilteredList().get(0).getChannel());
                }
            });
        };
    }

    @Override
    public void onActivate() {
        applyFilteredListPredicate(model.getShowClosedCases().get());

        mediationCaseListItemPin = FxBindings.<MuSigMediationCase, MuSigMediationCaseListItem>bind(model.getListItems())
                .filter(mediationCase -> {
                    MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
                    MuSigContract contract = muSigMediationRequest.getContract();
                    Optional<UserProfile> mediatorFromContract = contract.getMediator();
                    if (mediatorFromContract.isEmpty()) {
                        return false;
                    }
                    Optional<UserIdentity> myOptionalUserIdentity = muSigMediatorService.findMyMediatorUserIdentity(mediatorFromContract);
                    if (myOptionalUserIdentity.isEmpty()) {
                        return false;
                    }

                    MuSigOpenTradeChannel channel = findOrCreateChannel(muSigMediationRequest, myOptionalUserIdentity.get());
                    if (channel.getMediator().isEmpty()) {
                        // In case we found an existing channel at mediatorFindOrCreatesChannel the mediator field could be empty
                        return false;
                    }
                    try {
                        checkArgument(channel.getTraders().size() == 2);
                        // TODO: check whether this is needed or not
//                        checkArgument(channel.getMuSigOffer().equals(contract.getOffer()));
                        checkArgument(channel.getMediator().orElseThrow().equals(contract.getMediator().orElseThrow()));
                    } catch (IllegalArgumentException e) {
                        log.error("Validation of channel properties and contract properties failed. " +
                                "channel={}; contract={}", channel, contract, e);
                        return false;
                    }
                    return true;
                })
                .map(mediationCase -> {
                    MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
                    UserIdentity myUserIdentity = muSigMediatorService.findMyMediatorUserIdentity(muSigMediationRequest.getContract().getMediator()).orElseThrow();
                    MuSigOpenTradeChannel channel = findOrCreateChannel(muSigMediationRequest, myUserIdentity);
                    return new MuSigMediationCaseListItem(serviceProvider, mediationCase, channel);
                })
                .to(muSigMediatorService.getMediationCases());

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

    void onSelectItem(MuSigMediationCaseListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
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
                muSigMediationCaseHeader.setMediationCaseListItem(null);
                muSigMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                maybeSelectFirst();
                updateEmptyState();
            } else if (chatChannel instanceof MuSigOpenTradeChannel tradeChannel) {
                model.getListItems().stream()
                        .filter(item -> item.getChannel().getId().equals(tradeChannel.getId()))
                        .findAny()
                        .ifPresent(item -> {
                            model.getSelectedItem().set(item);
                            muSigMediationCaseHeader.setMediationCaseListItem(item);
                            muSigMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                        });
            }
        });
    }

    private void applyShowClosedCasesChange() {
        muSigMediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
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
            model.getClosedCasesPredicate().set(item -> item.getMuSigMediationCase().getIsClosed().get());
        } else {
            model.getClosedCasesPredicate().set(item -> !item.getMuSigMediationCase().getIsClosed().get());
        }
    }

    private void updateEmptyState() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<MuSigMediationCaseListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            selectionService.getSelectedChannel().set(null);
            muSigMediationCaseHeader.setMediationCaseListItem(null);
        } else {
            muSigMediationCaseHeader.setMediationCaseListItem(model.getSelectedItem().get());
        }
    }

    private void maybeSelectFirst() {
        UIThread.runOnNextRenderFrame(() -> {
            if (!model.getListItems().getFilteredList().isEmpty()) {
                selectionService.selectChannel(model.getListItems().getSortedList().get(0).getChannel());
            }
        });
    }

    private MuSigOpenTradeChannel findOrCreateChannel(MuSigMediationRequest muSigMediationRequest,
                                                      UserIdentity myUserIdentity) {
        MuSigContract contract = muSigMediationRequest.getContract();
        return muSigOpenTradeChannelService.mediatorFindOrCreatesChannel(
                muSigMediationRequest.getTradeId(),
//                contract.getOffer(),
                myUserIdentity,
                muSigMediationRequest.getRequester(),
                muSigMediationRequest.getPeer());
    }
}

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

package bisq.desktop.main.content.authorized_role.mediator;

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
import bisq.support.mediation.MediationCase;
import bisq.support.mediation.MediationRequest;
import bisq.support.mediation.MediatorService;
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

import static bisq.chat.ChatChannelDomain.BISQ_EASY_OPEN_TRADES;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MediatorController implements Controller {
    @Getter
    protected final MediatorModel model;
    @Getter
    protected final MediatorView view;
    protected final ServiceProvider serviceProvider;
    protected final ChatService chatService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChatMessageContainerController chatMessageContainerController;

    private final BisqEasyOpenTradeSelectionService selectionService;
    private final MediationCaseHeader mediationCaseHeader;
    private final MediatorService mediatorService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final InvalidationListener itemListener;
    private Pin mediationCaseListItemPin, selectedChannelPin;
    private Subscription searchPredicatePin, closedCasesPredicatePin;

    public MediatorController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        BisqEasyOpenTradeChannelService channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesSelectionService();
        mediatorService = serviceProvider.getSupportService().getMediatorService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider, BISQ_EASY_OPEN_TRADES, e -> {
        });
        mediationCaseHeader = new MediationCaseHeader(serviceProvider, this::closeCaseHandler, this::reOpenCaseHandler);

        model = new MediatorModel();
        view = new MediatorView(model, this, mediationCaseHeader.getRoot(), chatMessageContainerController.getView().getRoot());

        itemListener = observable -> update();
    }

    @Override
    public void onActivate() {
        model.getListItems().onActivate();
        applyFilteredListPredicate(model.getShowClosedCases().get());

        mediationCaseListItemPin = FxBindings.<MediationCase, MediationCaseListItem>bind(model.getListItems())
                .filter(mediationCase -> {
                    MediationRequest mediationRequest = mediationCase.getMediationRequest();
                    BisqEasyContract contract = mediationRequest.getContract();
                    Optional<UserProfile> mediatorFromContract = contract.getMediator();
                    if (mediatorFromContract.isEmpty()) {
                        return false;
                    }
                    Optional<UserIdentity> myOptionalUserIdentity = mediatorService.findMyMediatorUserIdentity(mediatorFromContract);
                    if (myOptionalUserIdentity.isEmpty()) {
                        return false;
                    }

                    BisqEasyOpenTradeChannel channel = findOrCreateChannel(mediationRequest, myOptionalUserIdentity.get());
                    if (channel.getMediator().isEmpty()) {
                        // In case we found an existing channel at mediatorFindOrCreatesChannel the mediator field could be empty
                        return false;
                    }
                    try {
                        checkArgument(channel.getTraders().size() == 2);
                        checkArgument(channel.getBisqEasyOffer().equals(contract.getOffer()));
                        checkArgument(channel.getMediator().orElseThrow().equals(contract.getMediator().orElseThrow()));
                    } catch (IllegalArgumentException e) {
                        log.error("Validation of channel properties and contract properties failed. " +
                                "channel={}; contract={}", channel, contract, e);
                        return false;
                    }
                    return true;
                })
                .map(mediationCase -> {
                    MediationRequest mediationRequest = mediationCase.getMediationRequest();
                    UserIdentity myUserIdentity = mediatorService.findMyMediatorUserIdentity(mediationRequest.getContract().getMediator()).orElseThrow();
                    BisqEasyOpenTradeChannel channel = findOrCreateChannel(mediationRequest, myUserIdentity);
                    return new MediationCaseListItem(serviceProvider, mediationCase, channel);
                })
                .to(mediatorService.getMediationCases());

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        searchPredicatePin = EasyBind.subscribe(model.getSearchPredicate(), searchPredicate -> updatePredicate());
        closedCasesPredicatePin = EasyBind.subscribe(model.getClosedCasesPredicate(), closedCasesPredicate -> updatePredicate());
        maybeSelectFirst();
        update();

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

    void onSelectItem(MediationCaseListItem item) {
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
                mediationCaseHeader.setMediationCaseListItem(null);
                mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                maybeSelectFirst();
                update();
            } else if (chatChannel instanceof BisqEasyOpenTradeChannel tradeChannel) {
                model.getListItems().stream()
                        .filter(item -> item.getChannel().getId().equals(tradeChannel.getId()))
                        .findAny()
                        .ifPresent(item -> {
                            model.getSelectedItem().set(item);
                            mediationCaseHeader.setMediationCaseListItem(item);
                            mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                        });
            }
        });
    }

    private void applyShowClosedCasesChange() {
        mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
        // Need a predicate change to trigger a list update
        applyFilteredListPredicate(!model.getShowClosedCases().get());
        applyFilteredListPredicate(model.getShowClosedCases().get());
        maybeSelectFirst();
    }

    private void updatePredicate() {
        model.getListItems().setPredicate(item -> model.getSearchPredicate().get().test(item) && model.getClosedCasesPredicate().get().test(item));
        maybeSelectFirst();
        update();
    }

    private void applyFilteredListPredicate(boolean showClosedCases) {
        if (showClosedCases) {
            model.getClosedCasesPredicate().set(item -> item.getMediationCase().getIsClosed().get());
        } else {
            model.getClosedCasesPredicate().set(item -> !item.getMediationCase().getIsClosed().get());
        }
    }

    private void update() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<MediationCaseListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            selectionService.getSelectedChannel().set(null);
            mediationCaseHeader.setMediationCaseListItem(null);
        } else {
            mediationCaseHeader.setMediationCaseListItem(model.getSelectedItem().get());
        }
    }

    private void maybeSelectFirst() {
        UIThread.runOnNextRenderFrame(() -> {
            if (!model.getListItems().getFilteredList().isEmpty()) {
                selectionService.selectChannel(model.getListItems().getSortedList().get(0).getChannel());
            }
        });
    }

    private BisqEasyOpenTradeChannel findOrCreateChannel(MediationRequest mediationRequest,
                                                         UserIdentity myUserIdentity) {
        BisqEasyContract contract = mediationRequest.getContract();
        return bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                mediationRequest.getTradeId(),
                contract.getOffer(),
                myUserIdentity,
                mediationRequest.getRequester(),
                mediationRequest.getPeer());
    }
}

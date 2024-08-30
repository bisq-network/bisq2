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

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeSelectionService;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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

    private final BisqEasyOpenTradeChannelService channelService;
    private final BisqEasyOpenTradeSelectionService selectionService;
    private final MediationCaseHeader mediationCaseHeader;
    private final MediatorService mediatorService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final InvalidationListener itemListener;
    private Pin mediationCaseListItemPin, selectedChannelPin;

    public MediatorController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        channelService = chatService.getBisqEasyOpenTradeChannelService();
        selectionService = chatService.getBisqEasyOpenTradesSelectionService();
        mediatorService = serviceProvider.getSupportService().getMediatorService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();

        chatMessageContainerController = new ChatMessageContainerController(serviceProvider, ChatChannelDomain.BISQ_EASY_OPEN_TRADES, e -> {
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

        mediationCaseListItemPin = FxBindings.<MediationCase, MediatorView.ListItem>bind(model.getListItems())
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
                    return new MediatorView.ListItem(serviceProvider, mediationCase, channel);
                })
                .to(mediatorService.getMediationCases());

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(chatChannel -> {
            UIThread.run(() -> {
                if (chatChannel == null) {
                    model.getSelectedItem().set(null);
                    mediationCaseHeader.setMediationCaseListItem(null);
                    mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                    model.getSelectedChannel().set(null);
                    update();
                } else if (chatChannel instanceof BisqEasyOpenTradeChannel) {
                    model.getSelectedChannel().set(chatChannel);
                    BisqEasyOpenTradeChannel channel = (BisqEasyOpenTradeChannel) chatChannel;
                    model.getListItems().stream()
                            .filter(item -> item.getChannel().equals(channel))
                            .findAny()
                            .ifPresent(item -> {
                                model.getSelectedItem().set(item);
                                mediationCaseHeader.setMediationCaseListItem(item);
                                mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                            });
                }
            });
        });

        update();

        model.getListItems().addListener(itemListener);
    }

    @Override
    public void onDeactivate() {
        model.getListItems().removeListener(itemListener);
        model.getListItems().onDeactivate();
        mediationCaseListItemPin.unbind();
        selectedChannelPin.unbind();
    }

    void onSelectItem(MediatorView.ListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
        }
    }

    void onToggleClosedCases() {
        model.getShowClosedCases().set(!model.getShowClosedCases().get());
        mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
        applyFilteredListPredicate(model.getShowClosedCases().get());
    }

    private void applyFilteredListPredicate(boolean showClosedCases) {
        if (showClosedCases) {
            model.getListItems().setPredicate(item -> item.getMediationCase().getIsClosed().get());
        } else {
            model.getListItems().setPredicate(item -> !item.getMediationCase().getIsClosed().get());
        }
        update();
    }

    private void update() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<MediatorView.ListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            selectionService.getSelectedChannel().set(null);
            mediationCaseHeader.setMediationCaseListItem(null);
        } else {
            selectionService.getSelectedChannel().set(sortedList.get(0).getChannel());
            mediationCaseHeader.setMediationCaseListItem(model.getSelectedItem().get());
        }
    }

    private BisqEasyOpenTradeChannel findOrCreateChannel(MediationRequest mediationRequest, UserIdentity myUserIdentity) {
        BisqEasyContract contract = mediationRequest.getContract();
        return bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                mediationRequest.getTradeId(),
                contract.getOffer(),
                myUserIdentity,
                mediationRequest.getRequester(),
                mediationRequest.getPeer());
    }

    private void closeCaseHandler() {
        onToggleClosedCases();
    }

    private void reOpenCaseHandler() {
        onToggleClosedCases();
    }
}

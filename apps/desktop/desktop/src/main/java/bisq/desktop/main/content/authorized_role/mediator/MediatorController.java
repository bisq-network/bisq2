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
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeSelectionService;
import bisq.chat.priv.PrivateGroupChatChannel;
import bisq.common.observable.Pin;
import bisq.contract.Contract;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.contract.mu_sig.MuSigContract;
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
import static bisq.chat.ChatChannelDomain.MU_SIG_OPEN_TRADES;
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
    protected final ChatMessageContainerController bisqEasyOpenTradeChatMessageContainerController;
    protected final ChatMessageContainerController muSigOpenTradeChatMessageContainerController;

    private final BisqEasyOpenTradeSelectionService bisqEasyOpenTradeSelectionService;
    private final MuSigOpenTradeSelectionService muSigOpenTradeSelectionService;
    private final MediationCaseHeader mediationCaseHeader;
    private final MediatorService mediatorService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private final InvalidationListener itemListener;
    private Pin mediationCaseListItemPin, bisqEasyOpenTradeSelectedChannelPin, muSigOpenTradeSelectedChannelPin;
    private Subscription searchPredicatePin, closedCasesPredicatePin;

    public MediatorController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        bisqEasyOpenTradeSelectionService = chatService.getBisqEasyOpenTradesSelectionService();
        muSigOpenTradeSelectionService = chatService.getMuSigOpenTradesSelectionService();
        mediatorService = serviceProvider.getSupportService().getMediatorService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        muSigOpenTradeChannelService = chatService.getMuSigOpenTradeChannelService();

        bisqEasyOpenTradeChatMessageContainerController = new ChatMessageContainerController(serviceProvider, BISQ_EASY_OPEN_TRADES, e -> {
        });
        muSigOpenTradeChatMessageContainerController = new ChatMessageContainerController(serviceProvider, MU_SIG_OPEN_TRADES, e -> {
        });
        mediationCaseHeader = new MediationCaseHeader(serviceProvider, this::closeCaseHandler, this::reOpenCaseHandler);

        model = new MediatorModel();
        view = new MediatorView(
                model,
                this,
                mediationCaseHeader.getRoot(),
                bisqEasyOpenTradeChatMessageContainerController.getView().getRoot(),
                muSigOpenTradeChatMessageContainerController.getView().getRoot());

        itemListener = observable -> {
            // We need to set predicate when a new item gets added.
            // Delaying it a render frame as otherwise the list shows an empty row for unclear reasons.
            UIThread.runOnNextRenderFrame(() -> {
                model.getListItems().setPredicate(item -> model.getSearchPredicate().get().test(item) && model.getClosedCasesPredicate().get().test(item));
                updateEmpytState();
                if (model.getListItems().getFilteredList().size() == 1) {
                    var channel = model.getListItems().getFilteredList().getFirst().getChannel();
                    if (channel instanceof BisqEasyOpenTradeChannel) {
                        bisqEasyOpenTradeSelectionService.selectChannel(channel);
                    } else if (channel instanceof MuSigOpenTradeChannel) {
                        muSigOpenTradeSelectionService.selectChannel(channel);
                    }
                }
            });
        };
    }

    @Override
    public void onActivate() {
        applyFilteredListPredicate(model.getShowClosedCases().get());

        mediationCaseListItemPin = FxBindings.<MediationCase, MediationCaseListItem>bind(model.getListItems())
                .filter(mediationCase -> {
                    MediationRequest mediationRequest = mediationCase.getMediationRequest();
                    Contract<?> contract = mediationRequest.getContract();
                    Optional<UserProfile> mediatorFromContract = getMediator(contract);
                    if (mediatorFromContract.isEmpty()) {
                        return false;
                    }
                    Optional<UserIdentity> myOptionalUserIdentity = mediatorService.findMyMediatorUserIdentity(mediatorFromContract);
                    if (myOptionalUserIdentity.isEmpty()) {
                        return false;
                    }

                    PrivateGroupChatChannel<?> channel = findOrCreateChannel(mediationRequest, myOptionalUserIdentity.get());

                    if (channel instanceof BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel) {
                        if (bisqEasyOpenTradeChannel.getMediator().isEmpty()) {
                            // In case we found an existing channel at mediatorFindOrCreatesChannel the mediator field could be empty
                            return false;
                        }
                        try {
                            checkArgument(bisqEasyOpenTradeChannel.getTraders().size() == 2);
                            checkArgument(bisqEasyOpenTradeChannel.getBisqEasyOffer().equals(contract.getOffer()));
                            checkArgument(bisqEasyOpenTradeChannel.getMediator().orElseThrow().equals(mediatorFromContract.orElseThrow()));
                        } catch (IllegalArgumentException e) {
                            log.error("Validation of channel properties and contract properties failed. " +
                                    "channel={}; contract={}", channel, contract, e);
                            return false;
                        }
                    } else if (channel instanceof MuSigOpenTradeChannel muSigOpenTradeChannel) {
                        if (muSigOpenTradeChannel.getMediator().isEmpty()) {
                            // In case we found an existing channel at mediatorFindOrCreatesChannel the mediator field could be empty
                            return false;
                        }
                        try {
                            checkArgument(muSigOpenTradeChannel.getTraders().size() == 2);
//                            checkArgument(muSigOpenTradeChannel.getMuSigOffer().equals(contract.getOffer()));
                            checkArgument(muSigOpenTradeChannel.getMediator().orElseThrow().equals(mediatorFromContract.orElseThrow()));
                        } catch (IllegalArgumentException e) {
                            log.error("Validation of channel properties and contract properties failed. " +
                                    "channel={}; contract={}", channel, contract, e);
                            return false;
                        }
                    }
                    return true;
                })
                .map(mediationCase -> {
                    MediationRequest mediationRequest = mediationCase.getMediationRequest();
                    UserIdentity myUserIdentity = mediatorService.findMyMediatorUserIdentity(getMediator(mediationRequest.getContract())).orElseThrow();
                    PrivateGroupChatChannel<?> channel = findOrCreateChannel(mediationRequest, myUserIdentity);
                    return new MediationCaseListItem(serviceProvider, mediationCase, channel);
                })
                .to(mediatorService.getMediationCases());

        bisqEasyOpenTradeSelectedChannelPin = bisqEasyOpenTradeSelectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
        muSigOpenTradeSelectedChannelPin = muSigOpenTradeSelectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        searchPredicatePin = EasyBind.subscribe(model.getSearchPredicate(), searchPredicate -> updatePredicate());
        closedCasesPredicatePin = EasyBind.subscribe(model.getClosedCasesPredicate(), closedCasesPredicate -> updatePredicate());
        maybeSelectFirst();
        updateEmpytState();

        model.getListItems().addListener(itemListener);
    }

    @Override
    public void onDeactivate() {
        doCloseChatWindow();

        model.getListItems().removeListener(itemListener);
        model.getListItems().onDeactivate();
        model.reset();

        mediationCaseListItemPin.unbind();
        bisqEasyOpenTradeSelectedChannelPin.unbind();
        muSigOpenTradeSelectedChannelPin.unbind();
        searchPredicatePin.unsubscribe();
        closedCasesPredicatePin.unsubscribe();
    }

    void onSelectItem(MediationCaseListItem item) {
        if (item == null) {
            bisqEasyOpenTradeSelectionService.selectChannel(null);
            muSigOpenTradeSelectionService.selectChannel(null);
        } else if (item.getChannel() instanceof BisqEasyOpenTradeChannel && !item.getChannel().equals(bisqEasyOpenTradeSelectionService.getSelectedChannel().get())) {
            muSigOpenTradeSelectionService.selectChannel(null);
            bisqEasyOpenTradeSelectionService.selectChannel(item.getChannel());
        } else if (item.getChannel() instanceof MuSigOpenTradeChannel && !item.getChannel().equals(muSigOpenTradeSelectionService.getSelectedChannel().get())) {
            bisqEasyOpenTradeSelectionService.selectChannel(null);
            muSigOpenTradeSelectionService.selectChannel(item.getChannel());
        }

//        if (item == null) {
//            selectionService.selectChannel(null);
//        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
//            selectionService.selectChannel(item.getChannel());
//        }
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
                if (model.getSelectedItem().get() != null) {
                    maybeSelectFirst();
                }
//                maybeSelectFirst();
                updateEmpytState();
            } else if (chatChannel instanceof BisqEasyOpenTradeChannel tradeChannel) {
                log.info("Selected channel changed to Bisq Easy Open Trade Channel");
                model.getListItems().stream()
                        .filter(item -> item.getChannel().getId().equals(tradeChannel.getId()))
                        .findAny()
                        .ifPresent(item -> {
                            model.getSelectedItem().set(item);
                            mediationCaseHeader.setMediationCaseListItem(item);
                            mediationCaseHeader.setShowClosedCases(model.getShowClosedCases().get());
                        });
            } else if (chatChannel instanceof MuSigOpenTradeChannel tradeChannel) {
                log.info("Selected channel changed to MuSig Open Trade Channel");
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
        updateEmpytState();
    }

    private void applyFilteredListPredicate(boolean showClosedCases) {
        if (showClosedCases) {
            model.getClosedCasesPredicate().set(item -> item.getMediationCase().getIsClosed().get());
        } else {
            model.getClosedCasesPredicate().set(item -> !item.getMediationCase().getIsClosed().get());
        }
    }

    private void updateEmpytState() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<MediationCaseListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            bisqEasyOpenTradeSelectionService.getSelectedChannel().set(null);
            muSigOpenTradeSelectionService.getSelectedChannel().set(null);
            mediationCaseHeader.setMediationCaseListItem(null);
        } else {
            mediationCaseHeader.setMediationCaseListItem(model.getSelectedItem().get());
        }
    }

    private void maybeSelectFirst() {
        UIThread.runOnNextRenderFrame(() -> {

            if (!model.getListItems().getFilteredList().isEmpty()) {
                var channel = model.getListItems().getSortedList().getFirst().getChannel();
                if (channel instanceof BisqEasyOpenTradeChannel) {
                    bisqEasyOpenTradeSelectionService.selectChannel(channel);
                } else if (channel instanceof MuSigOpenTradeChannel) {
                    muSigOpenTradeSelectionService.selectChannel(channel);
                }
            }
        });
    }

    private PrivateGroupChatChannel<?> findOrCreateChannel(MediationRequest mediationRequest,
                                                           UserIdentity myUserIdentity) {
        Contract<?> contract = mediationRequest.getContract();
        if (contract instanceof BisqEasyContract bisqEasyContract) {
            return bisqEasyOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    mediationRequest.getTradeId(),
                    bisqEasyContract.getOffer(),
                    myUserIdentity,
                    mediationRequest.getRequester(),
                    mediationRequest.getPeer());
        } else if (contract instanceof MuSigContract muSigContract) {
            return muSigOpenTradeChannelService.mediatorFindOrCreatesChannel(
                    mediationRequest.getTradeId(),
                    myUserIdentity,
                    mediationRequest.getRequester(),
                    mediationRequest.getPeer());
        } else {
            throw new IllegalArgumentException("Unsupported contract type: " + contract.getClass());
        }
    }

    private Optional<UserProfile> getMediator(Contract<?> contract) {
        if (contract instanceof BisqEasyContract bisqEasyContract) {
            return bisqEasyContract.getMediator();
        } else if (contract instanceof MuSigContract muSigContract) {
            return muSigContract.getMediator();
        } else {
            return Optional.empty();
        }
    }
}

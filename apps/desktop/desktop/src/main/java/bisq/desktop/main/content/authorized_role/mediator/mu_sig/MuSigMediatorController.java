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

import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeSelectionService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerController;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.user.identity.UserIdentityService;
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
import static bisq.i18n.Res.get;

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
    private Pin mediationCaseListItemPin;
    private Subscription selectedItemPin, searchPredicatePin, closedCasesPredicatePin, selectedItemChannelPin;

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
                    onSelectItem(model.getListItems().getFilteredList().getFirst());
                }
            });
        };
    }

    @Override
    public void onActivate() {
        applyFilteredListPredicate(model.getShowClosedCases().get());

        mediationCaseListItemPin = FxBindings.<MuSigMediationCase, MuSigMediationCaseListItem>bind(model.getListItems())
                .map(mediationCase -> {
                    MuSigMediationRequest muSigMediationRequest = mediationCase.getMuSigMediationRequest();
                    Optional<MuSigOpenTradeChannel> channel = mediationCase.hasMediatorLeftChat()
                            ? Optional.empty()
                            : muSigOpenTradeChannelService.findChannelByTradeId(muSigMediationRequest.getTradeId());
                    return new MuSigMediationCaseListItem(serviceProvider, mediationCase, channel);
                })
                .to(muSigMediatorService.getMediationCases());

        selectedItemPin = EasyBind.subscribe(model.getSelectedItem(), this::selectedItemChanged);

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
        selectedItemPin.unsubscribe();
        searchPredicatePin.unsubscribe();
        closedCasesPredicatePin.unsubscribe();

        clearSelectedItemChannelPin();
    }

    void onSelectItem(MuSigMediationCaseListItem item) {
        model.getSelectedItem().set(item);
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

    private void selectedItemChanged(MuSigMediationCaseListItem item) {
        muSigMediationCaseHeader.setMediationCaseListItem(item);
        clearSelectedItemChannelPin();
        if (item == null) {
            model.getChatAvailable().set(false);
            model.getChatUnavailableTitle().set(null);
            model.getChatUnavailableDescription().set(null);
            selectionService.selectChannel(null);
            maybeSelectFirst();
            updateEmptyState();
        } else {
            selectedItemChannelPin = EasyBind.subscribe(item.channelProperty(), maybeChannel -> {
                applyChatState(item, maybeChannel);
                maybeChannel.ifPresentOrElse(
                        channel -> {
                            if (!channel.equals(selectionService.getSelectedChannel().get())) {
                                selectionService.selectChannel(channel);
                            }
                        },
                        () -> {
                            if (selectionService.getSelectedChannel().get() != null) {
                                selectionService.selectChannel(null);
                            }
                        });
            });
        }
    }

    private void applyChatState(MuSigMediationCaseListItem item, Optional<MuSigOpenTradeChannel> maybeChannel) {
        if (maybeChannel.isPresent()) {
            model.getChatAvailable().set(true);
            model.getChatUnavailableTitle().set(null);
            model.getChatUnavailableDescription().set(null);
        } else if (item.getMuSigMediationCase().hasMediatorLeftChat()) {
            model.getChatAvailable().set(false);
            model.getChatUnavailableTitle().set(get("authorizedRole.disputeActor.chat.unavailable.left.title"));
            model.getChatUnavailableDescription().set(get("authorizedRole.mediator.chat.unavailable.left.description"));
        } else {
            model.getChatAvailable().set(false);
            model.getChatUnavailableTitle().set(get("authorizedRole.disputeActor.chat.unavailable.pending.title"));
            model.getChatUnavailableDescription().set(get("authorizedRole.mediator.chat.unavailable.pending.description"));
        }
    }

    private void applyShowClosedCasesChange() {
        model.getSelectedItem().set(null);
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
            model.getClosedCasesPredicate().set(item -> item.getMuSigMediationCase().getMediationCaseState() == MediationCaseState.CLOSED);
        } else {
            model.getClosedCasesPredicate().set(item -> item.getMuSigMediationCase().getMediationCaseState() != MediationCaseState.CLOSED);
        }
    }

    private void updateEmptyState() {
        // The sortedList is already sorted by date (triggered by the usage of the dateColumn)
        SortedList<MuSigMediationCaseListItem> sortedList = model.getListItems().getSortedList();
        boolean isEmpty = sortedList.isEmpty();
        model.getNoOpenCases().set(isEmpty);
        if (isEmpty) {
            model.getSelectedItem().set(null);
        }
    }

    private void maybeSelectFirst() {
        UIThread.runOnNextRenderFrame(() -> {
            if (!model.getListItems().getFilteredList().isEmpty()) {
                onSelectItem(model.getListItems().getSortedList().getFirst());
            }
        });
    }

    private void clearSelectedItemChannelPin() {
        if (selectedItemChannelPin != null) {
            selectedItemChannelPin.unsubscribe();
            selectedItemChannelPin = null;
        }
    }
}

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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat;

import bisq.account.bisqeasy.BisqEasyPaymentAccount;
import bisq.chat.channel.ChatChannelDomain;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
public class BisqEasyChatModel extends ChatModel {
    private final BooleanProperty offerOnly = new SimpleBooleanProperty();
    private final BooleanProperty offerOnlyVisible = new SimpleBooleanProperty();
    private final BooleanProperty createOfferButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty sendBtcAddressButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty sendPaymentAccountButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty openDisputeDisabled = new SimpleBooleanProperty();
    private final BooleanProperty isTradeChannelVisible = new SimpleBooleanProperty();
    private final BooleanProperty paymentAccountSelectionVisible = new SimpleBooleanProperty();
    private final ObservableList<BisqEasyPaymentAccount> paymentAccounts = FXCollections.observableArrayList();
    private final ObjectProperty<BisqEasyPaymentAccount> selectedAccount = new SimpleObjectProperty<>();

    public BisqEasyChatModel(ChatChannelDomain chatChannelDomain) {
        super(chatChannelDomain);
    }

    @Override
    public NavigationTarget getDefaultNavigationTarget() {
        return NavigationTarget.NONE;
    }

    @Nullable
    public BisqEasyPaymentAccount getSelectedAccount() {
        return selectedAccount.get();
    }

    public ObjectProperty<BisqEasyPaymentAccount> selectedAccountProperty() {
        return selectedAccount;
    }

    public void setSelectedAccount(BisqEasyPaymentAccount selectedAccount) {
        this.selectedAccount.set(selectedAccount);
    }

}

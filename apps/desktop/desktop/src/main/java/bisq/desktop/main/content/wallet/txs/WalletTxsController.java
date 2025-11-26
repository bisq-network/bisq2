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

package bisq.desktop.main.content.wallet.txs;

import bisq.common.observable.Pin;
import bisq.common.proto.ProtobufUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.wallet.WalletTxListItem;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.wallet.WalletService;
import bisq.wallet.vo.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Predicate;

@Slf4j
public class WalletTxsController implements Controller {
    @Getter
    private final WalletTxsView view;
    private final WalletTxsModel model;
    private final WalletService walletService;
    private final SettingsService settingsService;
    private Pin transactionsPin;
    private Subscription selectedFilterPin, searchTextPin;

    public WalletTxsController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService().orElseThrow();
        settingsService = serviceProvider.getSettingsService();

        model = new WalletTxsModel();
        view = new WalletTxsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSearchText().set("");

        transactionsPin = FxBindings.<Transaction, WalletTxListItem>bind(model.getListItems())
                .map(WalletTxListItem::new)
                .to(walletService.getTransactions());

        TxsFilter persistedFilter = settingsService.getCookie().asString(CookieKey.WALLET_TXS_FILTER).map(name ->
                ProtobufUtils.enumFromProto(TxsFilter.class, name, TxsFilter.ALL)).orElse(TxsFilter.ALL);
        model.getSelectedFilter().set(persistedFilter);
        selectedFilterPin = EasyBind.subscribe(model.getSelectedFilter(), filter -> {
           if (filter != null) {
               model.setFilterPredicate(getFilterPredicate(filter));
               settingsService.setCookie(CookieKey.WALLET_TXS_FILTER, filter.name());
               updateFilteredListItems();
           }
        });

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.trim().isEmpty()) {
                model.setSearchTextPredicate(item -> true);
            } else {
                String search = searchText.trim().toLowerCase();
                model.setSearchTextPredicate(item ->
                        item != null &&
                                (item.getTrade().toLowerCase().contains(search)
                                        || item.getDestinationAddress().toLowerCase().contains(search)
                                        || item.getTxId().toLowerCase().contains(search)
                                        || item.getAmountAsString().toLowerCase().contains(search))
                );
            }
            updateFilteredListItems();
        });

        walletService.requestTransactions();
    }

    @Override
    public void onDeactivate() {
        transactionsPin.unbind();
        selectedFilterPin.unsubscribe();
        searchTextPin.unsubscribe();
    }

    private void updateFilteredListItems() {
        model.getFilteredListItems().setPredicate(null);
        model.getFilteredListItems().setPredicate(model.getListItemsPredicate());
    }

    private Predicate<WalletTxListItem> getFilterPredicate(TxsFilter filter) {
        return switch (filter) {
            case ALL -> item -> true;
            case LOCKED_FUNDS ->  item -> item.getTxUsage() == TxUsage.LOCKED;
            case RESERVED_FUNDS -> item -> item.getTxUsage() == TxUsage.RESERVED;
        };
    }
}

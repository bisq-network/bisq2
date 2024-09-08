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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.user.profile.UserProfile;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.Optional;

public class TradeCompletedTable extends VBox {

    public TradeCompletedTable(UserProfile userProfile, Direction direction, String btcAmount, String fiatAmount,
                               String fiatCurrency, String paymentMethodUsed, String tradeIdUsed, String tradeDate,
                               String tradePriceUsed, String tradePriceSymbolUsed, String miningFeeUsed, Optional<String> txId) {
        Label title = new Label(Res.get("bisqEasy.tradeCompleted.title"));

        // Header
        GridPane headerGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        headerGridPane.setMouseTransparent(true);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(headerGridPane, 5);

        int rowTitle = 0;
        int rowValue = 1;

        int col = 0;
        Label tradeWith = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeWith").toUpperCase());
        UserProfileDisplay tradeWithValue = new UserProfileDisplay(userProfile);
        headerGridPane.add(tradeWith, col, rowTitle);
        headerGridPane.add(tradeWithValue, col, rowValue);

        ++col;
        Label myDirection = direction == Direction.BUY
                ? new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.buyer").toUpperCase())
                : new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.seller").toUpperCase());
        Label myDirectionValue = new Label(btcAmount);
        Label directionBtc = new Label(Res.get("bisqEasy.tradeCompleted.header.myDirection.btc").toUpperCase());
        HBox btcBox = new HBox(5, myDirectionValue, directionBtc);
        headerGridPane.add(myDirection, col, rowTitle);
        headerGridPane.add(btcBox, col, rowValue);

        ++col;
        Label myOutcome = direction == Direction.BUY
                ? new Label(Res.get("bisqEasy.tradeCompleted.header.myOutcome.buyer").toUpperCase())
                : new Label(Res.get("bisqEasy.tradeCompleted.header.myOutcome.seller").toUpperCase());
        Label myOutcomeValue = new Label(fiatAmount);
        Label fiat = new Label(fiatCurrency.toUpperCase());
        HBox fiatBox = new HBox(5, myOutcomeValue, fiat);
        headerGridPane.add(myOutcome, col, rowTitle);
        headerGridPane.add(fiatBox, col, rowValue);

        ++col;
        Label paymentMethod = new Label(Res.get("bisqEasy.tradeCompleted.header.paymentMethod").toUpperCase());
        Label paymentMethodValue = new Label(paymentMethodUsed);
        headerGridPane.add(paymentMethod, col, rowTitle);
        headerGridPane.add(paymentMethodValue, col, rowValue);

        ++col;
        Label tradeId = new Label(Res.get("bisqEasy.tradeCompleted.header.tradeId").toUpperCase());
        Label tradeIdValue = new Label(tradeIdUsed);
        headerGridPane.add(tradeId, col, rowTitle);
        headerGridPane.add(tradeIdValue, col, rowValue);

        // Body
        GridPane bodyGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        ColumnConstraints titleCol = new ColumnConstraints();
        titleCol.setPercentWidth(0.25);
        bodyGridPane.getColumnConstraints().add(titleCol);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setPercentWidth(0.75);
        bodyGridPane.getColumnConstraints().add(valueCol);

        int colTitle = 0;
        int colValue = 1;
        int row = 0;
        Label date = new Label(Res.get("bisqEasy.tradeCompleted.body.date"));
        Label dateValue = new Label(tradeDate);
        bodyGridPane.add(date, colTitle, row);
        bodyGridPane.add(dateValue, colValue, row);

        ++row;
        Label tradePrice = new Label(Res.get("bisqEasy.tradeCompleted.body.tradePrice"));
        Label tradePriceValue = new Label(tradePriceUsed);
        Label tradePriceSymbol = new Label(tradePriceSymbolUsed.toUpperCase());
        HBox tradePriceBox = new HBox(5, tradePriceValue, tradePriceSymbol);
        bodyGridPane.add(tradePrice, colTitle, row);
        bodyGridPane.add(tradePriceBox, colValue, row);

        ++row;
        Label tradeFee = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee"));
        Label tradeFeeValue = new Label(Res.get("bisqEasy.tradeCompleted.body.tradeFee.value"));
        bodyGridPane.add(tradeFee, colTitle, row);
        bodyGridPane.add(tradeFeeValue, colValue, row);

        ++row;
        Label miningFee = new Label(Res.get("bisqEasy.tradeCompleted.body.miningFee"));
        Label miningFeeValue = new Label(miningFeeUsed);
        Label miningFeeBtc = new Label(Res.get("bisqEasy.tradeCompleted.body.miningFee.btc").toUpperCase());
        HBox miningFeeBox = new HBox(5, miningFeeValue, miningFeeBtc);
        bodyGridPane.add(miningFee, colTitle, row);
        bodyGridPane.add(direction == Direction.BUY ? new Label(Res.get("bisqEasy.tradeCompleted.body.miningFee.buyer.value")) : miningFeeBox,
                colValue, row);

        if (txId.isPresent()) {
            ++row;
            Label txIdTitle = new Label(Res.get("bisqEasy.tradeCompleted.body.txId"));
            Label txIdValue = new Label(txId.get());
            // TODO: add link & copy
            bodyGridPane.add(txIdTitle, colTitle, row);
            bodyGridPane.add(txIdValue, colValue, row);
        }

        Region line1 = getLine();
        Region line2 = getLine();
        Region line3 = getLine();
        getChildren().addAll(title, line1, headerGridPane, line2, bodyGridPane, line3);
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}

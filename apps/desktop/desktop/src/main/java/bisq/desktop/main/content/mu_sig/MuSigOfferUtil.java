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

package bisq.desktop.main.content.mu_sig;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.desktop.main.content.components.UserProfileDisplay;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigOfferUtil {
    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileDisplay = new UserProfileDisplay(item.getMakerUserProfile(), true, true);
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);
                } else {
                    if (userProfileDisplay != null) {
                        userProfileDisplay.dispose();
                        userProfileDisplay = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getPaymentCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(5);
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    for (FiatPaymentMethod paymentMethod : item.getPaymentMethods()) {
                        Node icon = ImageUtil.getImageViewById(paymentMethod.getPaymentRailName());
                        Optional<Double> opacity = Optional.ofNullable(item.getAccountAvailableByPaymentMethod().get(paymentMethod))
                                .map(isAccountAvailable -> isAccountAvailable ? 1 : 0.2);
                        if (opacity.isPresent()) {
                            icon.setOpacity(opacity.get());
                        } else {
                            log.error("Unexpected state: accountAvailableByPaymentMethod={}", item.getAccountAvailableByPaymentMethod());
                        }
                        hbox.getChildren().add(icon);
                    }
                    tooltip.setText(item.getPaymentMethodsAsString());
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(7);
            private final BisqTooltip tooltip = new BisqTooltip();
            private final Label priceIconLabel = new Label();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();

                    Pair<String, String> pricePair = item.getPricePair();
                    Label price = new Label(pricePair.getFirst());
                    setupPriceIconLabel(item.isHasFixPrice());
                    Label pricePercentage = new Label(pricePair.getSecond());
                    hbox.getChildren().addAll(price, priceIconLabel, pricePercentage);

                    tooltip.setText(item.getPriceTooltip());
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }

            private void setupPriceIconLabel(boolean hasFixPrice) {
                String priceIconId = hasFixPrice ? "lock-icon-grey" : "chart-icon-grey";
                priceIconLabel.setGraphic(ImageUtil.getImageViewById(priceIconId));
                if (hasFixPrice) {
                    HBox.setMargin(priceIconLabel, new Insets(0));
                } else {
                    HBox.setMargin(priceIconLabel, new Insets(-2, 0, 2, 0));
                }
            }
        };
    }

    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getBaseAmountCellFactory(boolean showSymbol) {
        return column -> new TableCell<>() {
            @SuppressWarnings("UnnecessaryUnicodeEscape")
            private static final String DASH_SYMBOL = "\u2013"; // Unicode for "–"

            private final BitcoinAmountDisplay bitcoinMinAmountDisplay = new BitcoinAmountDisplay("0", false);
            private final BitcoinAmountDisplay bitcoinFixedOrMaxAmountDisplay = new BitcoinAmountDisplay("0", showSymbol);
            private final HBox hbox = new HBox(5);
            private final Label dashLabel = new Label(DASH_SYMBOL);

            {
                configureBitcoinAmountDisplay(bitcoinMinAmountDisplay);
                configureBitcoinAmountDisplay(bitcoinFixedOrMaxAmountDisplay);
                hbox.setAlignment(Pos.CENTER_LEFT);
                dashLabel.setAlignment(Pos.CENTER);
                dashLabel.setStyle("-fx-text-fill: -fx-mid-text-color;");
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
//                    if (item.isBaseAmountBtc()) {
//                        if (item.isHasAmountRange()) {
//                            Pair<String, String> minAndMaxBaseAmount = item.getMinAndMaxBaseAmountPair();
//                            bitcoinMinAmountDisplay.setBtcAmount(minAndMaxBaseAmount.getFirst());
//                            hbox.getChildren().add(bitcoinMinAmountDisplay);
//                            hbox.getChildren().add(dashLabel);
//                            bitcoinFixedOrMaxAmountDisplay.setBtcAmount(minAndMaxBaseAmount.getSecond());
//                        } else {
//                            bitcoinFixedOrMaxAmountDisplay.setBtcAmount(item.getBaseAmountAsString());
//                        }
//                        hbox.getChildren().add(bitcoinFixedOrMaxAmountDisplay);
//                        setGraphic(hbox);
//                    } else {
                        setGraphic(new Label(showSymbol
                                ? item.getBaseAmountWithSymbol()
                                : item.getBaseAmountAsString()));
//                    }
                } else {
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }

            private void configureBitcoinAmountDisplay(BitcoinAmountDisplay bitcoinAmountDisplay) {
                bitcoinAmountDisplay.getSignificantDigits().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.getLeadingZeros().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.getIntegerPart().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.setTranslateY(5);
                bitcoinAmountDisplay.applySmallCompactConfig();
                bitcoinAmountDisplay.setAlignment(Pos.CENTER_LEFT);
            }
        };
    }
}

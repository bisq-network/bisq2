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

package bisq.desktop.main.content.components;

import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.market.Market;
import bisq.desktop.common.utils.ImageUtil;
import bisq.security.DigestUtil;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MarketImageComposition {
    private static final List<String> CRYPTO_MARKETS_WITH_LOGO = List.of("bsq", "btc", "xmr");
    private static final Set<String> FIAT_MARKETS_WITH_LOGO = Stream.of("aed", "afn", "all", "amd", "aoa", "ars", "aud",
                    "awg", "azn", "bam", "bbd", "bdt", "bgn", "bhd", "bif", "bmd", "bnd", "bob", "brl", "bsd", "btn", "bwp",
                    "byn", "bzd", "cad", "chf", "clp", "cny", "cop", "crc", "cup", "cve", "czk", "djf", "dkk", "dop", "dzd",
                    "egp", "ern", "etb", "eur", "fjd", "fkp", "gbp", "gel", "ghs", "gip", "gmd", "gnf", "gtq", "gyd", "hkd",
                    "hnl", "htg", "huf", "idr", "ils", "inr", "iqd", "irr", "isk", "jmd", "jod", "jpy", "kes", "kgs", "khr",
                    "kmf", "kpw", "krw", "kwd", "kyd", "kzt", "lak", "lbp", "lkr", "lrd", "lsl", "lyd", "mad", "mdl", "mga",
                    "mmk", "mnt", "mop", "mru", "mur", "mvr", "mwk", "mxn", "myr", "mzn", "nad", "ngn", "nio", "nok", "npr",
                    "nzd", "omr", "pab", "pen", "pgk", "php", "pkr", "pln", "pyg", "qar", "ron", "rsd", "rub", "rwf", "sar",
                    "sbd", "scr", "sdg", "sek", "sgd", "sle", "sos", "srd", "ssp", "stn", "svc", "syp", "szl", "thb", "tjs",
                    "tmt", "tnd", "top", "try", "ttd", "twd", "tzs", "uah", "ugx", "usd", "uyu", "uzs", "ves", "vnd", "vuv",
                    "wst", "xaf", "yer", "zar", "zmw", "zwl")
            .collect(Collectors.toUnmodifiableSet());
    private static final Map<String, StackPane> MARKET_IMAGE_CACHE = new HashMap<>();

    public static StackPane getMarketIcons(Market market) {
        return getMarketIcons(market, Optional.empty());
    }

    public static StackPane getMarketIcons(Market market, Map<String, StackPane> dedicatedCache) {
        return getMarketIcons(market, Optional.of(dedicatedCache));
    }

    private static StackPane getMarketIcons(Market market, Optional<Map<String, StackPane>> dedicatedCache) {
        String baseCurrencyCode = market.getBaseCurrencyCode().toLowerCase(Locale.ROOT);
        String quoteCurrencyCode = market.getQuoteCurrencyCode().toLowerCase(Locale.ROOT);
        String key = baseCurrencyCode + "." + quoteCurrencyCode;

        if (dedicatedCache.isPresent()) {
            if (dedicatedCache.get().containsKey(key)) {
                return dedicatedCache.get().get(key);
            }
        } else {
            if (MARKET_IMAGE_CACHE.containsKey(key)) {
                return MARKET_IMAGE_CACHE.get(key);
            }
        }

        StackPane pane = getMarketPairIcons(baseCurrencyCode, quoteCurrencyCode);

        if (dedicatedCache.isPresent()) {
            dedicatedCache.get().put(key, pane);
        } else {
            MARKET_IMAGE_CACHE.put(key, pane);
        }
        return pane;
    }

    public static StackPane getMarketPairIcons(String baseCurrencyCode, String quoteCurrencyCode) {
        StackPane pane = new StackPane();
        pane.setPrefWidth(61);
        Stream<String> stream = Stream.of(baseCurrencyCode, quoteCurrencyCode);
        stream.forEach(code -> {
            if (quoteCurrencyCode.equals(code)) {
                double radius = 18;
                Circle circle = new Circle(radius);
                circle.getStyleClass().add("quote-currency-market-logo");
                StackPane.setAlignment(circle, Pos.CENTER);

                Node node = createMarketLogo(code);
                StackPane.setAlignment(node, Pos.CENTER);

                StackPane quoteLogo = new StackPane();
                quoteLogo.setMaxWidth(radius * 2);
                quoteLogo.getChildren().addAll(circle, node);
                StackPane.setAlignment(quoteLogo, Pos.CENTER_RIGHT);
                pane.getChildren().add(quoteLogo);
            } else {
                Node node = createMarketLogo(code);
                StackPane.setAlignment(node, Pos.CENTER_LEFT);
                pane.getChildren().add(node);
            }
        });
        return pane;
    }

    public static StackPane getCurrencyIcon(String code) {
        boolean isRetina = ImageUtil.isRetina();

        StackPane pane = new StackPane();
        pane.setPrefHeight(isRetina ? 34 : 17);
        pane.setPrefWidth(isRetina ? 30 : 15);

        String symbolOrCode = FiatCurrencyRepository.getSymbol(code);
        if (symbolOrCode.length() > 3) {
            symbolOrCode = code.toUpperCase().substring(0, 3);
        }
        Label label = new Label(symbolOrCode);

        if (symbolOrCode.length() == 1) {
            label.getStyleClass().setAll("fiat-symbol");
        } else if (symbolOrCode.length() == 2) {
            label.getStyleClass().setAll("fiat-symbol-2");
        } else {
            label.getStyleClass().setAll("fiat-code");
        }

        Circle circle = new Circle(12);
        circle.setSmooth(true);
        pane.getChildren().add(circle);
        if (code.equalsIgnoreCase("EUR")) {
            circle.setFill(Paint.valueOf("#0F0FD9"));
        } else if (code.equalsIgnoreCase("USD")) {
            circle.setFill(Paint.valueOf("#3D8603"));
        } else {
            circle.setFill(Paint.valueOf("#FF0000"));
            circle.setEffect(createColorAdjust(symbolOrCode));
        }

        StackPane.setAlignment(label, Pos.CENTER);
        pane.getChildren().add(label);
        //Move entire pane node horizontally
        pane.setTranslateX(6.5);

        return pane;
    }

    public static Node createMarketLogo(String marketCode) {
        return createLogo(marketCode, "market-%s");
    }

    public static Node createMarketMenuLogo(String marketCode) {
        return createLogo(marketCode, "%s-menu");
    }

    private static Node createLogo(String marketCode, String iconIdFormat) {
        String market = marketCode.toLowerCase(Locale.ROOT);
        String iconId = String.format(iconIdFormat, market);
        return FIAT_MARKETS_WITH_LOGO.contains(market) || CRYPTO_MARKETS_WITH_LOGO.contains(market)
                ? ImageUtil.getImageViewById(iconId)
                : createMarketLogoPlaceholder(marketCode);
    }

    private static Label createMarketLogoPlaceholder(String marketCode) {
        Circle circle = new Circle(16);
        circle.setFill(Paint.valueOf("#FF0000"));
        circle.setEffect(createColorAdjust(marketCode));

        Text text = new Text(marketCode.substring(0, Math.min(3, marketCode.length())).toUpperCase());
        Label label = new Label(text.getText(), new Circle(16, Color.TRANSPARENT));
        label.getStyleClass().add("fiat-code");
        label.setAlignment(Pos.CENTER);
        label.setGraphic(circle);
        label.setContentDisplay(ContentDisplay.CENTER);
        return label;
    }

    private static ColorAdjust createColorAdjust(String code) {
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setSaturation(-0.25);
        colorAdjust.setBrightness(-0.5);
        double hue = new BigInteger(DigestUtil.sha256(code.getBytes())).mod(BigInteger.valueOf(100000)).doubleValue() / 100000;
        colorAdjust.setHue(hue);
        return colorAdjust;
    }
}

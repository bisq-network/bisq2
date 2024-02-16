package bisq.desktop.main.content.components;

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.security.DigestUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MarketImageComposition {
    private static final List<String> MARKETS_WITH_IMAGE = List.of("bsq", "btc", "eur", "usd", "xmr", "any-base", "any-quote");
    private static final Set<String> MARKETS_WITH_LOGOS = Stream.of("aed", "afn", "all", "amd", "aoa", "ars", "aud",
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

    public static Pair<StackPane, List<ImageView>> imageBoxForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        StackPane pane = new StackPane();

        pane.setPrefHeight(34);
        pane.setPrefWidth(30);

        Stream<String> stream = baseCurrencyCode.equals("btc")
                ? Stream.of(baseCurrencyCode, quoteCurrencyCode)
                : Stream.of(quoteCurrencyCode, baseCurrencyCode);

        List<ImageView> imageViews = stream.map(code -> {
            Pos alignment = quoteCurrencyCode.equals(code) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
            ImageView imageView = new ImageView();

          /*  ColorAdjust monochrome = new ColorAdjust();
            monochrome.setSaturation(-0.5);
            monochrome.setBrightness(-0.5);
            imageView.setEffect(monochrome);*/


            StackPane.setAlignment(imageView, alignment);
            pane.getChildren().add(imageView);

            if (MARKETS_WITH_IMAGE.contains(code)) {
                imageView.setId("market-" + code);
            } else {
                boolean isFiat = TradeCurrency.isFiat(code.toUpperCase());
                if (code.length() > 4) {
                    code = code.toUpperCase().substring(0, 4);
                }
                Label label = new Label(code.toUpperCase());
                StackPane.setAlignment(label, alignment);
                if (isFiat) {
                    imageView.setId("market-fiat");
                    label.setPadding(new Insets(0, 1, 0, 0));
                    label.getStyleClass().setAll("market-fiat-label");
                    // When used in a list, the list style overwrites icon label style, so we need to 
                    // use !important. Using style class or id did not work ;-(
                    label.setStyle("-fx-text-fill: -fx-dark-text-color !important;");
                } else {
                    imageView.setId("market-crypto");
                    label.setPadding(new Insets(0, 0, 0, 2));
                    label.getStyleClass().setAll("market-crypto-label");
                    // list style overwrites icon label style, so we need to use !important
                    label.setStyle("-fx-text-fill: -fx-light-text-color !important;");
                }
                if (code.length() > 3) {
                    label.setStyle("-fx-font-size: 6");
                    if (isFiat) {
                        label.setPadding(new Insets(0, 0, 0, 0));
                    } else {
                        label.setPadding(new Insets(0, 0, 0, 1));
                    }
                }
                pane.getChildren().add(label);
            }
            return imageView;
        }).collect(Collectors.toList());

        return new Pair<>(pane, imageViews);
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
        String market = marketCode.toLowerCase();
        String iconId = String.format("market-%s", market);
        return MARKETS_WITH_LOGOS.contains(market)
                ? ImageUtil.getImageViewById(iconId)
                : createMarketLogoPlaceholder(marketCode);
    }

    private static Label createMarketLogoPlaceholder(String marketCode) {
        Circle circle = new Circle(16);
        circle.setFill(Paint.valueOf("#FF0000"));
        circle.setEffect(createColorAdjust(marketCode));

        Text text = new Text(marketCode.substring(0, Math.min(3, marketCode.length())));
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

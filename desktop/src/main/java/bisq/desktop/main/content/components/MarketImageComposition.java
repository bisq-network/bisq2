package bisq.desktop.main.content.components;

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.security.DigestUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MarketImageComposition {
    private static final List<String> MARKETS_WITH_IMAGE = List.of("bsq", "btc", "eur", "usd", "xmr", "any-base", "any-quote");

    public static Pair<StackPane, List<ImageView>> imageBoxForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        StackPane pane = new StackPane();

        // TODO check on systems without high resolution
       /* boolean isRetina = ImageUtil.isRetina();
        pane.setPrefHeight(isRetina ? 34 : 17);
        pane.setPrefWidth(isRetina ? 30 : 15);*/

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

        Circle circle = new Circle(10);
        circle.setSmooth(true);
        pane.getChildren().add(circle);
        if (code.equalsIgnoreCase("EUR")) {
            circle.setFill(Paint.valueOf("#0F0FD9"));
        } else if (code.equalsIgnoreCase("USD")) {
            circle.setFill(Paint.valueOf("#3D8603"));
        } else {
            circle.setFill(Paint.valueOf("#FF0000"));
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setSaturation(-0.25);
            colorAdjust.setBrightness(-0.5);
            double hue = new BigInteger(DigestUtil.sha256(symbolOrCode.getBytes())).mod(BigInteger.valueOf(100000)).doubleValue() / 100000;
            colorAdjust.setHue(hue);
            circle.setEffect(colorAdjust);
        }

        StackPane.setAlignment(label, Pos.CENTER);
        pane.getChildren().add(label);
        //Move entire pane node horizontally
        pane.setTranslateX(6.5);

        return pane;
    }
}

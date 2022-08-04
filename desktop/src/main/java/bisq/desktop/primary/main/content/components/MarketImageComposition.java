package bisq.desktop.primary.main.content.components;

import bisq.common.currency.TradeCurrency;
import bisq.desktop.common.utils.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class MarketImageComposition {
    private static final List<String> MARKETS_WITH_IMAGE = List.of("bsq", "btc", "eur", "usd", "xmr", "any-base", "any-quote");

    public static StackPane imageBoxForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        boolean isRetina = ImageUtil.isRetina();

        StackPane pane = new StackPane();
        pane.setPrefHeight(isRetina ? 34 : 17);
        pane.setPrefWidth(isRetina ? 30 : 15);

        Stream<String> stream = baseCurrencyCode.equals("btc")
                ? Stream.of(baseCurrencyCode, quoteCurrencyCode)
                : Stream.of(quoteCurrencyCode, baseCurrencyCode);

        stream.forEach(code -> {
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
                    label.setStyle("-fx-text-fill: -bisq-black !important;");
                } else {
                    imageView.setId("market-crypto");
                    label.setPadding(new Insets(0, 0, 0, 2));
                    label.getStyleClass().setAll("market-crypto-label");
                    // list style overwrites icon label style, so we need to use !important
                    label.setStyle("-fx-text-fill: -bisq-white !important;");
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
        });

        return pane;
    }
}

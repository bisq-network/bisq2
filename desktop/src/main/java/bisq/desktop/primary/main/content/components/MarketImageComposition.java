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
    private static final List<String> MARKETS_WITH_IMAGE = List.of("bsq", "btc", "eur", "ltc", "usd", "xmr");
    
    public static StackPane imageBoxForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        boolean isRetina = ImageUtil.isRetina();
        
        StackPane pane = new StackPane();
        pane.setPrefHeight(isRetina ? 34 : 17);
        pane.setPrefWidth(isRetina ? 60 : 30);

        Stream<String> stream = baseCurrencyCode.equals("btc")
                ? Stream.of(quoteCurrencyCode, baseCurrencyCode)
                : Stream.of(baseCurrencyCode, quoteCurrencyCode);

        stream.forEach(code -> {
            Pos alignment = quoteCurrencyCode.equals(code) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
            ImageView imageView = new ImageView();
            StackPane.setAlignment(imageView, alignment);
            pane.getChildren().add(imageView);
            if (MARKETS_WITH_IMAGE.contains(code)) {
                imageView.setId("market-" + code);
            } else {
                boolean isFiat = TradeCurrency.isFiat(code.toUpperCase());
                imageView.setId(isFiat ? "market-fiat" : "market-crypto");
                Label label = new Label(code.toUpperCase());
                label.getStyleClass().setAll(isFiat ? "market-fiat-label" : "market-crypto-label");
                StackPane.setAlignment(label, alignment);
                pane.getChildren().add(label);
            }
        });
        
        return pane;
    }
}

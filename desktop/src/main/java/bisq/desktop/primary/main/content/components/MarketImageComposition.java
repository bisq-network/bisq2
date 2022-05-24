package bisq.desktop.primary.main.content.components;

import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public class MarketImageComposition {
    // todo Better use normal composition instead of PixelWriter as rendered image does not has anti-alias and 
    // looks pixelated even with double scale
    public static ImageView imageViewForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        ImageView imageView = new ImageView();
        boolean isRetina = ImageUtil.isRetina();
        int size = isRetina ? 68 : 34;
        WritableImage image = new WritableImage(2 * size, size);
        PixelWriter writer = image.getPixelWriter();

        Stream<String> stream = baseCurrencyCode.equals("btc")
                ? Stream.of(quoteCurrencyCode, baseCurrencyCode)
                : Stream.of(baseCurrencyCode, quoteCurrencyCode);

        stream.forEach(code -> {
            String hiRes = isRetina ? "@2x" : "";
            Image part = ImageUtil.getImageByPath("images/markets/" + code + hiRes + ".png", size, size);
            if (part == null) {
                part = textIcon(code, quoteCurrencyCode.equals(code), size, isRetina);
            }
            PixelReader reader = part.getPixelReader();
            int offset = quoteCurrencyCode.equals(code) ? 26 : 0;
            offset = isRetina ? 2 * offset : offset;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    Color color = reader.getColor(i, j);
                    if (color.isOpaque()) {
                        writer.setColor(i + offset, j, color);
                    }
                }
            }
        });
        imageView.setImage(image);
        imageView.setFitWidth(34);
        imageView.setFitHeight(17);
        imageView.setSmooth(true);
        return imageView;
    }

    public static Image textIcon(String code, boolean alignRight, int size, boolean isRetina) {
        final Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.fillOval(0, 0, size, size);
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        int fontSize = code.length() > 3 ? 10 : 16;
        fontSize = isRetina ? 2 * fontSize : fontSize;
        gc.setFont(new Font(fontSize));

        int x = alignRight ? 8 : 4;
        x = isRetina ? 2 * x : x;
        int y = code.length() > 3 ? 20 : 24;
        y = isRetina ? 2 * y : y;
        int maxWidth = isRetina ? 48 : 24;
        gc.fillText(code.toUpperCase(), x, y, maxWidth);

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(sp, image);
        return image;
    }
}

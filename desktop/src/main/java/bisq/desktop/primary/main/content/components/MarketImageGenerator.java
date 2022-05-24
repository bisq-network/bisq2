package bisq.desktop.primary.main.content.components;

import bisq.desktop.common.utils.ImageUtil;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
public class MarketImageGenerator {
    public static Image imageForMarket(String baseCurrencyCode, String quoteCurrencyCode) {
        WritableImage image = new WritableImage(30, 17);
        PixelWriter writer = image.getPixelWriter();

        Stream<String> stream = baseCurrencyCode.equals("btc") 
                ? Stream.of(quoteCurrencyCode, baseCurrencyCode)
                : Stream.of(baseCurrencyCode, quoteCurrencyCode);
        
        stream.forEach(code -> {
            Image part = ImageUtil.getImageByPath("images/markets/" + code + ".png" , 17, 17);
            if (part == null) {
                part = textIcon(code, quoteCurrencyCode.equals(code));
            }
            PixelReader reader = part.getPixelReader();
            int offset = quoteCurrencyCode.equals(code) ? 13 : 0;
            for (int i = 0; i < 17; i++) {
                for (int j = 0; j < 17; j++) {
                    Color color = reader.getColor(i, j);
                    if (color.isOpaque()) {
                        writer.setColor(i + offset, j, color);
                    }
                }
            }
        });

        return image;
    }

    public static Image textIcon(String code, boolean alignRight) {
        final Canvas canvas = new Canvas(17,17);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);
        gc.fillOval(0, 0, 17, 17);
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        gc.setFont(new Font(code.length() > 3 ? 5 : 8));

        gc.fillText(code.toUpperCase(), alignRight ? 4 : 2, code.length() > 3 ? 10 : 12, 12);

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(17, 17);
        canvas.snapshot(sp, image);
        return image;
    }
}

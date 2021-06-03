package network.misq.jfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Add JVM Opts to IDE Launcher:
//  --module-path <path-to>/javafx-sdk-16/lib --add-modules=javafx.controls,javafx.graphics

public class ApplicationMain extends Application {
    private static final Logger log = LoggerFactory.getLogger(ApplicationMain.class);

    public static void main(String[] args) {
        log.info("ApplicationMain");
        launch(args);
    }

    public void start(Stage primaryStage) {
        Scene scene = new Scene(new Pane());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}

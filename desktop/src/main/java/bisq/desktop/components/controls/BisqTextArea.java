package bisq.desktop.components.controls;

import com.jfoenix.controls.JFXTextArea;
import javafx.scene.control.Skin;

public class BisqTextArea extends JFXTextArea {
    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextAreaSkinBisqStyle(this);
    }
}

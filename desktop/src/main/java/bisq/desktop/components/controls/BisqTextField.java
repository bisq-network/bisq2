package bisq.desktop.components.controls;

import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Skin;

public class BisqTextField extends JFXTextField {

    public BisqTextField(String value) {
        super(value);
    }

    public BisqTextField() {
        super();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextFieldSkinBisqStyle<>(this, 0);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    public void show() {
        setVisible(true);
        setManaged(true);
    }
}

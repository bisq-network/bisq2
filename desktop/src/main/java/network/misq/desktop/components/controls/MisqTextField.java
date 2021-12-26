package network.misq.desktop.components.controls;

import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Skin;

public class MisqTextField extends JFXTextField {

    public MisqTextField(String value) {
        super(value);
    }

    public MisqTextField() {
        super();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextFieldSkinMisqStyle<>(this, 0);
    }
}

package bisq.desktop.components.controls;

import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Insets;
import javafx.scene.control.Skin;

public class BisqTextField extends JFXTextField {

    public BisqTextField(String value) {
        super(value);
        setLabelFloat(true);
        createEnoughSpaceForPromptText();
    }

    public BisqTextField() {
        super();
        setLabelFloat(true);
        createEnoughSpaceForPromptText();
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

    private void createEnoughSpaceForPromptText() {
        setPadding(new Insets(20, 0, 0, 0));
    }
}

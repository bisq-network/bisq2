package bisq.desktop.components.controls.jfx;

import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.skins.JFXToggleButtonSkin;
import javafx.scene.control.Skin;

public class BisqToggleButton extends JFXToggleButton {
    public BisqToggleButton() {
        super();
    }

    public BisqToggleButton(String text) {
        super();
        setText(text);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new AutoTooltipSlideToggleButtonSkin(this);
    }

    private static class AutoTooltipSlideToggleButtonSkin extends JFXToggleButtonSkin {
        public AutoTooltipSlideToggleButtonSkin(JFXToggleButton toggleButton) {
            super(toggleButton);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            // showTooltipIfTruncated(this, getSkinnable());
        }
    }
}

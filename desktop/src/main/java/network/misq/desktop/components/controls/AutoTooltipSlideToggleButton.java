package network.misq.desktop.components.controls;

import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.skins.JFXToggleButtonSkin;
import javafx.scene.control.Skin;

import static network.misq.desktop.common.utils.TooltipUtil.showTooltipIfTruncated;

public class AutoTooltipSlideToggleButton extends JFXToggleButton {
    public AutoTooltipSlideToggleButton() {
        super();
    }

    public AutoTooltipSlideToggleButton(String label) {
        super();
        setText(label);
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
            showTooltipIfTruncated(this, getSkinnable());
        }
    }
}

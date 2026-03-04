package bisq.desktop.main.content.mu_sig.trade.pending.trade_details;

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.components.MuSigMediationCaseDetailsViewHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

class MuSigTradeDetailsViewHelper extends MuSigMediationCaseDetailsViewHelper {

    private static final double DESCRIPTION_LABEL_WIDTH = 180;

    public static Label getValueLabel(ValueLabelStyle style, String text) {
        Label label = getValueLabel(style);
        label.setText(text);
        return label;
    }

    public static Label getValueLabel(ValueLabelStyle style) {
        Label label = new Label();
        label.getStyleClass().addAll(style.textColor, style.textSize, "font-light");
        return label;
    }

    public static HBox getValueBox(Node... children) {
        HBox box = new HBox(5, children);
        box.setAlignment(Pos.BASELINE_LEFT);
        return box;
    }

    public static HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                          Node detailsNode,
                                                          List<BisqMenuItem> buttons) {
        descriptionLabel.setMaxWidth(DESCRIPTION_LABEL_WIDTH);
        descriptionLabel.setMinWidth(DESCRIPTION_LABEL_WIDTH);
        descriptionLabel.setPrefWidth(DESCRIPTION_LABEL_WIDTH);

        HBox hBox = new HBox(descriptionLabel, detailsNode);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (!buttons.isEmpty()) {
            HBox hBoxButtons = new HBox(5);
            hBoxButtons.getChildren().addAll(buttons);
            buttons.forEach(button -> button.useIconOnly(17));
            HBox.setMargin(hBoxButtons, new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), hBoxButtons);
        }
        return hBox;
    }

    //
    // Style enums
    //

    public enum ValueLabelStyle {

        NORMAL("normal-text", "text-fill-white"),
        SMALL("small-text", "text-fill-white"),
        DIMMED("normal-text", "text-fill-grey-dimmed"),
        SMALL_DIMMED("small-text", "text-fill-grey-dimmed");

        private final String textColor, textSize;

        ValueLabelStyle(String textSize, String textColor) {
            this.textColor = textColor;
            this.textSize = textSize;
        }
    }
}
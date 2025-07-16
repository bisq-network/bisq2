package bisq.desktop.main.content.user.fiat_accounts.create.options;

import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * STUB IMPLEMENTATION - Options step view
 */

public class PaymentOptionsView extends View<VBox, PaymentOptionsModel, PaymentOptionsController> {

    public PaymentOptionsView(PaymentOptionsModel model, PaymentOptionsController controller) {
        super(new VBox(), model, controller);

        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label placeholder = new Label("Options configuration will be implemented here");
        placeholder.getStyleClass().add("bisq-text-3");

        root.getChildren().add(placeholder);
    }

    @Override
    protected void onViewAttached() {
        // Stub implementation
    }

    @Override
    protected void onViewDetached() {
        // Stub implementation
    }
}

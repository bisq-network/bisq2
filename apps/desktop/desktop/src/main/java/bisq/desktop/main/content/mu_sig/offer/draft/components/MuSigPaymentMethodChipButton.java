/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.mu_sig.offer.draft.components;

import bisq.account.payment_method.PaymentMethod;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import lombok.Getter;

import javax.annotation.Nullable;

public class MuSigPaymentMethodChipButton extends StackPane {
    private final static int TRUNCATION_LENGTH = 18;
    @Getter
    private final ChipButton chipButton;
    @Getter
    private final PaymentMethod<?> paymentMethod;
    private final String paymentMethodDisplayString;
    private int numAccounts;
    @Nullable
    private String accountName;
    @Nullable
    private Tooltip explicitTooltip;
    @Nullable
    private Tooltip inadmissibleReasonTooltip;

    public MuSigPaymentMethodChipButton(PaymentMethod<?> paymentMethod) {
        this.paymentMethod = paymentMethod;
        setAlignment(Pos.BOTTOM_RIGHT);
        paymentMethodDisplayString = paymentMethod.getShortDisplayString();
        chipButton = new ChipButton(paymentMethodDisplayString);
        getChildren().addAll(chipButton);
    }

    public void setActive(boolean value) {
        chipButton.setOpacity(value ? 1 : 0.3);
    }

    public void dispose() {
        setOnAction(null);
        chipButton.setTooltip(null);
    }

    public void setNumAccounts(int value) {
        numAccounts = value;
        updateText();
    }

    public void setAccountName(@Nullable String value) {
        accountName = value;
        updateText();
    }

    private void updateText() {
        Tooltip derivedTooltip = null;
        if (accountName != null) {
            String text = accountName + " (" + paymentMethodDisplayString + ")";
            chipButton.setText(StringUtils.truncate(text, TRUNCATION_LENGTH));
            derivedTooltip = new BisqTooltip(text);
        } else if (numAccounts > 1) {
            String text = StringUtils.truncate(paymentMethodDisplayString, TRUNCATION_LENGTH) + " (" + numAccounts + ")";
            chipButton.setText(text);
            derivedTooltip = new BisqTooltip(paymentMethodDisplayString + " (" + numAccounts + ")");
        } else {
            chipButton.setText(paymentMethodDisplayString);
        }
        // Priority: the inadmissibility reason always wins, then the tooltip derived from the
        // account name or count (whose full text the truncated chip label cannot show), then an
        // explicitly set one (e.g. a long payment method display name).
        if (inadmissibleReasonTooltip != null) {
            chipButton.setTooltip(inadmissibleReasonTooltip);
        } else if (derivedTooltip != null) {
            chipButton.setTooltip(derivedTooltip);
        } else {
            chipButton.setTooltip(explicitTooltip);
        }
    }

    public void setInadmissibleReasonTooltip(@Nullable Tooltip tooltip) {
        inadmissibleReasonTooltip = tooltip;
        updateText();
    }

    public void setTooltip(BisqTooltip bisqTooltip) {
        explicitTooltip = bisqTooltip;
        updateText();
    }

    public void setTooltip(Tooltip tooltip) {
        explicitTooltip = tooltip;
        updateText();
    }

    public void setLeftIcon(Node icon) {
        chipButton.setLeftIcon(icon);
    }

    public void setRightIcon(Node icon) {
        chipButton.setRightIcon(icon);
    }

    public Label setRightIcon(AwesomeIcon awesomeIcon) {
        return chipButton.setRightIcon(awesomeIcon);
    }

    public ImageView setRightIcon(String iconId) {
        return chipButton.setRightIcon(iconId);
    }

    public void setSelected(boolean value) {
        chipButton.setSelected(value);
    }

    public boolean isSelected() {
        return chipButton.isSelected();
    }

    public void setOnAction(Runnable onActionHandler) {
        chipButton.setOnAction(onActionHandler);
    }

    public void setToggleGroup(ToggleGroup value) {
        chipButton.setToggleGroup(value);
    }
}

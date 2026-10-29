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

package bisq.desktop.main.content.settings;

import bisq.common.observable.Observable;
import bisq.desktop.common.Transitions;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.settings.trade.TradeSettingsModel;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.settings.SettingsService;
import javafx.beans.property.DoubleProperty;
import javafx.event.ActionEvent;
import javafx.util.StringConverter;
import org.fxmisc.easybind.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.List;

import static bisq.desktop.testutil.JavaFxToolkitExtension.runOnFxThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Uses the max. trade price tolerance of the trade settings, which accepts 1 % to 10 %. The commit handler stands
 * for the controller and the service: it records the value and updates the shown value.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class SettingsViewUtilsTest {
    private final List<Number> committedValues = new ArrayList<>();
    private DoubleProperty value;
    private StringConverter<Number> converter;
    private MaterialTextField field;
    private Subscription pin;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);

        TradeSettingsModel model = new TradeSettingsModel();
        value = model.getMaxTradePriceDeviation();
        value.set(0.05);
        converter = model.getMaxTradePriceDeviationConverter();
        runOnFxThread(() -> {
            field = new MaterialTextField();
            field.setValidators(model.getMaxTradePriceDeviationValidator());
            pin = SettingsViewUtils.bindOnCommit(field, value, converter, newValue -> {
                committedValues.add(newValue);
                value.set(newValue.doubleValue());
            });
        });
    }

    @AfterEach
    void tearDown() {
        Transitions.setSettingsService(null);
    }

    @Test
    void typedValueIsCommittedOnlyOnEnterAndWhenValid() {
        assertThat(field.getText()).isEqualTo(converter.toString(0.05));

        runOnFxThread(() -> {
            // Typing 90 passes the valid prefix 9 first
            field.setText("9");
            field.setText("90");
        });
        assertThat(committedValues).isEmpty();

        runOnFxThread(this::pressEnter);
        assertThat(committedValues).isEmpty();
        assertThat(field.isValidProperty().get()).isFalse();

        runOnFxThread(() -> {
            field.setText("7");
            pressEnter();
        });
        assertThat(committedValues).hasSize(1);
        assertThat(committedValues.get(0).doubleValue()).isCloseTo(0.07, within(1e-9));
        assertThat(field.getText()).isEqualTo(converter.toString(0.07));
    }

    @Test
    void valueBelowTheMinimumIsNotCommittedAlthoughItRoundsToTheMinimum() {
        runOnFxThread(() -> {
            field.setText("0.9");
            pressEnter();
        });

        assertThat(committedValues).isEmpty();
        assertThat(field.isValidProperty().get()).isFalse();
    }

    @Test
    void unchangedTextIsNotCommitted() {
        runOnFxThread(() -> {
            // The converter shows 5.5 % rounded as 6 %
            value.set(0.055);
            pressEnter();
        });

        assertThat(committedValues).isEmpty();
        assertThat(value.get()).isEqualTo(0.055);
    }

    @Test
    void fieldFollowsTheValueAndUnsubscribeCommitsTheTypedValue() {
        runOnFxThread(() -> value.set(0.03));
        assertThat(field.getText()).isEqualTo(converter.toString(0.03));

        runOnFxThread(() -> {
            field.setText("8");
            pin.unsubscribe();
        });
        assertThat(committedValues).hasSize(1);
        assertThat(committedValues.get(0).doubleValue()).isCloseTo(0.08, within(1e-9));

        runOnFxThread(() -> value.set(0.04));
        assertThat(field.getText()).isEqualTo(converter.toString(0.08));
    }

    private void pressEnter() {
        field.getTextInputControl().fireEvent(new ActionEvent());
    }
}

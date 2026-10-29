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

package bisq.desktop.main.content.settings.trade;

import bisq.common.observable.Observable;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.api.FxToolkit;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static bisq.desktop.testutil.JavaFxToolkitExtension.runOnFxThread;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the settings view through its real lifecycle: the view gets attached, the user types, and the view gets
 * detached without a focus change. When the view is detached, the controller has already removed its bindings.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class TradeSettingsControllerTest {
    private SettingsService settingsService;
    private MaterialTextField maxTradePriceDeviation;

    @BeforeEach
    void setUp() throws TimeoutException {
        settingsService = mock(SettingsService.class);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        when(settingsService.getCloseMyOfferWhenTaken()).thenReturn(new Observable<>(true));
        when(settingsService.getMaxTradePriceDeviation()).thenReturn(new Observable<>(0.05));
        when(settingsService.getPriceDeviationWarningThreshold()).thenReturn(new Observable<>(0.1));
        when(settingsService.getNumDaysAfterRedactingTradeData()).thenReturn(new Observable<>(90));
        Transitions.setSettingsService(settingsService);

        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);

        TradeSettingsController[] controller = new TradeSettingsController[1];
        runOnFxThread(() -> controller[0] = new TradeSettingsController(serviceProvider));
        Parent root = controller[0].getView().getRoot();
        FxToolkit.setupSceneRoot(() -> root);
        maxTradePriceDeviation = findField(root, Res.get("settings.trade.maxTradePriceDeviation")).orElseThrow();
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        FxToolkit.setupSceneRoot(Pane::new);
        Transitions.setSettingsService(null);
    }

    @Test
    void typedValidValueIsSavedWhenTheViewIsDetached() throws TimeoutException {
        runOnFxThread(() -> maxTradePriceDeviation.setText("7"));

        FxToolkit.setupSceneRoot(Pane::new);

        verify(settingsService).setMaxTradePriceDeviation(doubleThat(value -> Math.abs(value - 0.07) < 1e-9));
    }

    @Test
    void validPrefixOfAnInvalidValueIsNotSaved() throws TimeoutException {
        runOnFxThread(() -> {
            maxTradePriceDeviation.setText("9");
            maxTradePriceDeviation.setText("90");
        });

        FxToolkit.setupSceneRoot(Pane::new);

        verify(settingsService, never()).setMaxTradePriceDeviation(anyDouble());
    }

    private static Optional<MaterialTextField> findField(Parent parent, String description) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof MaterialTextField field && description.equals(field.getDescription())) {
                return Optional.of(field);
            }
            if (child instanceof Parent childParent) {
                Optional<MaterialTextField> result = findField(childParent, description);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }
}

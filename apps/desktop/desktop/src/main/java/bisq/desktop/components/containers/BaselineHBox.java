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

package bisq.desktop.components.containers;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * A custom {@link Pane} that positions a single child node such that its
 * baseline aligns with a fixed vertical coordinate.
 * <p>
 * This pane is useful for layouts where consistent baseline alignment is
 * required (e.g., aligning text fields with dynamic font sizes alongside
 * static components). The child node is vertically shifted so that its
 * {@link Node#getBaselineOffset()} matches the configured {@code baselineY}.
 * <p>
 * The pane assumes a single child and will size it to the full available width
 * while preserving its preferred height. The preferred height of this pane is
 * calculated to ensure that the baseline is positioned at {@code baselineY}
 * and the child is fully visible below it.
 */

public class BaselineHBox extends HBox {
    private final Node child;
    private final double baselineY;

    public BaselineHBox(Node child, double baselineY) {
        this.child = child;
        this.baselineY = baselineY;
        getChildren().add(child);
    }

    @Override
    protected void layoutChildren() {
        double baseline = child.getBaselineOffset();
        double y = baselineY - baseline;
        child.resizeRelocate(0, y, getWidth(), child.prefHeight(-1));
    }

    @Override
    protected double computePrefHeight(double width) {
        return baselineY + child.prefHeight(-1);
    }
}
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

package bisq.desktop.common.view;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class View<R extends Node, M extends Model, C extends Controller> {
    protected final R root;
    protected final M model;
    protected final C controller;
    private final ChangeListener<Scene> sceneChangeListener;
    private ChangeListener<Window> windowChangeListener;

    public View(R root, M model, C controller) {
        checkNotNull(root, "Root must not be null");
        this.root = root;
        this.model = model;
        this.controller = controller;

        sceneChangeListener = (ov, oldValue, newScene) -> {
            if (oldValue == null && newScene != null) {
                if (newScene.getWindow() != null) {
                    onViewAttached();
                    controller.onViewAttached();
                    model.onViewAttached();
                    //  UIThread.run(() -> root.sceneProperty().removeListener(View.this.sceneChangeListener));
                } else {
                    // For overlays, we need to wait until window is available
                    windowChangeListener = (observable, oldValue1, newWindow) -> {
                        checkNotNull(newWindow, "Window must not be null");
                        onViewAttached();   // We invert the order at deactivate as we want to remove listeners in view first
                        controller.onViewAttached();
                        model.onViewAttached();
                        // UIThread.run(() -> newScene.windowProperty().removeListener(View.this.windowChangeListener));
                    };
                    newScene.windowProperty().addListener(windowChangeListener);
                }
            } else if (oldValue != null && newScene == null) {
                onViewDetached();
                controller.onViewDetached();
                model.onViewDetached();
            }
        };
        root.sceneProperty().addListener(sceneChangeListener);
    }

    public R getRoot() {
        return root;
    }

    protected void onViewAttached() {
    }

    protected void onViewDetached() {
    }
}

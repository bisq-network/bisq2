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

import bisq.desktop.common.threading.UIThread;
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

        boolean isCaching = controller instanceof CachingController;
        sceneChangeListener = (ov, oldValue, newScene) -> {
            if (oldValue == null && newScene != null) {
                if (newScene.getWindow() != null) {
                    onViewAttachedPrivate();
                } else {
                    // For overlays, we need to wait until window is available
                    windowChangeListener = (observable, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            onViewAttachedPrivate();
                        } else {
                            onViewDetachedPrivate();
                            UIThread.runOnNextRenderFrame(() -> newScene.windowProperty().removeListener(windowChangeListener));
                        }
                    };
                    newScene.windowProperty().addListener(windowChangeListener);
                }
            } else if (oldValue != null && newScene == null) {
                onViewDetachedPrivate();
                if (!isCaching) {
                    // If we do not use caching we do not expect to get added again to stage without creating a 
                    // new instance of the view, so we remove our sceneChangeListener.
                    UIThread.runOnNextRenderFrame(() -> root.sceneProperty().removeListener(View.this.sceneChangeListener));
                    if (oldValue.getWindow() != null && windowChangeListener != null) {
                        UIThread.runOnNextRenderFrame(() -> oldValue.windowProperty().removeListener(windowChangeListener));
                    }
                }
            }
        };
        root.sceneProperty().addListener(sceneChangeListener);
    }

    public R getRoot() {
        return root;
    }

    private void onViewDetachedPrivate() {
        onViewDetachedInternal();
        controller.onDeactivateInternal();
    }

    private void onViewAttachedPrivate() {
        onViewAttachedInternal();
        controller.onActivateInternal();
    }

    // The internal methods should be only used by framework classes (e.g. TabView)
    void onViewAttachedInternal() {
        onViewAttached();
    }

    void onViewDetachedInternal() {
        onViewDetached();
    }

    abstract protected void onViewAttached();

    abstract protected void onViewDetached();
}

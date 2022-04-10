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

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIThread;
import com.jfoenix.controls.JFXTextArea;
import javafx.beans.InvalidationListener;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.text.Text;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * TextArea does not support of adjustment of height based on the text content.
 * A freelance developer has provided an implementation for that feature based on ideas shared at:
 * https://stackoverflow.com/questions/18588765/how-can-i-make-a-textarea-stretch-to-fill-the-content-expanding-the-parent-in-t/46590540#46590540
 */
@Slf4j
public class BisqTextArea extends JFXTextArea {
    private static final String SELECTOR_TEXT = ".viewport .content .text";
    private static final String SELECTOR_SCROLL_PANE = ".scroll-pane";
    private static final double INITIAL_HEIGHT = 19.0;
    private static final double SCROLL_HIDE_THRESHOLD = INITIAL_HEIGHT * 5;

    private double initialHeight = INITIAL_HEIGHT;
    @Setter
    private double scrollHideThreshold = SCROLL_HIDE_THRESHOLD;
    private boolean initialized;
    private final InvalidationListener textChangeListener = o -> adjustHeight();
    ;
    private ScrollPane selectorScrollPane;
    private Text selectorText;

    public BisqTextArea() {
        setWrapText(true);

        setStyle("-fx-background-color: -bisq-grey-5; -fx-border-color: -bisq-grey-5; -fx-text-fill: -fx-light-text-color;");

        // We use a weakReference for the sceneChangeListener to avoid leaking when our instance is gone
        Objects.requireNonNull(new WeakReference<>(this).get()).sceneProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue == null) {
                        // When we get removed from the display graph we remove the textChangeListener. 
                        // We delay that to the next render frame to avoid potential ConcurrentModificationExceptions 
                        // at the listener collections.
                        UIThread.runOnNextRenderFrame(() -> textProperty().removeListener(textChangeListener));
                    } else {
                        initialized = false;
                        layoutChildren();
                    }
                });
    }

    public BisqTextArea(String text) {
        this();
        setText(text);
    }

    public void setInitialHeight(double initialHeight) {
        this.initialHeight = initialHeight;
        if (scrollHideThreshold < initialHeight) {
            scrollHideThreshold = initialHeight;
        }
    }

    @Override
    protected void layoutChildren() {
        if (lookup(SELECTOR_SCROLL_PANE) instanceof ScrollPane selectorScrollPane) {
            if (!selectorScrollPane.getChildrenUnmodifiable().isEmpty()) {
                try {
                    super.layoutChildren();
                } catch (Throwable t) {
                    t.printStackTrace();
                    super.layoutChildren();
                }
            }

            if (!initialized) {
                this.selectorScrollPane = selectorScrollPane;

                if (lookup(SELECTOR_TEXT) instanceof Text selectorText) {
                    this.selectorText = selectorText;

                    textProperty().addListener(textChangeListener);
                    selectorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    adjustHeight();
                    initialized = true;
                }
            }
        }
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JFXTextAreaSkinBisqStyle(this);
    }

    private void adjustHeight() {
        double textHeight = selectorText.getBoundsInLocal().getHeight();
        if (textHeight < initialHeight) {
            textHeight = initialHeight;
        }
        if (textHeight > scrollHideThreshold) {
            selectorScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        } else {
            selectorScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        setMinHeight(textHeight + INITIAL_HEIGHT);
        setMaxHeight(textHeight + INITIAL_HEIGHT);
    }
}

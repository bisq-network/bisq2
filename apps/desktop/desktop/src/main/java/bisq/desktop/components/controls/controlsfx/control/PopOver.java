/*
 * Copyright (c) 2013, 2016 ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package bisq.desktop.components.controls.controlsfx.control;

import bisq.desktop.components.controls.controlsfx.skin.PopOverSkin;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import static java.util.Objects.requireNonNull;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

/**
 * The PopOver control provides detailed information about an owning node in a
 * popup window. The popup window has a very lightweight appearance (no default
 * window decorations) and an arrow pointing at the owner. Due to the nature of
 * popup windows the PopOver will move around with the parent window when the
 * user drags it. <br>
 * <center> <img src="popover.png" alt="Screenshot of PopOver"> </center> <br>
 * The PopOver can be detached from the owning node by dragging it away from the
 * owner. It stops displaying an arrow and starts displaying a title and a close
 * icon. <br>
 * <br>
 * <center> <img src="popover-detached.png"
 * alt="Screenshot of a detached PopOver"> </center> <br>
 * The following image shows a popover with an accordion content node. PopOver
 * controls are automatically resizing themselves when the content node changes
 * its size.<br>
 * <br>
 * <center> <img src="popover-accordion.png"
 * alt="Screenshot of PopOver containing an Accordion"> </center> <br>
 * For styling apply stylesheets to the root pane of the PopOver.
 *
 * <h3>Example:</h3>
 *
 * <pre>
 * PopOver popOver = new PopOver();
 * popOver.getRoot().getStylesheets().add(...);
 * </pre>
 */
public class PopOver extends PopupControl {
    private static final String DEFAULT_STYLE_CLASS = "popover";
    private static final Duration DEFAULT_FADE_DURATION = Duration.seconds(0.2);

    private double targetX;
    private double targetY;
    private Window ownerWindow;
    private final SimpleBooleanProperty isAnimated = new SimpleBooleanProperty(true);
    private final ObjectProperty<Duration> fadeInDuration = new SimpleObjectProperty<>(DEFAULT_FADE_DURATION);
    private final ObjectProperty<Duration> fadeOutDuration = new SimpleObjectProperty<>(DEFAULT_FADE_DURATION);
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener repositionListener = observable -> {
        if (isShowing() && !getIsDetached()) {
            show(getOwnerNode(), targetX, targetY);
            adjustWindowLocation();
        }
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener isDetachedListener = observable -> setAutoHide(!getIsDetached());
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener hideListener = observable -> {
        if (!getIsDetached()) {
            hide(Duration.ZERO);
        }
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> xListener = (value, oldX, newX) -> {
        if (!getIsDetached()) {
            setAnchorX(getAnchorX() + (newX.doubleValue() - oldX.doubleValue()));
        }
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> yListener = (value, oldY, newY) -> {
        if (!getIsDetached()) {
            setAnchorY(getAnchorY() + (newY.doubleValue() - oldY.doubleValue()));
        }
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakWindowEvent
    private final EventHandler<WindowEvent> ownerWindowCloseHandler = event -> hide(Duration.ZERO);

    public PopOver() {
        super();

        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setAnchorLocation(AnchorLocation.WINDOW_TOP_LEFT);
        setOnHiding(evt -> setIsDetached(false));

        // Create some initial content.
        Label label = new Label("No content set");
        label.setPrefSize(200, 200);
        label.setPadding(new Insets(4));
        setContentNode(label);

        arrowSize.addListener(new WeakInvalidationListener(repositionListener));
        cornerRadius.addListener(new WeakInvalidationListener(repositionListener));
        arrowLocation.addListener(new WeakInvalidationListener(repositionListener));
        arrowIndent.addListener(new WeakInvalidationListener(repositionListener));
        headerAlwaysVisible.addListener(new WeakInvalidationListener(repositionListener));

        // A detached popover should of course not automatically hide itself.
        isDetached.addListener(new WeakInvalidationListener(isDetachedListener));

        setAutoHide(true);
    }

    /**
     * Creates a pop-over with the given node as the content node.
     *
     * @param content The content shown by the pop-over
     */
    public PopOver(Node content) {
        this();

        setContentNode(content);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new PopOverSkin(this);
    }

    private final StackPane root = new StackPane();

    /**
     * The root pane stores the content node of the popover. It is accessible
     * via this method in order to support proper styling.
     *
     * <h3>Example:</h3>
     *
     * <pre>
     * PopOver popOver = new PopOver();
     * popOver.getRoot().getStylesheets().add(...);
     * </pre>
     *
     * @return the root pane
     */
    public final StackPane getRoot() {
        return root;
    }

    // Content support.

    private final ObjectProperty<Node> contentNode = new SimpleObjectProperty<>(
            this, "contentNode") {
        @Override
        public void setValue(Node node) {
            if (node == null) {
                throw new IllegalArgumentException(
                        "content node can not be null");
            }
        }

    };

    /**
     * Returns the content shown by the pop-over.
     *
     * @return the content node property
     */
    public final ObjectProperty<Node> contentNodeProperty() {
        return contentNode;
    }

    /**
     * Returns the value of the content property
     *
     * @return the content node
     * @see #contentNodeProperty()
     */
    public final Node getContentNode() {
        return contentNodeProperty().get();
    }

    /**
     * Sets the value of the content property.
     *
     * @param content the new content node value
     * @see #contentNodeProperty()
     */
    public final void setContentNode(Node content) {
        contentNodeProperty().set(content);
    }

    /**
     * Shows the pop-over in a position relative to the edges of the given owner
     * node. The position is dependent on the arrow location. If the arrow is
     * pointing to the right then the pop-over will be placed to the left of the
     * given owner. If the arrow points up then the pop-over will be placed
     * below the given owner node. The arrow will slightly overlap with the
     * owner node.
     *
     * @param owner the owner of the pop-over
     */
    public final void show(Node owner) {
        show(owner, 4);
    }

    /**
     * Shows the pop-over in a position relative to the edges of the given owner
     * node. The position is dependent on the arrow location. If the arrow is
     * pointing to the right then the pop-over will be placed to the left of the
     * given owner. If the arrow points up then the pop-over will be placed
     * below the given owner node.
     *
     * @param owner  the owner of the pop-over
     * @param offset if negative specifies the distance to the owner node or when
     *               positive specifies the number of pixels that the arrow will
     *               overlap with the owner node (positive values are recommended)
     */
    public final void show(Node owner, double offset) {
        requireNonNull(owner);

        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());

        switch (getArrowLocation()) {
            case BOTTOM_CENTER:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                show(owner, bounds.getMinX() + bounds.getWidth() / 2,
                        bounds.getMinY() + offset);
                break;
            case LEFT_BOTTOM:
            case LEFT_CENTER:
            case LEFT_TOP:
                show(owner, bounds.getMaxX() - offset,
                        bounds.getMinY() + bounds.getHeight() / 2);
                break;
            case RIGHT_BOTTOM:
            case RIGHT_CENTER:
            case RIGHT_TOP:
                show(owner, bounds.getMinX() + offset,
                        bounds.getMinY() + bounds.getHeight() / 2);
                break;
            case TOP_CENTER:
            case TOP_LEFT:
            case TOP_RIGHT:
                show(owner, bounds.getMinX() + bounds.getWidth() / 2,
                        bounds.getMinY() + bounds.getHeight() - offset);
                break;
            default:
                break;
        }
    }

    @Override
    public final void show(Window ownerWindow) {
        super.show(ownerWindow);
        doShow(ownerWindow);
    }

    @Override
    public final void show(Window ownerWindow, double anchorX, double anchorY) {
        super.show(ownerWindow, anchorX, anchorY);
        doShow(ownerWindow);
    }

    private void doShow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;

        if (getIsAnimated()) {
            showFadeInAnimation(getFadeInDuration());
        }

        ownerWindow.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, new WeakEventHandler<>(ownerWindowCloseHandler));
        ownerWindow.addEventFilter(WindowEvent.WINDOW_HIDING, new WeakEventHandler<>(ownerWindowCloseHandler));
    }

    /**
     * Makes the pop-over visible at the give location and associates it with
     * the given owner node. The anchorX and anchorY coordinate will be the target location
     * of the arrow of the pop-over and not the location of the window.
     *
     * @param owner the owning node
     * @param anchorX     the anchorX coordinate for the pop-over arrow tip
     * @param anchorY     the anchorY coordinate for the pop-over arrow tip
     */
    @Override
    public final void show(Node owner, double anchorX, double anchorY) {
        show(owner, anchorX, anchorY, getFadeInDuration());
    }

    /**
     * Makes the pop-over visible at the give location and associates it with
     * the given owner node. The anchorX and anchorY coordinate will be the target location
     * of the arrow of the pop-over and not the location of the window.
     *
     * @param owner          the owning node
     * @param anchorX              the anchorX coordinate for the pop-over arrow tip
     * @param anchorY              the anchorY coordinate for the pop-over arrow tip
     * @param fadeInDuration the time it takes for the pop-over to be fully visible. This duration takes precedence over the fade-in property without setting.
     */
    public final void show(Node owner, double anchorX, double anchorY, Duration fadeInDuration) {

        /*
         * Calling show() a second time without first closing the pop-over
         * causes it to be placed at the wrong location.
         */
        if (ownerWindow != null && isShowing()) {
            super.hide();
        }

        targetX = anchorX;
        targetY = anchorY;

        if (owner == null) {
            throw new IllegalArgumentException("owner can not be null");
        }

        if (fadeInDuration == null) {
            fadeInDuration = DEFAULT_FADE_DURATION;
        }

        ownerWindow = owner.getScene().getWindow();
        ownerWindow.xProperty().addListener(new WeakChangeListener<>(xListener));
        ownerWindow.yProperty().addListener(new WeakChangeListener<>(yListener));
        ownerWindow.widthProperty().addListener(new WeakInvalidationListener(hideListener));
        ownerWindow.heightProperty().addListener(new WeakInvalidationListener(hideListener));

        setOnShown(evt -> {

            /*
             * The user clicked somewhere into the transparent background. If
             * this is the case then hide the window (when attached).
             */
            getScene().addEventHandler(MOUSE_CLICKED, mouseEvent -> {
                if (mouseEvent.getTarget().equals(getScene().getRoot())) {
                    if (!getIsDetached()) {
                        hide();
                    }
                }
            });

            /*
             * Move the window so that the arrow will end up pointing at the
             * target coordinates.
             */
            adjustWindowLocation();
        });

        super.show(owner, anchorX, anchorY);

        if (getIsAnimated()) {
            showFadeInAnimation(fadeInDuration);
        }

        // Bug fix - close popup when owner window is closing
        ownerWindow.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, new WeakEventHandler<>(ownerWindowCloseHandler));
        ownerWindow.addEventFilter(WindowEvent.WINDOW_HIDING, new WeakEventHandler<>(ownerWindowCloseHandler));
    }

    private void showFadeInAnimation(Duration fadeInDuration) {
        // Fade In
        Node skinNode = getSkin().getNode();
        skinNode.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(fadeInDuration, skinNode);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Hides the pop-over by quickly changing its opacity to 0.
     *
     * @see #hide(Duration)
     */
    @Override
    public final void hide() {
        hide(getFadeOutDuration());
    }

    /**
     * Hides the pop-over by quickly changing its opacity to 0.
     *
     * @param fadeOutDuration the duration of the fade transition that is being used to
     *                        change the opacity of the pop-over
     * @since 1.0
     */
    public final void hide(Duration fadeOutDuration) {
        if (fadeOutDuration == null) {
            fadeOutDuration = DEFAULT_FADE_DURATION;
        }

        if (isShowing()) {
            if (getIsAnimated()) {
                // Fade Out
                Node skinNode = getSkin().getNode();

                FadeTransition fadeOut = new FadeTransition(fadeOutDuration,
                        skinNode);
                fadeOut.setFromValue(skinNode.getOpacity());
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(evt -> super.hide());
                fadeOut.play();
            } else {
                super.hide();
            }
        }
    }

    private void adjustWindowLocation() {
        Bounds bounds = PopOver.this.getSkin().getNode().getBoundsInParent();

        switch (getArrowLocation()) {
            case TOP_CENTER:
            case TOP_LEFT:
            case TOP_RIGHT:
                setAnchorX(getAnchorX() + bounds.getMinX() - computeXOffset());
                setAnchorY(getAnchorY() + bounds.getMinY() + getArrowSize());
                break;
            case LEFT_TOP:
            case LEFT_CENTER:
            case LEFT_BOTTOM:
                setAnchorX(getAnchorX() + bounds.getMinX() + getArrowSize());
                setAnchorY(getAnchorY() + bounds.getMinY() - computeYOffset());
                break;
            case BOTTOM_CENTER:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                setAnchorX(getAnchorX() + bounds.getMinX() - computeXOffset());
                setAnchorY(getAnchorY() - bounds.getMinY() - bounds.getMaxY() - 1);
                break;
            case RIGHT_TOP:
            case RIGHT_BOTTOM:
            case RIGHT_CENTER:
                setAnchorX(getAnchorX() - bounds.getMinX() - bounds.getMaxX() - 1);
                setAnchorY(getAnchorY() + bounds.getMinY() - computeYOffset());
                break;
        }
    }

    private double computeXOffset() {
        return switch (getArrowLocation()) {
            case TOP_LEFT, BOTTOM_LEFT -> getCornerRadius() + getArrowIndent() + getArrowSize();
            case TOP_CENTER, BOTTOM_CENTER -> getContentNode().prefWidth(-1) / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> getContentNode().prefWidth(-1) - getArrowIndent()
                    - getCornerRadius() - getArrowSize();
            default -> 0;
        };
    }

    private double computeYOffset() {
        double prefContentHeight = getContentNode().prefHeight(-1);

        return switch (getArrowLocation()) {
            case LEFT_TOP, RIGHT_TOP -> getCornerRadius() + getArrowIndent() + getArrowSize();
            case LEFT_CENTER, RIGHT_CENTER -> Math.max(prefContentHeight, 2 * (getCornerRadius()
                    + getArrowIndent() + getArrowSize())) / 2;
            case LEFT_BOTTOM, RIGHT_BOTTOM -> Math.max(prefContentHeight - getCornerRadius()
                    - getArrowIndent() - getArrowSize(), getCornerRadius()
                    + getArrowIndent() + getArrowSize());
            default -> 0;
        };
    }

    /**
     * Detaches the pop-over from the owning node. The pop-over will no longer
     * display an arrow pointing at the owner node.
     */
    public final void detach() {
        if (isDetachable()) {
            setIsDetached(true);
        }
    }

    // always show header

    private final BooleanProperty headerAlwaysVisible = new SimpleBooleanProperty(this, "headerAlwaysVisible");

    /**
     * Determines whether the {@link PopOver} header should remain visible, even while attached.
     */
    public final BooleanProperty headerAlwaysVisibleProperty() {
        return headerAlwaysVisible;
    }

    /**
     * Sets the value of the headerAlwaysVisible property.
     *
     * @param visible if true, then the header is visible even while attached
     * @see #headerAlwaysVisibleProperty()
     */
    public final void setHeaderAlwaysVisible(boolean visible) {
        headerAlwaysVisible.setValue(visible);
    }

    /**
     * Returns the value of the detachable property.
     *
     * @return true if the header is visible even while attached
     * @see #headerAlwaysVisibleProperty()
     */
    public final boolean isHeaderAlwaysVisible() {
        return headerAlwaysVisible.getValue();
    }

    // enable close button

    private final BooleanProperty closeButtonEnabled = new SimpleBooleanProperty(this, "closeButtonEnabled", true);

    /**
     * Determines whether the header's close button should be available.
     */
    public final BooleanProperty closeButtonEnabledProperty() {
        return closeButtonEnabled;
    }

    /**
     * Sets the value of the closeButtonEnabled property.
     *
     * @param enabled if false, the pop-over will not be closeable by the header's close button
     * @see #closeButtonEnabledProperty()
     */
    public final void setCloseButtonEnabled(boolean enabled) {
        closeButtonEnabled.setValue(enabled);
    }

    /**
     * Returns the value of the closeButtonEnabled property.
     *
     * @return true if the header's close button is enabled
     * @see #closeButtonEnabledProperty()
     */
    public final boolean isCloseButtonEnabled() {
        return closeButtonEnabled.getValue();
    }

    // detach support

    private final BooleanProperty detachable = new SimpleBooleanProperty(this,
            "detachable", true);

    /**
     * Determines if the pop-over is detachable at all.
     */
    public final BooleanProperty detachableProperty() {
        return detachable;
    }

    /**
     * Sets the value of the detachable property.
     *
     * @param detachable if true then the user can detach / tear off the pop-over
     * @see #detachableProperty()
     */
    public final void setDetachable(boolean detachable) {
        detachableProperty().set(detachable);
    }

    /**
     * Returns the value of the detachable property.
     *
     * @return true if the user is allowed to detach / tear off the pop-over
     * @see #detachableProperty()
     */
    public final boolean isDetachable() {
        return detachableProperty().get();
    }

    private final BooleanProperty isDetached = new SimpleBooleanProperty(this,
            "detached", false);

    /**
     * Determines whether the pop-over is detached from the owning node or not.
     * A detached pop-over no longer shows an arrow pointing at the owner and
     * features its own title bar.
     *
     * @return the detached property
     */
    public final BooleanProperty isDetachedProperty() {
        return isDetached;
    }

    /**
     * Sets the value of the detached property.
     *
     * @param isDetached if true the pop-over will change its appearance to "detached"
     *                 mode
     * @see #isDetachedProperty()
     */
    public final void setIsDetached(boolean isDetached) {
        isDetachedProperty().set(isDetached);
    }

    /**
     * Returns the value of the detached property.
     *
     * @return true if the pop-over is currently detached.
     * @see #isDetachedProperty()
     */
    public final boolean getIsDetached() {
        return isDetachedProperty().get();
    }

    private final DoubleProperty arrowSize = new SimpleDoubleProperty(this,
            "arrowSize", 12);

    /**
     * Controls the size of the arrow. Default value is 12.
     *
     * @return the arrow size property
     */
    public final DoubleProperty arrowSizeProperty() {
        return arrowSize;
    }

    /**
     * Returns the value of the arrow size property.
     *
     * @return the arrow size property value
     * @see #arrowSizeProperty()
     */
    public final double getArrowSize() {
        return arrowSizeProperty().get();
    }

    /**
     * Sets the value of the arrow size property.
     *
     * @param size the new value of the arrow size property
     * @see #arrowSizeProperty()
     */
    public final void setArrowSize(double size) {
        arrowSizeProperty().set(size);
    }

    private final DoubleProperty arrowIndent = new SimpleDoubleProperty(this,
            "arrowIndent", 12);

    /**
     * Controls the distance between the arrow and the corners of the pop-over.
     * The default value is 12.
     *
     * @return the arrow indent property
     */
    public final DoubleProperty arrowIndentProperty() {
        return arrowIndent;
    }

    /**
     * Returns the value of the arrow indent property.
     *
     * @return the arrow indent value
     * @see #arrowIndentProperty()
     */
    public final double getArrowIndent() {
        return arrowIndentProperty().get();
    }

    /**
     * Sets the value of the arrow indent property.
     *
     * @param size the arrow indent value
     * @see #arrowIndentProperty()
     */
    public final void setArrowIndent(double size) {
        arrowIndentProperty().set(size);
    }

    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(this,
            "cornerRadius", 6);

    /**
     * Returns the corner radius property for the pop-over.
     *
     * @return the corner radius property (default is 6)
     */
    public final DoubleProperty cornerRadiusProperty() {
        return cornerRadius;
    }

    /**
     * Returns the value of the corner radius property.
     *
     * @return the corner radius
     * @see #cornerRadiusProperty()
     */
    public final double getCornerRadius() {
        return cornerRadiusProperty().get();
    }

    /**
     * Sets the value of the corner radius property.
     *
     * @param radius the corner radius
     * @see #cornerRadiusProperty()
     */
    public final void setCornerRadius(double radius) {
        cornerRadiusProperty().set(radius);
    }

    // Detached stage title

    private final StringProperty title = new SimpleStringProperty(this, "title", "No title set"); //$NON-NLS-2$

    /**
     * Stores the title to display in the PopOver's header.
     *
     * @return the title property
     */
    public final StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the value of the title property.
     *
     * @return the detached title
     * @see #titleProperty()
     */
    public final String getTitle() {
        return titleProperty().get();
    }

    /**
     * Sets the value of the title property.
     *
     * @param title the title to use when detached
     * @see #titleProperty()
     */
    public final void setTitle(String title) {
        if (title == null) {
            throw new IllegalArgumentException("title can not be null");
        }

        titleProperty().set(title);
    }

    private final ObjectProperty<ArrowLocation> arrowLocation = new SimpleObjectProperty<>(
            this, "arrowLocation", ArrowLocation.LEFT_TOP);

    /**
     * Stores the preferred arrow location. This might not be the actual
     * location of the arrow if auto fix is enabled.
     *
     * @return the arrow location property
     * @see #setAutoFix(boolean)
     */
    public final ObjectProperty<ArrowLocation> arrowLocationProperty() {
        return arrowLocation;
    }

    /**
     * Sets the value of the arrow location property.
     *
     * @param location the requested location
     * @see #arrowLocationProperty()
     */
    public final void setArrowLocation(ArrowLocation location) {
        arrowLocationProperty().set(location);
    }

    /**
     * Returns the value of the arrow location property.
     *
     * @return the preferred arrow location
     * @see #arrowLocationProperty()
     */
    public final ArrowLocation getArrowLocation() {
        return arrowLocationProperty().get();
    }

    /**
     * All possible arrow locations.
     */
    public enum ArrowLocation {
        LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM, TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    /**
     * Stores the fade-in duration. This should be set before calling PopOver.show(..).
     *
     * @return the fade-in duration property
     */
    public final ObjectProperty<Duration> fadeInDurationProperty() {
        return fadeInDuration;
    }

    /**
     * Stores the fade-out duration.
     *
     * @return the fade-out duration property
     */
    public final ObjectProperty<Duration> fadeOutDurationProperty() {
        return fadeOutDuration;
    }

    /**
     * Returns the value of the fade-in duration property.
     *
     * @return the fade-in duration
     * @see #fadeInDurationProperty()
     */
    public final Duration getFadeInDuration() {
        return fadeInDurationProperty().get();
    }

    /**
     * Sets the value of the fade-in duration property. This should be set before calling PopOver.show(..).
     *
     * @param duration the requested fade-in duration
     * @see #fadeInDurationProperty()
     */
    public final void setFadeInDuration(Duration duration) {
        fadeInDurationProperty().setValue(duration);
    }

    /**
     * Returns the value of the fade-out duration property.
     *
     * @return the fade-out duration
     * @see #fadeOutDurationProperty()
     */
    public final Duration getFadeOutDuration() {
        return fadeOutDurationProperty().get();
    }

    /**
     * Sets the value of the fade-out duration property.
     *
     * @param duration the requested fade-out duration
     * @see #fadeOutDurationProperty()
     */
    public final void setFadeOutDuration(Duration duration) {
        fadeOutDurationProperty().setValue(duration);
    }

    /**
     * Stores the "animated" flag. If true then the PopOver will be shown / hidden with a short fade in / out animation.
     *
     * @return the "animated" property
     */
    public final BooleanProperty isAnimatedProperty() {
        return isAnimated;
    }

    /**
     * Returns the value of the "animated" property.
     *
     * @return true if the PopOver will be shown and hidden with a short fade animation
     * @see #isAnimatedProperty()
     */
    public final boolean getIsAnimated() {
        return isAnimatedProperty().get();
    }

    /**
     * Sets the value of the "animated" property.
     *
     * @param isAnimated if true the PopOver will be shown and hidden with a short fade animation
     * @see #isAnimatedProperty()
     */
    public final void setIsAnimated(boolean isAnimated) {
        isAnimatedProperty().set(isAnimated);
    }
}

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

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class ProgressBarWithLabel extends VBox {
    private final Label label;
    private final ProgressBar progressBar;
    private final StringProperty textProperty = new SimpleStringProperty();
    private final Subscription widthPin;
    private String postFix = "";
    private UIScheduler scheduler;
    @Setter
    private String ellipsis = "...";
    @Setter
    private boolean animateEllipsis = true;

    public ProgressBarWithLabel() {
        this("");
    }

    public ProgressBarWithLabel(String text) {
        this(text, -1);
    }

    public ProgressBarWithLabel(String text, double progress) {
        super(2.5);
        this.textProperty.set(text);
        setAlignment(Pos.CENTER_LEFT);


        label = new Label(text + ellipsis);
        label.setMouseTransparent(true);
        label.getStyleClass().add("bisq-text-18");

        progressBar = new ProgressBar(progress);
        progressBar.setMinHeight(1);
        progressBar.setMaxHeight(progressBar.getMinHeight());
        getChildren().addAll(label, progressBar);

        widthPin = EasyBind.subscribe(label.widthProperty(), w -> {
            if (w.doubleValue() > 0) {
                label.setMinWidth(w.doubleValue());
                unsubscribe();
            }
        });
        label.widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                progressBar.setPrefWidth(newValue.doubleValue())).get());

        textProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                label.setText(newValue + ellipsis)).get());

        if (animateEllipsis) {
            progressBar.progressProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                    animatePostfix()).get());
        }
        if (animateEllipsis) {
            animatePostfix();
        }
    }

    private void unsubscribe() {
        UIThread.runOnNextRenderFrame(widthPin::unsubscribe);
    }

    private void animatePostfix() {
        if (progressBar.getProgress() == -1) {
            if (scheduler != null) {
                scheduler.stop();
            }
            scheduler = UIScheduler.run(() -> {
                label.setText(getText() + postFix);
                switch (postFix) {
                    case "   ":
                        postFix = ".  ";
                        break;
                    case ".  ":
                        postFix = ".. ";
                        break;
                    case ".. ":
                        postFix = "...";
                        break;
                    default:
                        postFix = "   ";
                        break;
                }
            }).periodically(300, TimeUnit.MILLISECONDS);
        } else {
            postFix = "";
            if (scheduler != null) {
                scheduler.stop();
                scheduler = null;
            }
        }
    }

    public final void setText(String value) {
        textProperty.set(value);
    }

    public final String getText() {
        return textProperty.get();
    }

    public final StringProperty textProperty() {
        return textProperty;
    }

    public void setProgress(double value) {
        progressBar.setProgress(value);
    }

    public double getProgress() {
        return progressBar.getProgress();
    }

    public DoubleProperty progressProperty() {
        return progressBar.progressProperty();
    }
}
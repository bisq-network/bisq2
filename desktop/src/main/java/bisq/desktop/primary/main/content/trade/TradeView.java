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

package bisq.desktop.primary.main.content.trade;

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.overlay.OverlayWindow;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TradeView extends NavigationView<VBox, TradeModel, TradeController> {
    private final Label headlineLabel;
    private final Region line;
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private final Button createOfferButton;
    private OverlayWindow overlayWindow;

    public TradeView(TradeModel model, TradeController controller) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);
        root.setPadding(new Insets(0, -67, 0, 0));

        headlineLabel = new Label(Res.get("offerbook"));
        headlineLabel.getStyleClass().add("bisq-content-headline-label");

        createOfferButton = new Button(Res.get("createOffer.createOffer.button"));
        createOfferButton.setDefaultButton(true);
       // createOfferButton.getStyleClass().add("bisq-border-dark-bg-button");
        HBox.setMargin(createOfferButton, new Insets(-5, 0, 0, 0));

        HBox topBox = new HBox();
        topBox.setFillHeight(true);
        topBox.setSpacing(46);
        topBox.getChildren().addAll(headlineLabel, Spacer.fillHBox(), createOfferButton);
        topBox.setPadding(new Insets(0, 67, 2, 0));

        HBox.setMargin(headlineLabel, new Insets(-5, 0, 20, -2));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);


        line = new Region();
        line.getStyleClass().add("bisq-darkest-bg");
        double lineHeight = 1.5;
        line.setMinHeight(lineHeight);

        Pane lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line);
        lineAndMarker.setMinHeight(lineHeight);
        lineAndMarker.setPadding(new Insets(0, 67, 0, 0));

        root.getChildren().addAll(topBox, lineAndMarker, scrollPane);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                if (overlayWindow != null) {
                    overlayWindow.close();
                    overlayWindow = null;
                }
                if (model.showCreateOffer.get()) {
                    overlayWindow = new OverlayWindow(getRoot(), childRoot, controller::onCloseCreateOffer);
                    overlayWindow.show();
                } else if (model.showTakeOffer.get()) {
                    overlayWindow = new OverlayWindow(getRoot(), childRoot, controller::onCloseTakeOffer);
                    overlayWindow.show();
                } else {
                    childRoot.getStyleClass().add("bisq-content-bg");
                    childRoot.setPadding(new Insets(33, 67, 0, 0));
                    scrollPane.setContent(childRoot);
                }

            }
        };


        // addTab(Res.get("trade.offerbook"), NavigationTarget.OFFERBOOK);

        //   headlineLabel.setText(Res.get("trade"));
    }

    @Override
    protected void onViewAttached() {
      /*  UIThread.runOnNextRenderFrame(() -> {
            NavigationTarget navigationTarget = model.getNavigationTarget();
            if (navigationTarget != null) {
                Navigation.navigateTo(navigationTarget);
            }
        });*/
        createOfferButton.setOnAction(e -> controller.onOpenCreateOffer());
        line.prefWidthProperty().bind(root.widthProperty());
        model.getView().addListener(viewChangeListener);
    }

    @Override
    protected void onViewDetached() {
        createOfferButton.setOnAction(null);
        line.prefWidthProperty().unbind();
        model.getView().removeListener(viewChangeListener);

        // createOfferTabVisibleSubscription.unsubscribe();
        //  takeOfferTabVisibleSubscription.unsubscribe();
    }

/*    private void onCreateOfferTabVisibleChange(boolean value) {
        if (value) {
            addTab(Res.get("trade.createOffer"), NavigationTarget.CREATE_OFFER);
        } else {
            removeTab(NavigationTarget.CREATE_OFFER);
        }
    }

    private void onTakeOfferTabVisibleChange(boolean value) {
        if (value) {
            addTab(Res.get("trade.takeOffer"), NavigationTarget.TAKE_OFFER);
        } else {
            removeTab(NavigationTarget.TAKE_OFFER);
        }
    }*/
}

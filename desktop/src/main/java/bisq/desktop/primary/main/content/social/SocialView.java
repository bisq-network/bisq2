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

package bisq.desktop.primary.main.content.social;

import bisq.desktop.common.view.*;
import bisq.desktop.overlay.OverlayWindow;
import bisq.desktop.primary.main.content.social.gettingStarted.GettingStartedView;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocialView extends TabView<SocialModel, SocialController> {
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    
    public SocialView(SocialModel model, SocialController controller) {
        super(model, controller);

        addTab(Res.get("social.gettingStarted"), NavigationTarget.GETTING_STARTED);
        addTab(Res.get("social.discuss"), NavigationTarget.DISCUSS);
        addTab(Res.get("social.learn"), NavigationTarget.LEARN);
        addTab(Res.get("social.connect"), NavigationTarget.CONNECT);

        setupHeadline(model.getView()); // TODO: check why view is null at the beginning
        viewChangeListener = (observable, oldValue, newValue) -> setupHeadline(newValue);
    }

    @Override
    protected void onViewAttached() {
        model.getView().addListener(viewChangeListener);
    }

    @Override
    protected void onViewDetached() {
        model.getView().removeListener(viewChangeListener);
    }
    
    private void setupHeadline(Object view) {
        if (view instanceof GettingStartedView) {
            headlineLabel.setText(Res.get("welcome"));
            headlineLabel.getStyleClass().add("super-large-text");
            line.setOpacity(0);
        } else {
            headlineLabel.setText(Res.get("social"));
            headlineLabel.getStyleClass().remove("super-large-text");
            line.setOpacity(1);
        }
    }
}

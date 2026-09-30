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

package bisq.desktop.main.content.reputation.build_reputation;

import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class Tab2Controller<M extends Tab2Model, V extends Tab2View, S extends ScoreSimulation> implements Controller {
    @Getter
    private final V view;
    private final M model;
    private final S simulation;
    private Subscription tradeLimitHelpPin;

    public Tab2Controller(ServiceProvider serviceProvider) {
        model = createModel();
        simulation = createSimulation();
        view = createView(model, simulation);
    }

    protected abstract M createModel();

    protected abstract S createSimulation();

    protected abstract V createView(M model, S simulation);

    @Override
    public void onActivate() {
        tradeLimitHelpPin = EasyBind.subscribe(simulation.getTradeLimitHelp(),
                tadeLimitHelp -> model.getReducePadding().set(StringUtils.isNotEmpty(tadeLimitHelp)));
    }

    @Override
    public void onDeactivate() {
        tradeLimitHelpPin.unsubscribe();
    }

    public abstract void onBack();

    public abstract void onNext();

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }
}

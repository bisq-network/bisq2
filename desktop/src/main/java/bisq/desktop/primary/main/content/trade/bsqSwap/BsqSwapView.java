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

package bisq.desktop.primary.main.content.trade.bsqSwap;

import bisq.account.protocol.SwapProtocolType;
import bisq.desktop.primary.main.content.trade.ProtocolRoadmapView;

public class BsqSwapView extends ProtocolRoadmapView<BsqSwapModel, BsqSwapController> {
    public BsqSwapView(BsqSwapModel model, BsqSwapController controller) {
        super(model, controller);
    }

    @Override
    protected String getKey() {
        return SwapProtocolType.BSQ_SWAP.name();
    }

    @Override
    protected String getIconId() {
        return "protocol-bsq";
    }

    @Override
    protected String getUrl() {
        return "https://bisq.wiki/BSQ";
    }
}

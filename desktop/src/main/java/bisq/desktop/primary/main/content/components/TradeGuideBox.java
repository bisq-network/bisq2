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

package bisq.desktop.primary.main.content.components;

import bisq.common.data.Pair;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TradeGuideBox extends VBox {
    private final List<Pair<Label, Label>> labelItems = new ArrayList<>();
    @Getter
    private final Button next;
    @Getter
    private final Button back;
    @Getter
    private final Button closeButton;
    private int index;
    private Pair<Label, Label> visibleItem;

    public TradeGuideBox() {
        setSpacing(20);
        setAlignment(Pos.TOP_LEFT);
        setFillWidth(true);
        setPadding(new Insets(15, 30, 30, 30));
        getStyleClass().addAll("bisq-box-2");

        Label headlineLabel = new Label(Res.get("bisqEasy.privateTradeChannel.tradeInfo.headline")/*, ImageUtil.getImageViewById("onboarding-2-payment")*/);
        headlineLabel.getStyleClass().add("bisq-text-headline-4");

        closeButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(headlineLabel, Spacer.fillHBox(), closeButton);
        getChildren().add(hBox);

        List<Pair<String, String>> items = List.of(
                new Pair<>("bisqEasy.privateTradeChannel.tradeInfo.step0", "onboarding-1-reputation"),
                new Pair<>("bisqEasy.privateTradeChannel.tradeInfo.step1", "onboarding-3-profile"),
                new Pair<>("bisqEasy.privateTradeChannel.tradeInfo.step2", "onboarding-2-chat"),
                new Pair<>("bisqEasy.privateTradeChannel.tradeInfo.step3", "onboarding-2-payment"),
                new Pair<>("bisqEasy.privateTradeChannel.tradeInfo.step4", "onboarding-3-method")
        );
        for (Pair<String, String> item : items) {
            String resourceKey = item.getFirst();
            String iconId = item.getSecond();

            Label contentHeadlineLabel = new Label(Res.get(resourceKey + ".headline")/*, ImageUtil.getImageViewById(iconId)*/);
            //  headlineLabel.setGraphicTextGap(16.0);
            contentHeadlineLabel.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");
            // headlineLabel.setId("bisq-easy-onboarding-label");
            // headlineLabel.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");
            //  headlineLabel.setId("bisq-easy-trade-info-headline-label");
            contentHeadlineLabel.setWrapText(true);
            contentHeadlineLabel.setManaged(false);
            contentHeadlineLabel.setVisible(false);

            Label contentLabel = new Label(Res.get(resourceKey + ".content"));
            contentLabel.setId("bisq-easy-onboarding-label");
            // contentLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
            contentLabel.setWrapText(true);
            contentLabel.setManaged(false);
            contentLabel.setVisible(false);

            labelItems.add(new Pair<>(contentHeadlineLabel, contentLabel));

            VBox.setVgrow(contentLabel, Priority.ALWAYS);
            // VBox.setMargin(content, new Insets(0, 0, 0, 45));
            VBox.setMargin(contentHeadlineLabel, new Insets(0, 0, -10, 0));
            getChildren().addAll(contentHeadlineLabel, contentLabel);
        }
        next = new Button(Res.get("next"));
        next.setDefaultButton(true);
        back = new Button(Res.get("back"));
        back.setManaged(false);
        back.setVisible(false);
        HBox buttons = new HBox(10, back, next);

        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        getChildren().add(buttons);

        select(index);

        back.setOnAction(e -> {
            if (index > 0) {
                index--;
                select(index);
            }
        });
        next.setOnAction(e -> {
            if (index < labelItems.size() - 1) {
                index++;
                select(index);
            }
        });
    }

    private void select(int i) {
        if (visibleItem != null) {
            visibleItem.getFirst().setManaged(false);
            visibleItem.getFirst().setVisible(false);
            visibleItem.getSecond().setManaged(false);
            visibleItem.getSecond().setVisible(false);
        }
        visibleItem = labelItems.get(i);
        visibleItem.getFirst().setManaged(true);
        visibleItem.getFirst().setVisible(true);
        visibleItem.getSecond().setManaged(true);
        visibleItem.getSecond().setVisible(true);

        boolean first = index == 0;
        back.setManaged(!first);
        back.setVisible(!first);
        boolean last = index == labelItems.size() - 1;
        next.setManaged(!last);
        next.setVisible(!last);
    }
}
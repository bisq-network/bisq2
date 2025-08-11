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

package bisq.desktop.main.content.network.nodes.transport;

import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.platform.MemoryReportService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NumThreadStatistics {
    private final Controller controller;

    public NumThreadStatistics(ServiceProvider serviceProvider, TransportType transportType) {
        controller = new Controller(serviceProvider, transportType);
    }

    public Pane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final Model model;
        @Getter
        private final View view;
        private final MemoryReportService memoryReportService;
        private final Map<String, Pin> valuePinsByName = new HashMap<>();
        private Pin historicalNumThreadsByThreadNamePin, numThreadsInfoPin;

        private Controller(ServiceProvider serviceProvider, TransportType transportType) {
            memoryReportService = serviceProvider.getMemoryReportService();

            model = new Model(MemoryReportService.NUM_POOL_THREADS_UPDATE_INTERVAL_SEC, MemoryReportService.MAX_AGE_NUM_POOL_THREADS);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            historicalNumThreadsByThreadNamePin = memoryReportService.getHistoricalNumThreadsByThreadName()
                    .addObserver((poolName, value) -> UIThread.run(() -> onMapChanged(poolName, value)));

            numThreadsInfoPin = memoryReportService.getCurrentNumThreads().addObserver(currentNumThreads ->
                    UIThread.run(() -> {
                        model.getNumThreadsInfo().set(Res.get("network.transport.numThreadStatistics.numThreadsInfo",
                                currentNumThreads,
                                memoryReportService.getPeakNumThreads().get()));
                    }));
        }

        private void onMapChanged(String poolName, ObservableHashMap<Long, AtomicInteger> value) {
            ObservableMap<Long, AtomicInteger> map = model.getHistoricalNumThreadsByThreadName()
                    .computeIfAbsent(poolName, k -> FXCollections.observableHashMap());
            Pin existingPin = valuePinsByName.get(poolName);
            if (existingPin != null) {
                existingPin.unbind();
            }
            valuePinsByName.computeIfAbsent(poolName, k -> value.addObserver(() ->
                    UIThread.run(() -> {
                        map.clear();
                        map.putAll(value);
                    })));
        }

        @Override
        public void onDeactivate() {
            historicalNumThreadsByThreadNamePin.unbind();
            numThreadsInfoPin.unbind();
            valuePinsByName.values().forEach(Pin::unbind);
            valuePinsByName.clear();
            model.getHistoricalNumThreadsByThreadName().clear();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableMap<String, ObservableMap<Long, AtomicInteger>> historicalNumThreadsByThreadName = FXCollections.observableHashMap();
        private final int numPoolThreadsUpdateIntervalSec;
        private final long maxAgeNumPoolThreads;
        private final StringProperty numThreadsInfo = new SimpleStringProperty();

        private Model(int numPoolThreadsUpdateIntervalSec, long maxAgeNumPoolThreads) {
            this.numPoolThreadsUpdateIntervalSec = numPoolThreadsUpdateIntervalSec;
            this.maxAgeNumPoolThreads = maxAgeNumPoolThreads;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final LineChart<Number, Number> lineChart;
        private final Label numThreadsInfo;
        private UIScheduler scheduler;

        private View(Model model, Controller controller) {
            super(new VBox(15), model, controller);

            Label numThreadStatisticsHeadline = new Label(Res.get("network.transport.numThreadStatistics.headline"));
            numThreadStatisticsHeadline.getStyleClass().add("rich-table-headline");

            NumberAxis xAxis = new NumberAxis();
            xAxis.setTickUnit(1);
            xAxis.setMinorTickCount(0);
            xAxis.setLowerBound(1);
            xAxis.setUpperBound(model.getMaxAgeNumPoolThreads());
            xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis) {
                @Override
                public String toString(Number value) {
                    long totalSeconds = value.longValue();
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    return String.format("%02d:%02d", minutes, seconds);
                }
            });

            NumberAxis yAxis = new NumberAxis();
            yAxis.setTickUnit(1);
            yAxis.setMinorTickCount(0);
            yAxis.setAutoRanging(true);
            yAxis.setLabel(Res.get("network.transport.numThreadStatistics.yAxis.label"));
            yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
                @Override
                public String toString(Number value) {
                    return String.valueOf(value.intValue());
                }
            });

            lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setAnimated(false);
            lineChart.setCreateSymbols(false);
            lineChart.getStyleClass().add("network-details-view");
            lineChart.setPadding(new Insets(10, 20, 10, 10));

            numThreadsInfo = new Label();
            numThreadsInfo.getStyleClass().add("network-details-view");
            numThreadsInfo.setPadding(new Insets(10));
            numThreadsInfo.setAlignment(Pos.TOP_LEFT);
            numThreadsInfo.setMaxWidth(Double.MAX_VALUE);

            root.getChildren().addAll(numThreadStatisticsHeadline, lineChart, numThreadsInfo);
        }

        @Override
        protected void onViewAttached() {
            numThreadsInfo.textProperty().bind(model.getNumThreadsInfo());

            scheduler = UIScheduler.run(this::updateCharts).periodically(model.getNumPoolThreadsUpdateIntervalSec(), TimeUnit.SECONDS);
            updateCharts();
        }

        @Override
        protected void onViewDetached() {
            numThreadsInfo.textProperty().unbind();

            scheduler.stop();
            scheduler = null;
        }

        private void updateCharts() {
            long now = System.currentTimeMillis();
            List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();

            model.getHistoricalNumThreadsByThreadName().forEach((threadName, timeMap) -> {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName(threadName);

                timeMap.forEach((timestamp, numThreads) -> {
                    long passedTime = Math.round((now - timestamp) / 1000d);
                    series.getData().add(new XYChart.Data<>(passedTime, numThreads.get()));
                });

                if (!series.getData().isEmpty()) {
                    allSeries.add(series);
                }
            });

            lineChart.getData().setAll(allSeries);
        }
    }
}

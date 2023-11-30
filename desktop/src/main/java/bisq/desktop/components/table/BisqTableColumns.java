package bisq.desktop.components.table;

import bisq.i18n.Res;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import javax.annotation.Nullable;
import java.util.Comparator;

public class BisqTableColumns {

    public static <T extends DateTableItem> BisqTableColumn<T> getDateColumn() {
        return getDateColumn(null);
    }

    public static <T extends DateTableItem> BisqTableColumn<T> getDateColumn(ObservableList<TableColumn<T, ?>> sortOrder) {
        return getDateColumn(null, sortOrder);
    }

    public static <T extends DateTableItem> BisqTableColumn<T> getDateColumn(@Nullable String title,
                                                                             @Nullable ObservableList<TableColumn<T, ?>> sortOrder) {
        BisqTableColumn.Builder<T> builder = new BisqTableColumn.Builder<T>()
                .fixWidth(85)
                .comparator(Comparator.comparing(T::getDate))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getCellFactory());
        if (title != null) {
            builder.title(title);
        } else {
            builder.title(Res.get("temporal.date"));
        }
        BisqTableColumn<T> column = builder.build();
        if (sortOrder != null) {
            sortOrder.add(column);
        }
        return column;
    }

    public static <T extends DateTableItem> Callback<TableColumn<T, T>, TableCell<T, T>> getCellFactory() {
        return column -> new TableCell<>() {
            @Override
            public void updateItem(final T item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    Label date = new Label(item.getDateString());
                    date.getStyleClass().add("table-view-date-column-date");
                    Label time = new Label(item.getTimeString());
                    time.getStyleClass().add("table-view-date-column-time");
                    VBox vBox = new VBox(3, date, time);
                    vBox.setAlignment(Pos.CENTER);
                    setAlignment(Pos.CENTER);
                    setGraphic(vBox);
                    // vBox.setStyle("-fx-background-color: blue");
                } else {
                    setGraphic(null);
                }
            }
        };
    }
}

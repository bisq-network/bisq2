package bisq.desktop.components.table;

import javafx.collections.transformation.SortedList;

public class IndexColumnUtil {
    public static <T> BisqTableColumn<T> getIndexColumn(SortedList<T> sortedList) {
        BisqTableColumn.Builder<T> builder = new BisqTableColumn.Builder<T>()
                .fixWidth(40)
                .includeForCsv(false)
                .isSortable(false)
                .valueSupplier(item -> String.valueOf(sortedList.indexOf(item) + 1));
        BisqTableColumn<T> column = builder.build();
        column.setStyle("-fx-padding: 0;");
        return column;
    }
}

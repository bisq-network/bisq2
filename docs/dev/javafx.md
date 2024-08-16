# JavaFx Best practice

## List based containers

### Scaling huge collections

Collection containers like `ListView` and `TableView` use
a `VirtualFlow` [[1](https://openjfx.io/javadoc/12/javafx.controls/javafx/scene/control/skin/VirtualFlow.html)] under
the hood. Only those elements (cells) which are visible are created and updated.
At scrolling the disappearing cells get recycled for the newly added row cells. Thus, they are able to handle very large
lists.

`Flowless` [[2](https://github.com/FXMisc/Flowless)] is an alternative implementation to the JavaFX `VirtualFlow` with
performance improvements. We should have a look if it is feasible to use it.

### Cell factory

Here an example of a Table cell factory:

```
private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getCellFactory() {
    return column -> new TableCell<>() {
        private final Button button = new Button();

        {
            button.setDefaultButton(true);
        }

        @Override
        public void updateItem(final ListItem item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null && !empty) {
                button.setText(tiem.getButtonText());
                button.setOnAction(e -> controller.onCloseItem(item));
                setGraphic(button);
            } else {
                button.clear();
                button.setOnAction(null);
                setGraphic(null);
            }
        }
    };
}
```

We use as convention the ListItem type as the second type parameter (`<ListItem, ListItem>`) instead of using the
columns type as that gives more flexibility for feature extension of the behaviour of the cell.
Our customized BisqTableView is built on that convention and reduce the signature overhead of maintaining 2 types to
only one.

#### MVC pattern

The cell can be considered as the view in our MVC model.
The list item as the model and the controller of the view holding the ListView or TableView is the controller.
As the controller need to act on the list items context, method calls on the controller should pass the list item.

#### Notes

When a ListView or TableView is removed from stage the 'else' branch is not always called (also if the list is
cleared at that moment). Thus, we cannot rely on resource cleanup in the `else` branch of the cell factory.
Avoid using custom listeners or observers which require manual resource cleanup.
JavaFx Binding use weak listeners inside and therefore can be considered more or less safe, though there are some
edge cases where that listeners don't get removed and thus leading to memory leaks (weak listeners only get removed
at a change event which is not guaranteed to happen, thus can lead to memory
leaks). [[3](https://continuously.dev/blog/2015/02/10/the-trouble-with-weak-listeners.html)]
If you use weak listeners or references, be sure to pin down the actual listener, otherwise it get GC'ed and lead to
nasty bags.

Another unexpected behaviour is that the `updateItem` method is called more often as needed. Be sure that your code cope
with that accordingly.

#### Best practice for cell factories

- Always call the `super.updateItem(item, empty);`
- Always use the `if (item != null && !empty) {` and `else` branches. Keep it in that order for consistency.
- Create the UI controls or containers as final fields.
- Use static initializer for customizing the controls or containers.
- Always reset display data in the `else` branch. Otherwise, empty or recycled cells could display the content of
  previously used cells.
- Always dispose resources in the `else` branch.
- Access the data from fields of list item (not from the domain object inside the list item)
- For expensive operations use the list item for lazy data initialisation and consider caching.

### List items

The List items are just value objects and grow with the number of list elements. Therefore, we have to be careful with
memory usage and performance costs from complex operations.
If listeners are used they have to be disposed.
For expensive operations we should use lazy initialisation (e.g. get called from the cell on demand and cache the result
for next repeated calls).
List items can hold domain services and a reference to the controller.

#### Best practice for List items

- For any non-trivial use case create a dedicated list item instead of using the domain object directly.
- Do not create and store UI elements in the ListItem. Those have to be in the CellFactory as only there we benefit from
  the Virtual layout.
- Always use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
- Mark the identity defining fields with `@EqualsAndHashCode.Include`. Those are usually those domain objects which are
  passed in the constructor.
- Order constructor parameters so that the most relevant domain data are first, and then follow with services or other
  helper objects.
- Separate the defining fields from the other ones with a space at class and constructor as code format convention.
- Put the List item into the view class if it is rather small. Name it `ListItem` in that case. The View class gives
  the usage context about the domain.
- If stored as individual class use the domain name as prefix, e.g. `MarketListItem`.

### TODO

Apply those best practice to our code base. It is currently not following those in all cases.

[1] https://openjfx.io/javadoc/12/javafx.controls/javafx/scene/control/skin/VirtualFlow.html<br>
[2] https://github.com/FXMisc/Flowless<br>
[3] https://continuously.dev/blog/2015/02/10/the-trouble-with-weak-listeners.html

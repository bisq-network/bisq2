package bisq.desktop.components.controls;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class BisqPopupMenuItem {
    private final String title;
    private final Runnable action;

    public BisqPopupMenuItem(String title, Runnable action) {
        this.title = title;
        this.action = action;
    }
}
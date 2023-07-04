package bisq.desktop.common.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Styles {
    private final String normal;
    private final String hoover;
    private final String selected;
    private final String deactivated;

    public Styles(String normal, String hoover, String selected, String deactivated) {
        this.normal = normal;
        this.hoover = hoover;
        this.selected = selected;
        this.deactivated = deactivated;
    }
}

package de.jensd.fx.fontawesome;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

public enum AwesomeIcon {
    CHEVRON_SIGN_LEFT(FontAwesomeIcon.CHEVRON_CIRCLE_LEFT),
    CHEVRON_SIGN_RIGHT(FontAwesomeIcon.CHEVRON_CIRCLE_RIGHT),
    COPY(FontAwesomeIcon.COPY),
    EDIT(FontAwesomeIcon.EDIT),
    EDIT_SIGN(FontAwesomeIcon.PENCIL_SQUARE),
    EXCLAMATION_SIGN(FontAwesomeIcon.EXCLAMATION_CIRCLE),
    EXTERNAL_LINK(FontAwesomeIcon.EXTERNAL_LINK),
    EYE_CLOSE(FontAwesomeIcon.EYE_SLASH),
    EYE_OPEN(FontAwesomeIcon.EYE),
    INFO_SIGN(FontAwesomeIcon.INFO_CIRCLE),
    REMOVE_SIGN(FontAwesomeIcon.TIMES_CIRCLE),
    STAR(FontAwesomeIcon.STAR),
    WARNING_SIGN(FontAwesomeIcon.EXCLAMATION_TRIANGLE),
    YOUTUBE_PLAY(FontAwesomeIcon.YOUTUBE_PLAY);

    private final FontAwesomeIcon delegate;

    AwesomeIcon(FontAwesomeIcon delegate) {
        this.delegate = delegate;
    }

    FontAwesomeIcon delegate() {
        return delegate;
    }
}

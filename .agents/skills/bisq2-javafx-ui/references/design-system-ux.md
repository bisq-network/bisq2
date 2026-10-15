# Design System And UX

## CSS Sources

- `base.css`, `text.css`, `controls.css`, `containers.css`, `application.css`,
  `chat.css`, `user.css`, `bisq_easy.css`, `mu_sig.css`, `trade_apps.css`, `images.css`, `markets.css`
- Load order source: `apps/desktop/desktop/src/main/java/bisq/desktop/CssConfig.java`

## Core Visual Tokens

From `base.css`:
- `-bisq2-green: #56ae48`
- `-bisq2-yellow: #d0831f`
- `-bisq2-red: #d23246`
- `-bisq-white: #fafafa`
- `-bisq-dark-grey-20: #1c1c1c`
- `-bisq-dark-grey-40: #2b2b2b`
- `-bisq-dark-grey-50: #383838`
- `-bisq-mid-grey-20: #808080`

## Typography Anchors

From `text.css`:
- `bisq-text-headline-1`: 4.1em
- `bisq-text-headline-2`: 1.7em
- `bisq-text-headline-5`: 2.5em
- `bisq-text-1`: 1.25em
- `bisq-text-7`: 0.92em

Families:
- IBM Plex Sans, Light, Medium, SemiBold, Mono

## Spacing And Sizing Anchors

- `Layout.SPACING = 20`
- Tab side padding: `TabView.SIDE_PADDING = 40`
- Button padding: `5 32 5 32`
- Radius: 4px controls, 8px cards
- Table row height: 54px
- Table header height: 34px

## Interaction And Motion

Use `Transitions` + `ManagedDuration`.

Examples:
```java
Transitions.fadeIn(node);
Transitions.slideOutRight(node, onDone);
Transitions.blurStrong(owner, -0.5);
Transitions.removeEffect(owner);
```

Respect animation settings:
```java
Duration d = ManagedDuration.getHalfOfDefaultDuration();
if (Transitions.useAnimations()) {
    // animated path
} else {
    // immediate end state
}
```

## Elite JavaFX UX Practices

1. Render-frame discipline:
- Defer bounds/width-dependent layout to next frame.

2. Stable layout under async:
- Reserve space for loaders/status if jump is undesirable.

3. Keyboard determinism:
- Enter/Escape behavior should remain predictable regardless of focused text input.

4. Truncation discoverability:
- Add tooltip on truncated labels (`TooltipUtil.showTooltipIfTruncated(...)`).

5. Weak listeners in custom controls:
- Use weak listeners/handlers for long-lived controls.

6. Avoid inline style strings:
- Prefer reusable CSS classes except truly dynamic values.

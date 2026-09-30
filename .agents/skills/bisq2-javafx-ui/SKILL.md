---
name: bisq2-javafx-ui
description: Create production-ready JavaFX UI code following Bisq2's strict MVC architecture, design system, UX principles, and desktop UI automation harness workflow. Use this skill when building new UI components, views, controllers, overlays, modifying existing JavaFX interfaces, adding or validating automation selectors, or running JavaFX UI smoke/scenario checks. Triggers include UI feature requests, component creation, view implementation, navigation additions, form building, table/list views, overlay/dialog work, desktop UI harness work, and any JavaFX-related development in the Bisq2 desktop application.
---

# Bisq2 JavaFX UI Development

## Overview

Use this skill to implement or review JavaFX UI work in Bisq2 with repository-accurate patterns.

Primary goals:
- Ship UI that matches Bisq2 MVC architecture and navigation framework.
- Reuse existing controls, CSS classes, formatters, validators, and bindings.
- Prevent lifecycle leaks, threading bugs, and stale async UI updates.

## When to Use

Activate for:
- New Controller/Model/View triads.
- Tab or navigation-target additions.
- Overlay/dialog/wizard UI.
- Forms, validation, converters, table/list UIs.
- UI refactors and UI code reviews.

## Working Method

1. Identify task type (leaf view, navigation container, tab view, overlay, form, table).
2. Read only the matching reference file(s) from `references/` (see map below).
3. Implement with existing Bisq components and CSS classes first, custom controls only if necessary.
4. When the change affects user-visible interaction, add or update harness selectors and verify with the desktop UI automation harness.
5. Apply lifecycle cleanup and threading safeguards before finishing.
6. Run a self-review using `references/navigation-review-checklist.md`.

## Reference Map

- MVC architecture, lifecycle, caching, threading contract:
  - `references/architecture-lifecycle.md`
- Component templates, bindings, forms, tables, anti-patterns:
  - `references/component-patterns.md`
- Visual language, CSS tokens/classes, animation and advanced UX tactics:
  - `references/design-system-ux.md`
- Navigation changes, final review checklist, key source paths:
  - `references/navigation-review-checklist.md`
- Desktop UI automation harness selectors, binders, scenarios, and local verification:
  - `references/ui-automation-harness.md`
- Example dry-run audit (prompt + rubric + scored result):
  - `references/dry-run-audit-2026-02-19.md`

## Non-Negotiables

- Controller updates model state; view reacts via bindings.
- View never accesses domain services directly.
- Follow existing controller wiring style in this repo: use `View<..., ..., ConcreteController>` for view/controller typing.
- Do not introduce per-view action interfaces (e.g. `XxxActions`) unless that local module already uses that pattern.
- Cleanup everything on detach/deactivate (bindings, pins, subscriptions, handlers, schedulers).
- All cross-thread UI changes go through `UIThread` (or `FxBindings` that marshal automatically).
- For layout-dependent reads, defer with `UIThread.runOnNextRenderFrame(...)`.
- Use `addTab(...)` (not `createTab(...)`).
- Prefer `ManagedDuration` and `Transitions` for animation timing and animation-disable behavior.
- Pair visibility and layout participation (`setVisible` + `setManaged`) when toggling layout nodes.
- Use `Res.get(...)` for user-facing strings.
- Use converters for typed text bindings; avoid ad-hoc parsing in views.
- Keep automation selectors out of production views: expose package-private semantic accessors and bind selectors in `desktop-ui-harness-app`.
- Do not use JavaFX `Node.id` as the automation contract; the current harness uses scoped selectors (`scope/automationId`).

## Output Contract

When asked to generate UI code, produce:
- `XxxController` with domain interaction and lifecycle wiring.
- `XxxModel` with JavaFX properties (and only lightweight helper logic).
- `XxxView` with layout, bindings, event delegation, and full detach cleanup.

For navigation work, also include:
- `NavigationTarget` entries (with persistence flag decision).
- Parent `createController(...)` switch registration.
- Tab/menu wiring in the parent view when needed.

For UI changes with an interaction path, also include:
- Package-private semantic view accessors for controls the harness must address.
- A harness binder in `apps/desktop/desktop-ui-harness-app/src/main/java` under the same Java package as the production view.
- Binder registration in `DesktopAutomationViewObserver` and a binder unit test.
- Local verification commands or harness scenario steps used to exercise the change.

For reviews, prioritize:
- Regressions, leaks, race conditions, threading violations, navigation/caching mistakes.

# Desktop UI Harness (JavaFX)

This harness provides a Playwright-like loop for Bisq desktop UI work:

1. Start the real desktop app with deterministic settings.
2. Discover addressable UI nodes.
3. Trigger simple UI actions (`click`, `type`, `press-key`) over localhost.
4. Capture JavaFX scene screenshots for visual review.

It is intended for iterative UI development and user-perspective smoke checks.

## Build

```bash
./gradlew :apps:desktop:desktop-ui-harness-app:installDist
```

The wrapper script also expects common local tooling:

- `curl` for automation API calls
- `openssl` to generate the local automation token
- `lsof` or `nc` to find a free isolated P2P port
- `python3` to parse scenario files with shell-style quoting

## Start / Stop

```bash
make desktop-ui-start
make desktop-ui-status
make desktop-ui-stop
```

Equivalent direct script:

```bash
./scripts/desktop-ui-harness.bash start
./scripts/desktop-ui-harness.bash status
./scripts/desktop-ui-harness.bash stop
```

## Core Commands

List automatable UI nodes and validation state:

```bash
make desktop-ui-nodes
```

Validate that the current visible UI has no duplicate scopes/selectors:

```bash
./scripts/desktop-ui-harness.bash validate
```

Type into an input field:

```bash
make desktop-ui-type selector=chat-message-container/input text="test prompt"
```

Click a button:

```bash
make desktop-ui-click selector=chat-message-container/send
```

Wait until a node exists (optionally visible):

```bash
make desktop-ui-wait-node selector=splash/logo timeout_ms=30000 visible=true
```

Send a key to current focus (or to a target selector):

```bash
make desktop-ui-press-key key=ENTER
make desktop-ui-press-key key=ENTER selector=chat-message-container/input
```

Create a screenshot of the first showing JavaFX scene (saved under `/tmp/bisq2-ui-harness/artifacts` by default):

```bash
make desktop-ui-screenshot name=chat-after-send
```

Run a full UI scenario:

```bash
make desktop-ui-scenario file=scripts/scenarios/desktop-ui-smoke.scenario
```

## Runtime Design

The harness app launches the normal `DesktopApp` and registers a harness-owned `DesktopAutomationViewObserver` through the desktop module's production-neutral `ViewLifecycleObservers` registry before startup. When views attach to JavaFX scenes, the observer dispatches the view to harness-owned binder classes. Those binders attach automation scopes and ids to package-private semantic nodes exposed by the views.

The production desktop app keeps only the neutral integration points required for this:

- `ViewLifecycleObserver` and `ViewLifecycleObservers` in the desktop view framework
- package-private semantic view accessors for nodes the harness needs to address

The production `desktop-app` binary does not start the automation server and does not depend on the harness app. The harness app owns the automation server startup, selector strings, and selector metadata binding via `DesktopAutomationMetadata`.

View-specific binders live in `desktop-ui-harness-app` and deliberately use the same Java package as the production view they bind. This same-package pattern lets binders call package-private semantic accessors without making mutable JavaFX controls part of a public desktop API. The source files are split across modules, but the project runs on the classpath rather than JPMS modules, so package-private access works as intended.

The local automation server is started from the dedicated `desktop-ui-harness-app` module:

- Bind host/port: `127.0.0.1:18180` by default
- Token auth (required): header `X-Bisq-Automation-Token`
- Endpoints:
  - `GET /health`
  - `GET /nodes`
  - `GET /automation/validate`
  - `POST /screenshot?name=...`
  - `POST /action/click?selector=<scope>/<automationId>`
  - `POST /action/type?selector=<scope>/<automationId>&text=...`
  - `POST /action/pressKey?key=ENTER&selector=<scope>/<automationId>`
  - `POST /wait/node?selector=<scope>/<automationId>&timeoutMs=...&visible=true|false`

Normal `desktop-app` runs are unaffected because the automation server is no longer wired into the app module itself. The harness binary owns:

- `-Dbisq.desktopUiHarness.bind.host`
- `-Dbisq.desktopUiHarness.bind.port`
- `-Dbisq.desktopUiHarness.token`
- `-Dbisq.desktopUiHarness.artifacts.dir`
- `-Dbisq.desktopUiHarness.fx.timeoutMs`
- `-Dbisq.desktopUiHarness.window.width`
- `-Dbisq.desktopUiHarness.window.height`
- `-Dbisq.desktopUiHarness.stage.timeoutMs`

## Determinism Controls

The harness defaults to:

- fixed window size (`1440x900`) for stable screenshots
- isolated data dir (`/tmp/bisq2-ui-harness/data`)
- fresh data reset on each `start` (`HARNESS_RESET_ON_START=1`) for deterministic runs
- explicit stage attach timeout through the wrapper (`STAGE_TIMEOUT_MS`, default `45000`)
- isolated CLEAR P2P profile with auto-selected free local port (`19101-19250`)
- self-referenced CLEAR seed addresses (`127.0.0.1:<same-port>`) to avoid accidental external peers

Override through environment variables:

- `HARNESS_DIR`, `DATA_DIR`, `ARTIFACTS_DIR`, `APP_NAME`
- `AUTOMATION_HOST`, `AUTOMATION_PORT`
- `WINDOW_WIDTH`, `WINDOW_HEIGHT`, `P2P_PORT`, `HARNESS_RESET_ON_START`
- `HARNESS_NETWORK_OPTS` (optional explicit network profile for local clusters)

To connect the harness desktop to the local 3-node clearnet stack, prefer `HARNESS_NETWORK_OPTS` over ad-hoc JVM arguments:

```bash
HARNESS_NETWORK_OPTS="-Dapplication.network.supportedTransportTypes.0=CLEAR -Dapplication.network.configByTransportType.clear.defaultNodePort=18003 -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:18000 -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:18000" \
make desktop-ui-start
```

If you intentionally reuse the persistent local desktop identity, ensure no other desktop process is using the same app name or data dir:

```bash
HARNESS_RESET_ON_START=0 APP_NAME=bisq2_gui1 DATA_DIR=/tmp/bisq2-local-3node/desktop make desktop-ui-start
```

## Selector Model

- Harness selectors are now scoped: `<scope>/<automationId>`.
- `scope` is declared on a stable view/container root by harness-owned view binders.
- `automationId` is local to that scope and only needs to be unique within the scope.
- JavaFX `Node.id` is no longer the automation contract. It remains available for CSS/styling when needed.
- The server indexes all showing JavaFX windows/scenes, so scope names must be globally unique across the visible UI.

## Best Practices For Reliable UI E2E

- Prefer stable automation scopes and automation ids for interactive controls.
- Keep selector strings and `DesktopAutomationMetadata` calls in `desktop-ui-harness-app`.
- Do not add public view getters only for automation.
- Do not rely on generated/randomized JavaFX ids in tests.
- Keep user-facing text assertions separate from interaction selectors.
- Use a dedicated harness data dir for reproducible runs.
- Capture screenshots at key states and on failure paths.

## Recommended Developer Workflow

Use the harness as a short local feedback loop after JavaFX changes:

1. build the harness app
2. start the harness
3. inspect the current visible UI with `nodes`
4. add package-private semantic accessors and a harness binder for missing controls
5. validate the visible UI with `validate`
6. automate the interaction with `click`, `type`, `press-key`, or a scenario file
7. capture screenshots before/after the change for review

Typical loop:

```bash
./gradlew :apps:desktop:desktop-ui-harness-app:installDist
./scripts/desktop-ui-harness.bash start
./scripts/desktop-ui-harness.bash nodes
./scripts/desktop-ui-harness.bash validate
./scripts/desktop-ui-harness.bash scenario scripts/scenarios/desktop-ui-smoke.scenario
./scripts/desktop-ui-harness.bash screenshot after-change
./scripts/desktop-ui-harness.bash stop
```

## Adding Automation Selectors To A View

Automation selector declarations belong in the harness app, not in production view constructors. Do not use annotations for this contract: annotations still live in production source, are easy to treat as public test API, and add reflection or processing without improving the selector boundary. Use explicit semantic accessors plus harness-side binders instead.

If the target view does not expose a stable node yet, add a package-private semantic accessor to the view and keep the selector string in `desktop-ui-harness-app`. Name the accessor by user intent, not by the concrete widget type:

```java
TextInputControl messageInput() {
    return inputField;
}

Node sendMessageAction() {
    return sendButton;
}
```

Then add or update a binder in `desktop-ui-harness-app` under the same Java package as the production view:

```java
package bisq.desktop.main.content.chat.message_container;

import bisq.desktop_ui_harness_app.AbstractDesktopAutomationViewBinder;

public final class ChatMessageContainerAutomationBinder
        extends AbstractDesktopAutomationViewBinder<ChatMessageContainerView> {
    @Override
    public Class<ChatMessageContainerView> viewType() {
        return ChatMessageContainerView.class;
    }

    @Override
    public void bind(ChatMessageContainerView view) {
        scope(view.getRoot(), "chat-message-container");
        id(view.messageInput(), "input");
        id(view.sendMessageAction(), "send");
    }
}
```

Finally, register the binder in `DesktopAutomationViewObserver`:

```java
new ChatMessageContainerAutomationBinder()
```

Only bind automation metadata to stable, meaningful UI surfaces:

- view/container roots that act as a durable scope
- interactive controls such as inputs, buttons, toggles, and search boxes
- stable content containers that are useful for assertions

Do not bind automation metadata to:

- decorative icons
- transient wrappers added only for layout
- list cells or recycled table rows
- controls whose identity is based only on visible text

Resulting selectors:

- `chat-message-container/input`
- `chat-message-container/send`

Notes:

- scope names must be unique across all showing JavaFX scenes/windows
- automation ids only need to be unique inside their scope
- keep JavaFX `Node.id` for CSS only when needed; it is not the automation contract
- binder tests belong in `desktop-ui-harness-app/src/test/java` under the same Java package as the view so they can assert package-private semantic accessors
- production view tests may use the same package-private semantic accessors, but external modules should not depend on them

## Extension Checklist For Developers And Agents

When adding or changing harness selectors:

- inspect the current UI first with `./scripts/desktop-ui-harness.bash nodes`
- add only package-private semantic accessors to production views
- return `Node` for buttons/toggles/actions and `TextInputControl` for editable text inputs
- keep accessor names stable and intent-based, such as `messageInput`, `sendMessageAction`, or `nextAction`
- add the binder under `apps/desktop/desktop-ui-harness-app/src/main/java` using the same Java package as the production view
- keep all selector strings in the binder, never in the production view
- register the binder in `DesktopAutomationViewObserver`
- add a binder unit test under `apps/desktop/desktop-ui-harness-app/src/test/java` using the same Java package as the production view
- run `./gradlew :apps:desktop:desktop-ui-harness-app:test` before manual harness checks
- run `./scripts/desktop-ui-harness.bash validate` against the target screen before adding scenario steps

Do not add:

- public view getters just for automation
- annotations for selectors in production source
- `DesktopAutomationMetadata` calls in production source
- JavaFX `Node.id` values as automation selectors
- selectors for decorative, recycled, or text-only-identified nodes

## Selector Naming Rules

- use kebab-case
- name selectors by intent, not by appearance
- keep names stable across visual refactors
- avoid position-based names such as `left-button`
- avoid style-based names such as `green-button`

Good:

- `offer-book/search`
- `chat-message-container/send`
- `trade-wizard/next`

Bad:

- `main-button`
- `bottom-input`
- `highlighted-send`

## Working With Dynamic Content

The scoped selector model is designed for stable controls, not repeated list items.

Examples of dynamic content:

- chat message bubbles
- table rows
- offer cards in a scrollable list

Do not try to make recycled cells addressable with permanent selectors.
Instead:

- target the stable parent container
- assert on its rendered text/content
- add a dedicated container-specific automation action later if item-level interaction becomes necessary

## Writing Good Scenarios

Prefer small, task-focused scenario files over long end-to-end scripts.

Good scenario shape:

1. wait for a stable root or control
2. validate the scene
3. perform one logical user flow
4. capture one or two screenshots

Example:

```text
health
validate
wait-node splash/logo 30000 true
screenshot smoke-logo
```

Selectors such as `chat-message-container/input` are only valid after the app has reached a screen where that view is attached. Scenario files should explicitly navigate or wait for the required screen state before interacting with screen-specific controls.

Keep scenarios:

- deterministic
- independent from previous local state
- focused on one user intent

## Debugging Failures

When a harness action fails:

1. run `./scripts/desktop-ui-harness.bash status`
2. run `./scripts/desktop-ui-harness.bash validate`
3. run `./scripts/desktop-ui-harness.bash nodes`
4. capture a screenshot
5. inspect the harness log with `./scripts/desktop-ui-harness.bash logs`

Common causes:

- the selector does not exist in the current visible UI
- the control exists but is not visible yet
- duplicate scopes or automation ids make the scene invalid
- the UI changed but the scenario still targets the old selector
- the app is on a different screen/state than the scenario expects

Use `wait-node` before interacting with lazily rendered controls.
If `validate` fails, fix the selector metadata before adding more automation steps.

## Security And Operational Boundaries

The harness is a local development tool, not a production runtime feature.

- it binds to loopback only
- it requires a token
- it writes screenshots into a local artifacts directory
- it is expected to run against local developer environments and CI, not user desktops

Do not expose the harness port outside localhost.
Do not use production secrets or persistent user data in harness scenarios.

## Scenario Format

Scenario files are line-based command scripts with optional comments:

- blank lines and lines starting with `#` are ignored
- each command is the same as the harness CLI command
- arguments use shell-style tokenization; quote multi-word text
- supported commands:
  - `health`
  - `validate`
  - `nodes`
  - `wait-node <selector> [timeout_ms] [visible]`
  - `click <selector>`
  - `type <selector> <text...>`
  - `press-key <key> [selector]`
  - `screenshot <name>`
  - `sleep <ms>`

Example (`scripts/scenarios/desktop-ui-smoke.scenario`):

```text
health
validate
wait-node splash/logo 30000 true
screenshot smoke-logo
```

## Known Limits

- `click` guarantees `ButtonBase` fire behavior; non-button nodes receive synthetic primary mouse press/release/click events after focus is requested.
- `type` supports `TextInputControl`.
- `press-key` dispatches `KEY_PRESSED`/`KEY_RELEASED` for one `KeyCode`; the HTTP API supports modifier query params, but the wrapper exposes only the key and optional selector.
- `screenshot` captures the first showing JavaFX scene returned by the server's window ordering, with overlays before the primary stage.
- For complex gestures (drag/drop, keyboard chords, context menus), extend the automation server APIs.

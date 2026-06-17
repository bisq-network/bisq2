# Desktop UI Automation Harness

Use this reference when JavaFX work changes an interactive path, needs visual smoke verification, or requires new automation selectors.

## Current Model

- The harness is the dedicated `desktop-ui-harness-app`; the production `desktop-app` does not start the automation server.
- Build it before use:
  ```bash
  ./gradlew :apps:desktop:desktop-ui-harness-app:installDist
  ```
- Start/stop and inspect with:
  ```bash
  make desktop-ui-start
  make desktop-ui-status
  make desktop-ui-nodes
  make desktop-ui-validate
  make desktop-ui-stop
  ```
- Direct script equivalents live in `scripts/desktop-ui-harness.bash`.
- The wrapper starts a deterministic desktop by default: fixed `1440x900` window, isolated data under `/tmp/bisq2-ui-harness`, fresh data on each start, and isolated CLEAR networking with an auto-selected local P2P port.

## Selector Contract

- Selectors are scoped strings: `<scope>/<automationId>`, for example `chat-message-container/input`.
- JavaFX `Node.id` is not the automation contract. Use it only for CSS/styling when needed.
- Scopes must be globally unique across all showing JavaFX scenes/windows.
- Automation ids only need to be unique inside their scope.
- Use `make desktop-ui-nodes` to inspect addressable nodes and `make desktop-ui-validate` to catch duplicate or invalid selector metadata.

## Adding Selectors

Keep selector metadata in the harness app, not in production view constructors.

1. Add package-private semantic accessors to the production view when a stable node is not exposed:
   ```java
   TextInputControl messageInput() {
       return inputField;
   }

   Node sendMessageAction() {
       return sendButton;
   }
   ```
2. Add or update a binder in `apps/desktop/desktop-ui-harness-app/src/main/java` under the same Java package as the production view:
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
3. Register the binder in `DesktopAutomationViewObserver`.
4. Add a binder unit test in `apps/desktop/desktop-ui-harness-app/src/test/java` under the same Java package.
5. Run:
   ```bash
   ./gradlew :apps:desktop:desktop-ui-harness-app:test
   ```

Do not add public view getters, annotations, `DesktopAutomationMetadata` calls in production source, or selectors for decorative/recycled/text-only nodes.

## Local Interaction Loop

Use the harness after UI changes:

```bash
./gradlew :apps:desktop:desktop-ui-harness-app:installDist
make desktop-ui-start
make desktop-ui-nodes
make desktop-ui-validate
```

Use commands like these once the target screen is visible:

```bash
make desktop-ui-wait-node selector=splash/logo timeout_ms=30000 visible=true
make desktop-ui-click selector=chat-message-container/send
make desktop-ui-type selector=chat-message-container/input text="test prompt"
make desktop-ui-press-key key=ENTER selector=chat-message-container/input
make desktop-ui-screenshot name=after-change
make desktop-ui-stop
```

Prefer screenshots for visual review after meaningful layout or styling changes.

## Scenarios

Run the default first-run smoke scenario:

```bash
make desktop-ui-scenario file=scripts/scenarios/desktop-ui-smoke.scenario
```

The default smoke scenario expects fresh harness data, accepts the two-step TAC flow, creates `SmokeUser`, waits for `left-nav/dashboard`, and captures `smoke-main`.

Scenario files support:

- `health`
- `validate`
- `wait-node <selector> [timeout_ms] [visible]`
- `click <selector>`
- `type <selector> <text...>`
- `press-key <key> [selector]`
- `screenshot <name>`
- `sleep <ms>`

Use quoted text in scenario files when the typed text contains spaces.

## Persistent And Local-Network Runs

For iterative work with an existing profile:

```bash
HARNESS_RESET_ON_START=0 APP_NAME=bisq2_gui1 DATA_DIR=/tmp/bisq2-local-3node/desktop make desktop-ui-start
```

When connecting to the local 3-node clearnet stack, prefer `HARNESS_NETWORK_OPTS` instead of ad-hoc JVM args:

```bash
HARNESS_NETWORK_OPTS="-Dapplication.network.supportedTransportTypes.0=CLEAR -Dapplication.network.configByTransportType.clear.defaultNodePort=18003 -Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:18000 -Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:18000" \
make desktop-ui-start
```

## Verification Checklist

- `./gradlew :apps:desktop:desktop-ui-harness-app:test`
- `bash -n scripts/desktop-ui-harness.bash` when editing the wrapper
- `make desktop-ui-validate` on the target screen
- Targeted `click`/`type`/`press-key` commands or a scenario for the changed workflow
- Screenshot artifact for visual changes

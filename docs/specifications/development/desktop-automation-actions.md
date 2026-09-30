# Desktop automation actions

The desktop automation server runs only in the dedicated harness application. Requests require its
token and execute UI actions on the JavaFX application thread.

When a request times out or its waiting thread is interrupted, the server cancels any UI action that
has not started. An abandoned queued click, text input, or key press must not execute when the UI
thread becomes available later. This prevents a retry from executing both the abandoned action and
the new action.

Cancellation must not interrupt the JavaFX thread. An action which already started may finish after
the request times out; the timeout does not establish whether that action changed application state.
The caller must inspect the current state before retrying an action with side effects.

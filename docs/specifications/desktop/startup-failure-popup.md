# Popup of a failed start

This specification defines what the desktop application shows when the start of its services fails.

## Rules

1. A failed start shows one popup. The start of the services fails when one of its steps fails, or when it does not
   complete within the startup timeout.
2. The popup of a failed start shows the error and offers a bug report. Closing it shuts the application down.

## Reasons

- Rule 1: a second popup would cover the first one and hide the error. Before this rule, a failed start also showed
  the warning "Initialising applicationService failed." without any detail, on top of the popup with the error.

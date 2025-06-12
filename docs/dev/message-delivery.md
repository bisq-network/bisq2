## Overview of the Message Send Process and Delivery States

When sending a direct message, we display a message delivery state via icons and tooltips. The message transmission process consists of several steps, each of which may fail. Most messages implement an ACK protocol: when a user receives an `AckRequestingMessage`, they must respond with an `AckMessage`. This feedback mechanism confirms successful delivery to the sender.

Messages are stored locally upon sending, so that in case of failure, a resend can be attempted.

Below is an overview of the possible message delivery paths:

---

### Both Users Online

1. Establish connection if not already active (`CONNECTING`)
2. Attempt to send the message (`SENT`)
3. Send does not fail; waiting for ACK
4. ACK message received (`ACK_RECEIVED`)

---

### Peer Just Went Offline

1. Use an existing, still-active connection
2. Attempt to send the message (`SENT`)
3. Send fails
4. Attempt to send via mailbox (`TRY_ADD_TO_MAILBOX`)
5. Once enough successful broadcast responses are received, update state to `ADDED_TO_MAILBOX`
6. When the peer comes online (or if they were online but failed to receive the direct message), they receive the mailbox message and send back an ACK
7. ACK received (`MAILBOX_MSG_RECEIVED`)

---

### Peer is Offline

1. Attempt to establish a connection, which fails (`CONNECTING`)
2. Attempt to send via mailbox (`TRY_ADD_TO_MAILBOX`)
3. Once enough successful broadcast responses are received, update state to `ADDED_TO_MAILBOX`
4. When the peer comes online (or was online but missed the message), they receive the mailbox message and respond with an ACK
5. ACK received (`MAILBOX_MSG_RECEIVED`)

---

### Failure Due to Fast App Close While Connecting

1. Attempting to establish a connection (`CONNECTING`)
2. The user sending the message closes the app before sending completes; message is stored locally
3. On next app start, failed sends are reloaded from local store
4. Since state is `CONNECTING`, a resend button is shown and an automatic delayed resend is attempted

---

### Failure Due to Fast App Close While Adding to Mailbox

1. Attempting to connect, which fails (`CONNECTING`)
2. Attempting to send via mailbox (`TRY_ADD_TO_MAILBOX`)
3. The user sending the message closes the app; message is stored locally
4. On next app start, failed sends are reloaded from local store
5. Since state is `TRY_ADD_TO_MAILBOX`, a resend button is shown and a delayed resend is attempted

---

### Other Failure Cases

- If sending fails at any stage, status is marked as `FAILED`, and a resend button is shown.
- If no ACK is received after the `SENT` state (assuming the peer is online), we attempt a resend and display the resend button on next app start.

---

### Delivery Status Enum

```
enum MessageDeliveryStatus {
    CONNECTING,
    SENT,
    ACK_RECEIVED,
    TRY_ADD_TO_MAILBOX,
    ADDED_TO_MAILBOX,
    MAILBOX_MSG_RECEIVED,
    FAILED;
}

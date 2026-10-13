# Secrets in logs

This specification defines that a node never writes a secret to its log, and how the node logs the commands and
replies of the Tor control port, which carry secrets.

A secret is data which lets somebody act as the node or as one of its identities, or control a local service of the
node:

- a private key: an EC private key, the private key of an onion service (Tor), the private key of an I2P destination;
- the control password of the Tor control port, and the auth cookie of an external Tor.

## Problem

Users share their log files. The error popup has a button which zips `bisq.log` and the Tor log for a bug report.
Users also attach logs to issues and send them to support.

From version 2.1.3 to version 2.1.13, the node wrote every Tor control command to the log at INFO and hid only the
data of `AUTHENTICATE`. The command `ADD_ONION` contains the private key of the onion service. The node sends it at
every start, once for the default identity and once for each user profile. Seed nodes and oracle nodes use the same
code. So every log file contained the onion private keys of all identities of the node.

Whoever has the private key of an onion service can publish the same onion service and can sign the proof of the onion
address in the connection handshake. So that person can connect to peers as that address and can receive messages
which peers send to it. The onion key of an identity does not change, so an old log file stays dangerous while the
identity exists.

The API of the same versions logged at INFO each message which a client sent through the WebSocket and each event which
the node sent to a subscriber. From version 2.1.9 on, it also logged the body of each REST response which it forwarded
through the WebSocket. Some error lines contain a request or its body. The API runs on the node of Bisq Connect, where
the WebSocket is enabled by default, and on the desktop when the user enables it. These lines contain the EC private key
of a user profile in two cases:

- the key material of a new profile, which `GET /user-identities/key-material` returns and `POST /user-identities`
  sends back;
- the key bundle of the identity in each trade and trade chat, which the topic of trades sends to each subscriber. The
  key bundle also contains the onion private key, and from version 2.1.8 on the I2P private key.

Whoever has the EC private key of a profile can sign as the profile and can decrypt its confidential messages, for
example trade chats with payment details. That is worse than an onion key. The lines also contain trade, chat and
payment account data. On a node without the API, the EC private keys were not in the log.

## Requirements

1. A node never writes a secret to a log, at any log level. This includes the message of an exception, because the
   callers log exception messages, and the `toString` of an object which holds a secret.
2. **Tor control commands.** The log form of a command keeps the command keyword and only arguments which are known to
   be public. The arguments of every other command are replaced by `[hidden]`:
   - `ADD_ONION`: the log keeps the key type and the port mappings and hides the key, for example
     `ADD_ONION ED25519-V3:[hidden] Port=9999,12345`. Other arguments, for example client authorization data, are
     hidden.
   - `AUTHENTICATE`: the password or the auth cookie is hidden: `AUTHENTICATE [hidden]`.
   - `SETCONF`: the arguments are hidden, because the value of an option can be a password.
   - `GETINFO`, `HSFETCH`, `RESETCONF`, `SETEVENTS`, `TAKEOWNERSHIP`: the arguments are info keys, onion service IDs,
     option names and event names. They are public, so the log keeps them.
3. **Tor control replies.** The log form of a reply line keeps the status code and the status text, for example
   `250 OK` or an error message of Tor, and keeps asynchronous events, for example `650 HS_DESC ...`. Reply data of the
   form `Key=Value` keeps its value only if the key is known to have a public value (`ServiceID`,
   `net/listeners/socks`). Otherwise the value is hidden, for example `250-PrivateKey=[hidden]`. A line without a
   status code is a line of a multi-line data reply. It has no fixed form, so it is hidden.
4. The exception of a failed Tor control command contains only the log forms of the command and of the reply. The
   exception of a failed `SETCONF` names only the option.

## Reasons

- Rules 2 and 3 list what is public instead of what is secret. The earlier rule hid only commands which contain
  `AUTHENTICATE`, and `ADD_ONION` was missing from it. With a list of public parts, a new command does not leak a
  secret if its author does not think about the log.
- `ADD_ONION` keeps the key type and the port mappings, because they help support and contain no secret. The key
  argument is handled by its position, not by searching for `=`: the base64 form of a key ends with `=`.
- Rule 3 hides reply data by default, because Tor returns keys which it generates as reply data: `PrivateKey=` after
  `ADD_ONION NEW:...`. Bisq always sends its own key, so Tor does not send this line today, but a change of the command
  must not leak a key.
- Rule 3 keeps status texts, because they help support. They are messages of Tor. For the commands which Bisq sends,
  they do not contain a secret: the error replies to `AUTHENTICATE` and `ADD_ONION` are fixed texts without the
  password, the cookie or the key (checked in the source of Tor 0.4.9). Bisq does not subscribe to events which carry
  secrets.

## Log files of earlier versions

Versions 2.1.3 to 2.1.13 wrote the onion private keys of all identities into the log files, and their API wrote the
private keys of user profiles (see Problem). Log files are appended across versions, and the rolled files
`bisq_<n>.log` keep their content for up to 20 rollovers.

At its start, before it opens its log file, a node hides these secrets in `bisq.log` and the rolled files
`bisq_<n>.log` of its data directory. It replaces with `[hidden]`:

1. The key of each `ADD_ONION` command, as the log form of `ADD_ONION` does. The key type stays.
2. The rest of each line in which the API of these versions logged a message, an event or a REST body. The message of
   such a line starts with one of these texts, after the class name of the logger:
   - `WebSocketConnectionHandler: Received message ` or `No service found for handling message: `
   - `WebSocketRestApiRequest: Json deserialization failed. Message=`, and the same message of `SubscriptionRequest`
   - `WebSocketRestApiService: REST API call failed for request: `, `httpResponse WebSocketRestApiResponse(`, or
     `Error at sending a '<method>' request to '<url>' with body: `
   - `BaseWebSocketService: Sending json with modificationType <...>. json=`
   - `Subscriber: Send json: ` or `SubscriptionService: Send SubscriptionResponse json: `
3. The rest of the line after `Message was not sent to websocket.` of `WebSocketRestApiService`. It starts with
   `Json=` and contains a request.
4. In any other line, the value of a private key in the JSON of the API: `"privateKey":{"encoded":...}` of a key pair,
   `"privateKeyEncoded"` of an onion key and `"identityBytes"` of an I2P key, also in JSON which is escaped within
   other JSON.

It keeps all other bytes. It writes a new file and then replaces the old one, so a failure leaves the old file complete.
A file without a secret stays unchanged. A failure does not stop the start: the node logs a warning which names the
file, and it tries again at the next start. After a start without failure, the node writes the file
`legacy_log_secrets_hidden_v<n>` into the data directory, where `<n>` is the version of the patterns. It does not check
the files again until a later version adds a pattern and increases `<n>`.

When the desktop app builds the zip file of the error report, it also replaces each such secret in the copies of the
log files. This covers a secret which the start could not hide.

Reasons:

- The keys stay valid, and users share their log files with support and in public issues. The first start of this
  version is the moment when users of earlier versions share logs.
- The node rewrites the files before the logger opens them. Windows cannot replace a file which the process holds
  open, and on other systems the logger would keep writing into the replaced file.
- The files are kept and not deleted, because support needs them.
- Rule 2 hides the whole rest of the line, because the payload also contains trade, chat and payment account data,
  which users should not share either. The class name and the start of the message stay, so support can still see
  what the node did. The class name is part of the match, so a line of another class with the same text stays
  unchanged. The texts come from all log statements of the API in versions 2.1.3 to 2.1.13. The line
  `httpResponse (GET <url>) <status>` of versions 2.1.3 to 2.1.8 contains no body and stays.
- Rule 3: a line break in the message puts the request into the next line. No other message of these versions starts
  a line with `Json=`.
- Rule 4 covers a line form which rules 2 and 3 do not know. The field names are those of the DTOs of the API in all
  these versions.
- The pairing codes which versions 2.1.9 to 2.1.13 logged stay. In these versions a code expires one hour after it
  was created, unless the operator configured a longer time (`ttlInSeconds`), so a code in an old log is no longer
  valid.
- The check runs once per version of the patterns, because it reads up to 21 files of 10 MB. A version which hides a
  further secret has to check the files again, also in data directories which an earlier version already checked. So
  the name of the marker carries the version.

## Compatibility

The rules change only the log, not the network protocol or persisted data.

The node cannot reach copies of the old log files outside its data directory: logs which users shared or attached to
issues, zip files of error reports of earlier versions, backups, the console output of seed, oracle and API nodes in the
systemd journal or in `docker logs`, and old blocks on the disk. A node which a user downgrades to an earlier version
writes the keys again, and the node does not check the files again after the next upgrade, unless the version of the
patterns increased. Users who shared such a log, and operators of seed and oracle nodes, need a decision about the
rotation of onion keys. A new onion key changes the network address of the identity. Users of the API whose log was
shared need a decision about their profiles: the EC key of a profile is its identity, so only a new profile has a new
key.

## Related specifications

- [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md), rule 7: exception messages of
  integrity rules do not contain the rejected value, because users share their log files.

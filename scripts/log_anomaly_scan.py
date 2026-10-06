#!/usr/bin/env python3
"""
Scan the log files of Bisq 2 nodes for failures which the user interface does not show.

A node logs some failures without showing them on screen: entries which a store skips at load, network data which a
store rejects, messages which a node drops, and inventory responses which reach the size limit. A tester who only
watches the screen does not see them. This script lists such lines for each node and each application session. Use it
after a manual test session, and in automated tests which start nodes.

A PATH is a data directory, a log file, or a directory whose subdirectories are data directories. In a data directory
the script reads `bisq.log` and the rolled files `bisq_1.log` to `bisq_20.log`, oldest first.

Each start of the application begins a new session. The script reads the version of each session from the line
`Version: v<version> / Commit hash: <hash>`. So a data directory which an old version and then a new version used
shows the findings of each version separately.

An allowlist removes lines which are expected and harmless, for example the warning of a node of version 2.1.13 about
the unknown feature of newer peers. The report counts the allowed lines, so that a tester still sees how often they
occurred.

Usage:
    python3 scripts/log_anomaly_scan.py [options] PATH [PATH ...]

Examples:
    python3 scripts/log_anomaly_scan.py ~/.local/share/Bisq2
    python3 scripts/log_anomaly_scan.py --last-session --include-warnings ~/.local/share/bisq2_alice
    python3 scripts/log_anomaly_scan.py --json /tmp/bisq2-local-3node

Exit codes:
    0  no finding at or above the level of --fail-on
    1  at least one finding at or above the level of --fail-on
    2  a usage error, or no log file found
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, Iterator, List, Optional, Pattern, Tuple

SEVERITIES = ("notice", "warning", "error")
DEFAULT_MAX_EXAMPLES = 3
DEFAULT_INVENTORY_REPEAT_THRESHOLD = 5
MAX_ROLLED_LOG_INDEX = 20
MAX_EXAMPLE_LINE_LENGTH = 400
MAX_EXAMPLE_CONTINUATION_LINES = 3

# Pattern of LogSetup: %d{MMM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{15}: %mask(%msg) %xEx%n
# The month name depends on the locale of the JVM, so any token is accepted.
ENTRY_START = re.compile(
    r"^(?P<timestamp>\S+-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(?P<thread>.*?)\] "
    r"(?P<level>ERROR|WARN|INFO|DEBUG|TRACE) +(?P<logger>\S+?): ?(?P<message>.*)$")
SESSION_START = re.compile(r"^Version: v(?P<version>\S+) / Commit hash:")
ROLLED_LOG_FILE = re.compile(r"^bisq_(?P<index>\d+)\.log$")


@dataclass(frozen=True)
class Rule:
    rule_id: str
    severity: str
    pattern: Pattern[str]
    description: str


@dataclass(frozen=True)
class AllowRule:
    rule_id: str
    pattern: Pattern[str]
    reason: str
    # If set, the rule applies only to sessions of a lower version. A session without a known version is not allowed.
    only_before_version: Optional[Tuple[int, ...]] = None


def _rule(rule_id: str, severity: str, pattern: str, description: str) -> Rule:
    return Rule(rule_id, severity, re.compile(pattern), description)


# The first matching rule wins. The rules are matched against the first line of a log entry, which holds the level,
# the logger and the first line of the message.
RULES: List[Rule] = [
    # Data which a node loses without notice
    _rule("store-entry-skipped", "error",
          r"We skip an entry which we cannot parse",
          "An entry failed to parse or failed a check and was skipped. A store deletes a skipped entry at its next "
          "write."),
    _rule("store-entries-skipped-count", "error",
          r"We skipped \d+ of \d+ entries which we cannot parse",
          "Count of the skipped entries of one list or store."),
    _rule("store-file-unreadable", "error",
          r"Couldn't read .+ from file",
          "A store file could not be read. The node tries its backups and moves the file to corruptedFilesAtRead."),
    _rule("store-restore-from-backup", "error",
          r"Trying to restore from backup file",
          "The main file of a store could not be read, so the node restores a backup."),
    _rule("store-corrupted-file-not-moved", "error",
          r"Error trying to backup corrupted file",
          "A store file which could not be read could also not be moved to corruptedFilesAtRead."),

    # Data or messages which a node rejects or drops
    _rule("storage-add-signature-invalid", "warning",
          r"AddAuthenticatedDataRequest was not added to store\. Result=.*signatureInvalid=true",
          "A store rejected an add request with an invalid signature. In a network with nodes of version 2.1.13 or "
          "older, a known cause is the add request which such an owner stores after a refresh (see "
          "docs/specifications/network/storage-refresh-requests.md). Other causes are a changed request or different "
          "encodings of the signed data."),
    _rule("storage-request-rejected", "warning",
          r"(?:Add|Remove)\w*Request was not added to store\. Result=",
          "A store rejected a request because of a severe failure. The result lists the reason."),
    _rule("storage-refresh-rejected", "warning",
          r"RefreshAuthenticatedDataRequest was not applied\. Result=",
          "A store rejected a refresh request because of a severe failure. The result lists the reason."),
    _rule("network-message-unparsable", "warning",
          r"We drop a message which we cannot parse",
          "A node received a network message which it cannot parse."),
    _rule("connection-authorization-failed", "warning",
          r"\bAUTHORIZATION_FAILED\b",
          "A connection was closed because the authorization of a message failed."),
    _rule("enum-unresolved", "warning",
          r"Enum could not be resolved|Could not resolve enum for proto|Enum name is null",
          "A node received an enum value which it does not know. Nodes of older versions log this for new values."),
    _rule("feature-list-unresolved", "warning",
          r"The size of the resolved feature list is not same as the protobuf list's size",
          "A node received a feature which it does not know."),
    _rule("trade-message-dropped", "warning",
          r"We drop a trade message|We drop a \w+ because .*tradeId=",
          "A node dropped a trade message."),
    _rule("trade-message-ignored", "warning",
          r"Ignoring inbound trade message as trading is currently not allowed",
          "A node ignored a trade message because an alert restricts trading."),
    _rule("take-offer-rejected", "warning",
          r"We reject the take offer request",
          "The maker rejected a take offer request. The stack trace names the failed check."),
    _rule("take-offer-mediators-mismatch", "warning",
          r"Mediators do not match",
          "Maker and taker selected different mediators."),
    _rule("take-offer-prices-not-same-time", "warning",
          r"Market prices are not from the same time",
          "The maker could not check the reputation of the taker, because its market prices are too far apart."),
    _rule("take-offer-price-deviation", "warning",
          r"\bPRICE_DEVIATION\b",
          "A trade failed because of a price deviation."),
    _rule("trade-chat-channel-mismatch", "warning",
          r"Trade chat channel does not match",
          "A node failed a trade because its chat channel does not match the trade."),
    _rule("oracle-registrations-unverifiable", "warning",
          r"cannot be verified by the Bisq 1 bridge",
          "The oracle node keeps registrations which the Bisq 1 bridge cannot verify."),
    _rule("oracle-registrations-unsupported-protocol", "warning",
          r"with unsupported protocol versions",
          "The oracle node keeps registrations of a protocol version which it does not support."),
    _rule("inventory-peer-given-up", "warning",
          r"Our filter lists none of the \d+ entries of the inventory from",
          "A node stopped requesting inventory from a peer whose responses stay at the size limit."),

    # Lines which are normal alone, but which a tester should count
    _rule("inventory-response-truncated", "notice",
          r"maxSizeReached=true|Response got truncated because max size was reached",
          "An inventory response reached the size limit. The requester asks again."),
    _rule("publisher-migration-broadcast", "notice",
          r"We broadcast \d+ requests to migrate the publisher of our data in store",
          "An oracle or seed node published its data again under its authorized key."),
]

ERROR_LINE_RULE = Rule("error-line", "error", re.compile(r""), "A line with level ERROR which no other rule matches.")
WARNING_LINE_RULE = Rule("warning-line", "warning", re.compile(r""),
                         "A line with level WARN which no other rule matches.")
INVENTORY_REPEATS_RULE_ID = "inventory-truncation-repeats"
INVENTORY_REPEATS_DESCRIPTION = ("Inventory responses reached the size limit many times in one session. A node of "
                                 "version 2.1.13 or older repeats its request on the same connection while the "
                                 "response is full, so it may never receive the rest, such as mailbox messages.")

# Allowed lines are matched against the whole entry, including its stack trace. Allow only lines whose cause is known
# and harmless. Lines which have a harmless cause and also a harmful one stay findings and explain the known cause in
# their description.
ALLOW_RULES: List[AllowRule] = [
    AllowRule("error-line", re.compile(r"LinuxNotificationService: Copying notificationIcon from resources failed"),
              "The api-app has no notification icon in its resources (v2.1.13 and v2.1.14 on Linux). It affects only "
              "the icon of a desktop notification."),
    AllowRule("feature-list-unresolved", re.compile(r"resolved feature list"),
              "Nodes of version 2.1.13 and older do not know the feature INVENTORY_HASH_SET_V2 of newer peers.",
              only_before_version=(2, 1, 14)),
    AllowRule("enum-unresolved", re.compile(r"Enum type= Feature; name=UNRECOGNIZED"),
              "Nodes of version 2.1.13 and older do not know the feature INVENTORY_HASH_SET_V2 of newer peers.",
              only_before_version=(2, 1, 14)),
    # Only the paths which recover from the failed send are allowed: the confidential send path stores the message in
    # the mailbox of the peer, and the requester of an inventory asks again. The same error on another path can lose
    # data, so it stays a finding.
    AllowRule("error-line",
              re.compile(r"Send executor rejected task.*ConfidentialMessageService\.trySendAsync", re.DOTALL),
              "A node sent a confidential message over a connection which the peer had closed. When two nodes open "
              "connections to each other at the same time, both connections can close (a known race of v2.1.13 and "
              "v2.1.14). The node then stores the message in the mailbox of the peer."),
    AllowRule("error-line",
              re.compile(r"Send executor rejected task.*InventoryService\.processRequest", re.DOTALL),
              "A node sent an inventory response over a connection which the peer had closed, in the same race of two "
              "connections. The requester cancels its request when the connection closes and asks on another "
              "connection (docs/specifications/network/inventory-request-lifecycle.md)."),
]


@dataclass
class LogEntry:
    file_name: str
    line_number: int
    timestamp: str
    level: str
    logger: str
    first_line: str
    continuation: List[str] = field(default_factory=list)

    @property
    def full_text(self) -> str:
        return "\n".join([self.first_line] + self.continuation)


@dataclass
class Session:
    index: int
    version: Optional[str]
    start_file: Optional[str] = None
    start_line: Optional[int] = None
    start_timestamp: Optional[str] = None
    findings: Dict[str, List[LogEntry]] = field(default_factory=dict)
    allowed: Dict[str, int] = field(default_factory=dict)
    num_entries: int = 0


@dataclass
class NodeReport:
    name: str
    path: str
    files: List[str]
    sessions: List[Session]


def parse_version(version: Optional[str]) -> Optional[Tuple[int, ...]]:
    if not version:
        return None
    match = re.match(r"^(\d+(?:\.\d+)*)", version)
    if not match:
        return None
    return tuple(int(part) for part in match.group(1).split("."))


def find_log_files(data_dir: Path) -> List[Path]:
    """Returns the log files of a data directory, oldest first."""
    rolled: List[Tuple[int, Path]] = []
    for path in data_dir.iterdir():
        match = ROLLED_LOG_FILE.match(path.name)
        if match and path.is_file():
            index = int(match.group("index"))
            if 1 <= index <= MAX_ROLLED_LOG_INDEX:
                rolled.append((index, path))
    # bisq_1.log is the most recent rolled file, so the highest index is the oldest
    files = [path for _, path in sorted(rolled, key=lambda item: item[0], reverse=True)]
    current = data_dir / "bisq.log"
    if current.is_file():
        files.append(current)
    return files


def find_nodes(paths: Iterable[Path]) -> List[Tuple[str, Path, List[Path]]]:
    """Returns (name, path, log files) for each node. A directory without log files is searched one level down."""
    nodes: List[Tuple[str, Path, List[Path]]] = []
    for path in paths:
        if path.is_file():
            nodes.append((path.name, path, [path]))
        elif path.is_dir():
            files = find_log_files(path)
            if files:
                nodes.append((path.name, path, files))
            else:
                for child in sorted(p for p in path.iterdir() if p.is_dir()):
                    child_files = find_log_files(child)
                    if child_files:
                        nodes.append((child.name, child, child_files))
    return nodes


def read_entries(files: List[Path]) -> Iterator[LogEntry]:
    """Yields the log entries of the files in order. Lines without a log prefix belong to the entry before them."""
    for path in files:
        current: Optional[LogEntry] = None
        with path.open("r", encoding="utf-8", errors="replace") as file:
            for line_number, raw_line in enumerate(file, start=1):
                line = raw_line.rstrip("\r\n")
                match = ENTRY_START.match(line)
                if match:
                    if current is not None:
                        yield current
                    current = LogEntry(file_name=path.name,
                                       line_number=line_number,
                                       timestamp=match.group("timestamp"),
                                       level=match.group("level"),
                                       logger=match.group("logger"),
                                       first_line=line)
                elif current is not None:
                    current.continuation.append(line)
        if current is not None:
            yield current


def message_of(entry: LogEntry) -> str:
    match = ENTRY_START.match(entry.first_line)
    return match.group("message") if match else entry.first_line


def classify(entry: LogEntry, include_warnings: bool) -> Optional[Rule]:
    for rule in RULES:
        if rule.pattern.search(entry.first_line):
            return rule
    if entry.level == "ERROR":
        return ERROR_LINE_RULE
    if entry.level == "WARN" and include_warnings:
        return WARNING_LINE_RULE
    return None


def find_allow_rule(rule: Rule, entry: LogEntry, session: Session) -> Optional[AllowRule]:
    session_version = parse_version(session.version)
    for allow_rule in ALLOW_RULES:
        if allow_rule.rule_id != rule.rule_id:
            continue
        if allow_rule.only_before_version is not None:
            if session_version is None or session_version >= allow_rule.only_before_version:
                continue
        if allow_rule.pattern.search(entry.full_text):
            return allow_rule
    return None


def scan_node(files: List[Path], include_warnings: bool) -> List[Session]:
    sessions: List[Session] = [Session(index=0, version=None)]
    for entry in read_entries(files):
        session_match = SESSION_START.match(message_of(entry))
        if session_match:
            sessions.append(Session(index=len(sessions),
                                    version=session_match.group("version"),
                                    start_file=entry.file_name,
                                    start_line=entry.line_number,
                                    start_timestamp=entry.timestamp))
        session = sessions[-1]
        session.num_entries += 1
        rule = classify(entry, include_warnings)
        if rule is None:
            continue
        allow_rule = find_allow_rule(rule, entry, session)
        if allow_rule is not None:
            key = rule.rule_id
            session.allowed[key] = session.allowed.get(key, 0) + 1
        else:
            session.findings.setdefault(rule.rule_id, []).append(entry)
    # Lines before the first start line come from the start banner, which precedes the version line, or from a log which
    # rolled over within a session. Keep them only if they contain something to report.
    if not sessions[0].findings and not sessions[0].allowed:
        sessions.pop(0)
    return sessions


def rules_by_id() -> Dict[str, Rule]:
    result = {rule.rule_id: rule for rule in RULES}
    result[ERROR_LINE_RULE.rule_id] = ERROR_LINE_RULE
    result[WARNING_LINE_RULE.rule_id] = WARNING_LINE_RULE
    return result


@dataclass(frozen=True)
class Finding:
    rule_id: str
    severity: str
    description: str
    count: int
    examples: List[LogEntry]


def findings_of(session: Session, max_examples: int, inventory_repeat_threshold: int) -> List[Finding]:
    rules = rules_by_id()
    findings: List[Finding] = []
    for rule_id, entries in session.findings.items():
        rule = rules[rule_id]
        findings.append(Finding(rule_id, rule.severity, rule.description, len(entries), entries[:max_examples]))
    truncated = session.findings.get("inventory-response-truncated", [])
    if inventory_repeat_threshold > 0 and len(truncated) >= inventory_repeat_threshold:
        findings.append(Finding(INVENTORY_REPEATS_RULE_ID, "warning", INVENTORY_REPEATS_DESCRIPTION, len(truncated),
                                truncated[-max_examples:] if max_examples > 0 else []))
    findings.sort(key=lambda finding: (-SEVERITIES.index(finding.severity), finding.rule_id))
    return findings


def select_sessions(sessions: List[Session], last_session: bool, session_version: Optional[str]) -> List[Session]:
    selected = sessions
    if session_version is not None:
        selected = [session for session in selected if session.version == session_version]
    if last_session and selected:
        selected = [selected[-1]]
    return selected


def shorten(text: str, max_length: int = MAX_EXAMPLE_LINE_LENGTH) -> str:
    return text if len(text) <= max_length else text[:max_length - 3] + "..."


def describe_session(session: Session) -> str:
    version = "v" + session.version if session.version else "unknown version"
    if session.start_file is None:
        return f"Session before the first start line ({version})"
    return f"Session {session.index} ({version}, starts at {session.start_file}:{session.start_line}, " \
           f"{session.start_timestamp})"


def print_text_report(reports: List[NodeReport], max_examples: int, inventory_repeat_threshold: int) -> None:
    allow_reasons = {allow_rule.rule_id: allow_rule.reason for allow_rule in ALLOW_RULES}
    for report in reports:
        print(f"Node {report.name}: {report.path}")
        print(f"  Files: {', '.join(report.files)}")
        if not report.sessions:
            print("  No log entries.")
        for session in report.sessions:
            print(f"  {describe_session(session)}")
            findings = findings_of(session, max_examples, inventory_repeat_threshold)
            if not findings:
                print("    No findings.")
            for finding in findings:
                lines = "line" if finding.count == 1 else "lines"
                print(f"    [{finding.severity}] {finding.rule_id}: {finding.count} {lines}")
                print(f"      {finding.description}")
                for entry in finding.examples:
                    print(f"      {entry.file_name}:{entry.line_number}: {shorten(entry.first_line)}")
                    for continuation in entry.continuation[:MAX_EXAMPLE_CONTINUATION_LINES]:
                        print(f"        {shorten(continuation)}")
            for rule_id, count in sorted(session.allowed.items()):
                lines = "line" if count == 1 else "lines"
                print(f"    [allowed] {rule_id}: {count} {lines}. {allow_reasons.get(rule_id, '')}")
        print()


def json_report(reports: List[NodeReport], max_examples: int, inventory_repeat_threshold: int) -> dict:
    nodes = []
    for report in reports:
        sessions = []
        for session in report.sessions:
            findings = findings_of(session, max_examples, inventory_repeat_threshold)
            sessions.append({
                "index": session.index,
                "version": session.version,
                "startFile": session.start_file,
                "startLine": session.start_line,
                "startTimestamp": session.start_timestamp,
                "numEntries": session.num_entries,
                "findings": [{
                    "ruleId": finding.rule_id,
                    "severity": finding.severity,
                    "description": finding.description,
                    "count": finding.count,
                    "examples": [{
                        "file": entry.file_name,
                        "line": entry.line_number,
                        "text": entry.first_line,
                        "continuation": entry.continuation[:MAX_EXAMPLE_CONTINUATION_LINES],
                    } for entry in finding.examples],
                } for finding in findings],
                "allowed": dict(sorted(session.allowed.items())),
            })
        nodes.append({"name": report.name, "path": report.path, "files": report.files, "sessions": sessions})
    return {"nodes": nodes}


def worst_severity(reports: List[NodeReport], inventory_repeat_threshold: int) -> Optional[str]:
    worst: Optional[str] = None
    for report in reports:
        for session in report.sessions:
            for finding in findings_of(session, 0, inventory_repeat_threshold):
                if worst is None or SEVERITIES.index(finding.severity) > SEVERITIES.index(worst):
                    worst = finding.severity
    return worst


def parse_args(argv: Optional[List[str]]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scan Bisq 2 log files for failures which the user interface does not show.")
    parser.add_argument("paths", nargs="+", metavar="PATH", type=Path,
                        help="data directory, log file, or directory of data directories")
    parser.add_argument("--last-session", action="store_true",
                        help="report only the last application session of each node")
    parser.add_argument("--session-version", metavar="VERSION",
                        help="report only sessions of this version, for example 2.1.14")
    parser.add_argument("--include-warnings", action="store_true",
                        help="also report WARN lines which no specific rule matches")
    parser.add_argument("--max-examples", type=int, default=DEFAULT_MAX_EXAMPLES, metavar="N",
                        help=f"log lines to show for each finding (default {DEFAULT_MAX_EXAMPLES})")
    parser.add_argument("--inventory-repeat-threshold", type=int, default=DEFAULT_INVENTORY_REPEAT_THRESHOLD,
                        metavar="N",
                        help="report repeated truncated inventory responses from this count on, 0 to disable "
                             f"(default {DEFAULT_INVENTORY_REPEAT_THRESHOLD})")
    parser.add_argument("--fail-on", choices=SEVERITIES, default="warning",
                        help="lowest severity which makes the exit code 1 (default warning)")
    parser.add_argument("--json", action="store_true", help="print the report as JSON")
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    for path in args.paths:
        if not path.exists():
            print(f"Path does not exist: {path}", file=sys.stderr)
            return 2

    reports: List[NodeReport] = []
    for name, path, files in find_nodes(args.paths):
        sessions = select_sessions(scan_node(files, args.include_warnings), args.last_session, args.session_version)
        reports.append(NodeReport(name, str(path), [file.name for file in files], sessions))
    if not reports:
        print("No Bisq log file found. Expected bisq.log or bisq_<n>.log in a data directory.", file=sys.stderr)
        return 2

    if args.json:
        print(json.dumps(json_report(reports, args.max_examples, args.inventory_repeat_threshold), indent=2))
    else:
        print_text_report(reports, args.max_examples, args.inventory_repeat_threshold)

    worst = worst_severity(reports, args.inventory_repeat_threshold)
    if worst is not None and SEVERITIES.index(worst) >= SEVERITIES.index(args.fail_on):
        if not args.json:
            print(f"Result: findings up to severity {worst}.")
        return 1
    if not args.json:
        print("Result: no findings at or above severity " + args.fail_on + ".")
    return 0


if __name__ == "__main__":
    sys.exit(main())

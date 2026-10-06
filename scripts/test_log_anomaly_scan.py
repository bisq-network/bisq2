import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import log_anomaly_scan as scan


def line(level: str, message: str, logger: str = "b.n.p.s.d.s.StorageService", thread: str = "main",
         timestamp: str = "Oct-06 10:00:00.000") -> str:
    # Same layout as LogSetup: %d{MMM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{15}: %msg
    return f"{timestamp} [{thread}] {level:<5} {logger}: {message}"


def start(version: str) -> str:
    return line("INFO", f"Version: v{version} / Commit hash: abc1234", logger="b.a.ApplicationService")


FEATURE_LIST_WARNING = ("The size of the resolved feature list is not same as the protobuf list's size. This can "
                        "happen if new, unrecognized protobuf elements have been received. Old clients ignore such "
                        "new fields.")
ENUM_WARNING = "Could not resolve enum for proto UNRECOGNIZED. We skip that collection entry."
ENUM_STACK_TRACE = ("bisq.common.proto.UnresolvableProtobufEnumException: java.lang.NullPointerException: "
                    "Enum could not be resolved. Enum type= Feature; name=UNRECOGNIZED")
ADD_REJECTED_SIGNATURE = ("AddAuthenticatedDataRequest was not added to store. Result=DataStorageResult{"
                          "success=false, signatureInvalid=true, isSevereFailure=true}")
ADD_REJECTED_SEQUENCE = ("AddAuthenticatedDataRequest was not added to store. Result=DataStorageResult{"
                         "success=false, sequenceNrInvalid=true, signatureInvalid=false, isSevereFailure=true}")


class LogAnomalyScanTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)

    def tearDown(self):
        self.temp_dir.cleanup()

    def write_log(self, data_dir: str, file_name: str, lines):
        directory = self.root / data_dir
        directory.mkdir(parents=True, exist_ok=True)
        (directory / file_name).write_text("\n".join(lines) + "\n", encoding="utf-8")
        return directory

    def run_scan(self, *args):
        stdout = io.StringIO()
        stderr = io.StringIO()
        with redirect_stdout(stdout), redirect_stderr(stderr):
            exit_code = scan.main([str(arg) for arg in args])
        return exit_code, stdout.getvalue(), stderr.getvalue()

    def scan_json(self, *args):
        exit_code, output, _ = self.run_scan("--json", *args)
        return exit_code, json.loads(output)

    def test_reads_rolled_files_oldest_first_and_splits_sessions_by_version(self):
        self.write_log("node", "bisq_2.log", [start("2.1.13"), line("INFO", "oldest")])
        self.write_log("node", "bisq_1.log", [line("ERROR", "We skip an entry which we cannot parse. type=x")])
        directory = self.write_log("node", "bisq.log", [start("2.1.14"), line("ERROR", "Something failed")])

        self.assertEqual(["bisq_2.log", "bisq_1.log", "bisq.log"], [p.name for p in scan.find_log_files(directory)])

        exit_code, report = self.scan_json(directory)

        self.assertEqual(1, exit_code)
        sessions = report["nodes"][0]["sessions"]
        self.assertEqual(["2.1.13", "2.1.14"], [session["version"] for session in sessions])
        # The skipped entry in bisq_1.log belongs to the session which started in bisq_2.log
        self.assertEqual(["store-entry-skipped"], [f["ruleId"] for f in sessions[0]["findings"]])
        self.assertEqual(["error-line"], [f["ruleId"] for f in sessions[1]["findings"]])

    def test_lines_before_the_first_start_line_form_a_session_without_version(self):
        directory = self.write_log("node", "bisq.log", [
            line("ERROR", "Error before the start line"),
            start("2.1.14"),
        ])

        _, report = self.scan_json(directory)

        sessions = report["nodes"][0]["sessions"]
        self.assertIsNone(sessions[0]["version"])
        self.assertEqual("error-line", sessions[0]["findings"][0]["ruleId"])
        self.assertEqual([], sessions[1]["findings"])

    def test_start_banner_before_the_version_line_forms_no_session(self):
        directory = self.write_log("node", "bisq.log", [
            line("INFO", "", logger="b.a.ApplicationService"),
            "        ......   .....      ....",
            line("INFO", "Data directory: /data", logger="b.a.ApplicationService"),
            start("2.1.14"),
        ])

        _, report = self.scan_json(directory)

        self.assertEqual([1], [session["index"] for session in report["nodes"][0]["sessions"]])

    def test_stack_trace_lines_belong_to_the_entry(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.14"),
            line("WARN", "We reject the take offer request. tradeId=abc", logger="b.t.b.BisqEasyTradeService"),
            "java.lang.IllegalArgumentException: Mediators do not match.",
            "\tat bisq.trade.Validator.validate(Validator.java:1)",
            line("INFO", "next entry"),
        ])

        _, report = self.scan_json(directory)

        finding = report["nodes"][0]["sessions"][0]["findings"][0]
        self.assertEqual("take-offer-rejected", finding["ruleId"])
        self.assertEqual(1, finding["count"])
        self.assertEqual(2, len(finding["examples"][0]["continuation"]))

    def test_first_matching_rule_wins_over_the_generic_error_rule(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.14"),
            line("ERROR", "Couldn't read /data/db/private/UserIdentityStore.protobuf from file.",
                 logger="b.p.PersistableStoreReaderWriter"),
        ])

        _, report = self.scan_json(directory)

        rule_ids = [f["ruleId"] for f in report["nodes"][0]["sessions"][0]["findings"]]
        self.assertEqual(["store-file-unreadable"], rule_ids)

    def test_feature_warnings_of_old_versions_are_allowed_only_in_old_sessions(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.13"),
            line("WARN", FEATURE_LIST_WARNING, logger="b.n.p.n.Capability"),
            line("WARN", ENUM_WARNING, logger="b.c.p.ProtobufUtils"),
            ENUM_STACK_TRACE,
            start("2.1.14"),
            line("WARN", FEATURE_LIST_WARNING, logger="b.n.p.n.Capability"),
        ])

        exit_code, report = self.scan_json(directory)

        old_session, new_session = report["nodes"][0]["sessions"]
        self.assertEqual([], old_session["findings"])
        self.assertEqual({"enum-unresolved": 1, "feature-list-unresolved": 1}, old_session["allowed"])
        self.assertEqual(["feature-list-unresolved"], [f["ruleId"] for f in new_session["findings"]])
        self.assertEqual(1, exit_code)

    def test_unresolved_enum_of_another_type_is_reported_in_old_sessions(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.13"),
            line("ERROR", "Enum could not be resolved. We use the fallback value instead. "
                          "Enum type= TradeProtocolFailure; name=NEW_REASON", logger="b.c.p.ProtobufUtils"),
        ])

        _, report = self.scan_json(directory)

        session = report["nodes"][0]["sessions"][0]
        self.assertEqual(["enum-unresolved"], [f["ruleId"] for f in session["findings"]])
        self.assertEqual({}, session["allowed"])

    def test_rejected_add_with_invalid_signature_stays_a_finding_and_names_the_known_cause(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.14"),
            line("INFO", ADD_REJECTED_SIGNATURE),
            line("INFO", ADD_REJECTED_SEQUENCE),
        ])

        _, report = self.scan_json(directory)

        session = report["nodes"][0]["sessions"][0]
        self.assertEqual({}, session["allowed"])
        findings = {finding["ruleId"]: finding for finding in session["findings"]}
        self.assertEqual({"storage-add-signature-invalid", "storage-request-rejected"}, set(findings))
        self.assertIn("storage-refresh-requests.md", findings["storage-add-signature-invalid"]["description"])
        self.assertIn("sequenceNrInvalid=true", findings["storage-request-rejected"]["examples"][0]["text"])

    def test_warn_lines_without_rule_are_reported_only_on_request(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.14"),
            line("WARN", "Some warning"),
        ])

        exit_code, report = self.scan_json(directory)
        self.assertEqual(0, exit_code)
        self.assertEqual([], report["nodes"][0]["sessions"][0]["findings"])

        exit_code, report = self.scan_json("--include-warnings", directory)
        self.assertEqual(1, exit_code)
        self.assertEqual("warning-line", report["nodes"][0]["sessions"][0]["findings"][0]["ruleId"])

    def test_repeated_truncated_inventory_responses_are_a_warning(self):
        truncated = line("INFO", "Inventory with 6000 items and accumulatedSize of 2001 kb. maxSizeReached=true",
                         logger="b.n.p.s.d.i.f.FilterService")
        directory = self.write_log("node", "bisq.log", [start("2.1.14")] + [truncated] * 3)

        exit_code, report = self.scan_json(directory)
        self.assertEqual(0, exit_code)
        findings = report["nodes"][0]["sessions"][0]["findings"]
        self.assertEqual([("inventory-response-truncated", "notice", 3)],
                         [(f["ruleId"], f["severity"], f["count"]) for f in findings])

        exit_code, report = self.scan_json("--inventory-repeat-threshold", "3", directory)
        self.assertEqual(1, exit_code)
        findings = report["nodes"][0]["sessions"][0]["findings"]
        self.assertEqual(["inventory-truncation-repeats", "inventory-response-truncated"],
                         [f["ruleId"] for f in findings])

    def test_fail_on_selects_the_lowest_severity_which_fails(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.14"),
            line("INFO", "AddAuthenticatedDataRequest was not added to store. Result=DataStorageResult{"
                         "success=false, signatureInvalid=true}"),
        ])

        self.assertEqual(1, self.run_scan(directory)[0])
        self.assertEqual(0, self.run_scan("--fail-on", "error", directory)[0])

    def test_directory_of_data_directories_reports_each_node(self):
        self.write_log("alice", "bisq.log", [start("2.1.14")])
        self.write_log("bob", "bisq.log", [start("2.1.13"), line("ERROR", "Failure")])

        exit_code, report = self.scan_json(self.root)

        self.assertEqual(1, exit_code)
        self.assertEqual(["alice", "bob"], [node["name"] for node in report["nodes"]])

    def test_session_filters(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.13"), line("ERROR", "old failure"),
            start("2.1.14"), line("ERROR", "first new failure"),
            start("2.1.14"),
        ])

        _, report = self.scan_json("--last-session", directory)
        self.assertEqual([3], [s["index"] for s in report["nodes"][0]["sessions"]])

        _, report = self.scan_json("--session-version", "2.1.13", directory)
        self.assertEqual([1], [s["index"] for s in report["nodes"][0]["sessions"]])

        exit_code, report = self.scan_json("--session-version", "2.1.14", "--last-session", directory)
        self.assertEqual(0, exit_code)
        self.assertEqual([3], [s["index"] for s in report["nodes"][0]["sessions"]])

    def test_text_report_names_findings_and_allowed_lines(self):
        directory = self.write_log("node", "bisq.log", [
            start("2.1.13"),
            line("ERROR", "We skip an entry which we cannot parse. type=bisq.user.protobuf.UserProfile, 5 bytes"),
            line("WARN", FEATURE_LIST_WARNING, logger="b.n.p.n.Capability"),
        ])

        exit_code, output, _ = self.run_scan(directory)

        self.assertEqual(1, exit_code)
        self.assertIn("[error] store-entry-skipped: 1 line", output)
        self.assertIn("[allowed] feature-list-unresolved: 1 line.", output)
        self.assertIn("Result: findings up to severity error.", output)

    def test_missing_path_and_missing_logs_are_usage_errors(self):
        self.assertEqual(2, self.run_scan(self.root / "missing")[0])
        (self.root / "empty").mkdir()
        self.assertEqual(2, self.run_scan(self.root / "empty")[0])

    def test_version_comparison_uses_numbers(self):
        self.assertLess(scan.parse_version("2.1.9"), scan.parse_version("2.1.13"))
        self.assertEqual((2, 1, 14), scan.parse_version("2.1.14-SNAPSHOT"))
        self.assertIsNone(scan.parse_version(None))


if __name__ == "__main__":
    unittest.main()

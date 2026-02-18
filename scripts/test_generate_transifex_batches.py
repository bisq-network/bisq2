import io
import json
import unittest
from contextlib import redirect_stdout
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))

import generate_transifex_batches as g


class GenerateTransifexBatchesTests(unittest.TestCase):
    def test_preserves_locale_format_in_batches(self):
        batches = g.generate_batches(batch_size=4, max_parallel=2, locales=["pt_BR", "zh-Hans", "af_ZA"])
        self.assertEqual("pt_BR,zh-Hans,af_ZA", batches[0]["locales"])

    def test_resources_mapping_supports_hyphen_and_underscore_locales(self):
        batches = [{"id": 1, "locales": "pt_BR,zh-Hans,af_ZA", "name": "batch-1"}]
        resources = {
            "pt_BR": "bisq-2.accountproperties",
            "zh-Hans": "bisq-2.chatproperties",
            "af_ZA": "bisq-2.settingsproperties",
        }

        buf = io.StringIO()
        with redirect_stdout(buf):
            g.print_github_actions_json(batches, resources)

        matrix = json.loads(buf.getvalue())
        actual_resources = set(matrix["include"][0]["resources"].split(","))
        expected_resources = {
            "bisq-2.accountproperties",
            "bisq-2.chatproperties",
            "bisq-2.settingsproperties",
        }
        self.assertSetEqual(expected_resources, actual_resources)


    def test_priority_tiers_have_no_duplicates(self):
        all_tier_locales = (
            g.PRIORITY_TIERS["critical"]
            + g.PRIORITY_TIERS["important"]
            + g.PRIORITY_TIERS["standard"]
        )
        self.assertEqual(len(all_tier_locales), len(set(all_tier_locales)))

if __name__ == "__main__":
    unittest.main()

import re
import unittest
from pathlib import Path


SETTINGS = (
    Path(__file__).resolve().parents[2]
    / "app/src/main/java/com/lu4p/fokuslauncher/ui/settings"
)


class HomeAlignmentPlacementTest(unittest.TestCase):
    def test_alignment_control_belongs_to_look_and_feel_not_settings_hub(self):
        appearance = (SETTINGS / "AppearanceSettingsScreen.kt").read_text()
        hub = (SETTINGS / "SettingsScreen.kt").read_text()

        self.assertEqual(len(re.findall(r"\bHomeAlignmentRow\s*\(", appearance)), 1)
        self.assertNotRegex(hub, r"\bHomeAlignmentRow\s*\(")
        self.assertRegex(
            appearance,
            r"HomeAlignmentRow\s*\(\s*"
            r"currentAlignment\s*=\s*uiState\.homeAlignment,\s*"
            r"onAlignmentChanged\s*=\s*viewModel::setHomeAlignment,\s*\)",
        )


if __name__ == "__main__":
    unittest.main()

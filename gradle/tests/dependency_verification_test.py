"""Offline functional checks for the dependency audit, using Gradle and a local Maven repository."""

import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile


REPOSITORY = Path(__file__).resolve().parents[2]
BUILDS = {
    "": ["build-logic", "apps", "network"],
    "build-logic": [],
    "apps": ["..", "desktop"],
    "apps/desktop": ["../.."],
    "network": ["..", "tor"],
    "network/tor": ["../.."],
}
RESOLVER = "resolveAndVerifyDependenciesForCurrentBuild"


class DependencyVerificationTest(unittest.TestCase):
    def setUp(self):
        (REPOSITORY / "temp").mkdir(exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(prefix="dependency-audit-test-", dir=REPOSITORY / "temp")
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.gradle = self.root / "gradle"
        self.gradle.mkdir()
        (self.gradle / "libs.versions.toml").write_text('[versions]\nfixture = "1"\n')
        shutil.copy(REPOSITORY / "gradle/dependency-verification.gradle.kts", self.gradle)
        self.inventory = self.root / ".gradle/dependency-verification"
        self.inventory_files = {
            build.replace("/", "_") or "root": self.root / build / ".gradle/dependency-verification/inventory.tsv"
            for build in BUILDS
        }
        self.metadata = self.gradle / "verification-metadata.xml"
        # Match the main build's JVM options so Gradle can reuse its daemon.
        (self.root / "gradle.properties").write_text("org.gradle.jvmargs=-Xmx5120M\n")
        metadata = ET.Element("verification-metadata")
        configuration = ET.SubElement(metadata, "configuration")
        ET.SubElement(configuration, "verify-metadata").text = "true"
        ET.SubElement(configuration, "verify-signatures").text = "false"
        components = ET.SubElement(metadata, "components")

        for build, children in BUILDS.items():
            project = self.root / build
            project.mkdir(parents=True, exist_ok=True)
            name = build.replace("/", "_") or "root"
            (project / "settings.gradle.kts").write_text(
                f'rootProject.name = "{project.name}"\n'
                + "\n".join(f'includeBuild("{child}")' for child in children)
            )
            repository_path = os.path.relpath(self.root / "maven", project).replace(os.sep, "/")
            script_path = os.path.relpath(self.gradle / "dependency-verification.gradle.kts", project).replace(os.sep, "/")
            (project / "build.gradle").write_text(
                f"apply from: '{script_path}'\n"
                "configurations { audit { canBeResolved = true; canBeConsumed = false } }\n"
                f"repositories {{ maven {{ url = uri('{repository_path}') }} }}\n"
                f'dependencies {{ audit("fixture:{name}:" + providers.gradleProperty("auditVersion").getOrElse("1")) }}\n'
                + ('tasks.register("check") { dependsOn("verifyDependencySignaturePolicy") }\n'
                   'tasks.register("clean", Delete) { delete(layout.buildDirectory) }\n'
                   'tasks.register("build") { dependsOn("check") }\n' if not build else "")
            )
            for version in ("1", "2"):
                module = self.root / "maven/fixture" / name / version
                module.mkdir(parents=True)
                pom = module / f"{name}-{version}.pom"
                pom.write_text(
                    f"<project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>"
                    f"<artifactId>{name}</artifactId><version>{version}</version></project>"
                )
                jar = module / f"{name}-{version}.jar"
                with zipfile.ZipFile(jar, "w") as archive:
                    archive.writestr("fixture.txt", f"{name}:{version}")
                component = ET.SubElement(components, "component", group="fixture", name=name, version=version)
                for artifact in (pom, jar):
                    entry = ET.SubElement(component, "artifact", name=artifact.name)
                    ET.SubElement(entry, "sha256", value=hashlib.sha256(artifact.read_bytes()).hexdigest(),
                                  reason="Artifact is not signed")
        build_dirs = ", ".join(f'"{build + "/" if build else ""}build"' for build in BUILDS)
        with (self.root / "build.gradle").open("a") as script:
            script.write(f'tasks.register("cleanAll", Delete) {{ delete({build_dirs}) }}\n')
        ET.ElementTree(metadata).write(self.metadata, encoding="utf-8", xml_declaration=True)
        self.extra_allowlist_lines = []
        self.write_allowlist()

    def write_allowlist(self):
        lines = list(self.extra_allowlist_lines)
        for build in BUILDS:
            name = build.replace("/", "_") or "root"
            for version in ("1", "2"):
                for extension in ("jar", "pom"):
                    lines.append(f"fixture:{name}:{version}\t{name}-{version}.{extension}\tLocal test fixture")
        (self.gradle / "dependency-checksum-fallback-allowlist.tsv").write_text("\n".join(sorted(lines)) + "\n")

    def add_metadata_artifact(self, name, artifact, reason=None, pgp=None):
        tree = ET.parse(self.metadata)
        components = tree.getroot().find("components")
        component = ET.SubElement(components, "component", group="fixture", name=name, version="1")
        entry = ET.SubElement(component, "artifact", name=artifact)
        if pgp:
            ET.SubElement(entry, "pgp", value=pgp)
        attributes = {"value": "0" * 64}
        if reason:
            attributes["reason"] = reason
        ET.SubElement(entry, "sha256", **attributes)
        tree.write(self.metadata, encoding="utf-8", xml_declaration=True)

    def set_reason(self, artifact, reason):
        tree = ET.parse(self.metadata)
        for entry in tree.getroot().iter("artifact"):
            if entry.get("name") == artifact:
                entry.find("sha256").set("reason", reason)
        tree.write(self.metadata, encoding="utf-8", xml_declaration=True)

    def run_gradle(self, *arguments, success=True):
        wrapper = REPOSITORY / ("gradlew.bat" if os.name == "nt" else "gradlew")
        result = subprocess.run(
            [str(wrapper), "-p", str(self.root), "--offline", "--console=plain", "--parallel", "--max-workers=4",
             *arguments],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=120,
        )
        self.assertEqual(result.returncode == 0, success, result.stdout)
        return result.stdout

    def assert_all_reused(self, output):
        lines = [line for line in output.splitlines() if line.startswith("> Task ") and RESOLVER in line]
        self.assertEqual(len(lines), len(BUILDS), output)
        self.assertTrue(all(line.endswith(" UP-TO-DATE") for line in lines), output)

    def test_inventory_reuse_and_invalidation(self):
        self.run_gradle("check")
        for name, inventory in self.inventory_files.items():
            self.assertIn(f"direct\tfixture:{name}:1\n", inventory.read_text())
        self.assert_all_reused(self.run_gradle("check"))

        self.assert_all_reused(self.run_gradle("clean", "build"))

        (self.root / "Example.java").write_text("class Example {}\n")
        self.assert_all_reused(self.run_gradle("check"))

        self.metadata.write_text(self.metadata.read_text() + "\n<!-- Updated verification policy -->\n")
        self.assertEqual(self.run_gradle("check").count("Resolved and verified"), len(BUILDS))
        self.assert_all_reused(self.run_gradle("check"))

        desktop_inventory = self.inventory_files["apps_desktop"]
        desktop_inventory.unlink()
        self.run_gradle("check")
        self.assertTrue(desktop_inventory.is_file())
        self.assert_all_reused(self.run_gradle("check"))

        expected_inventory = desktop_inventory.read_text()
        desktop_inventory.write_text("configurations\t0\n")
        self.run_gradle("check")
        self.assertEqual(desktop_inventory.read_text(), expected_inventory)

        # Old inventories must neither poison the graph nor conceal missing expected inventories.
        (self.inventory / "obsolete.tsv").write_text(
            "direct\tfixture:obsolete:1\nartifact\tfixture:obsolete:1\tobsolete.exe\n"
        )
        self.assert_all_reused(self.run_gradle("check"))
        desktop_inventory.unlink()
        failure = self.run_gradle("check", "-x", ":apps:desktop:" + RESOLVER, success=False)
        self.assertIn("Missing dependency verification inventory", failure)

        output = self.run_gradle("check", "-PauditVersion=2")
        self.assertEqual(output.count("Resolved and verified"), len(BUILDS), output)
        for name, inventory in self.inventory_files.items():
            self.assertIn(f"direct\tfixture:{name}:2\n", inventory.read_text())
        self.assert_all_reused(self.run_gradle("check", "-PauditVersion=2"))

        for option in ("--refresh-dependencies", "--rerun-tasks", "--write-verification-metadata=sha256"):
            with self.subTest(option=option):
                output = self.run_gradle("resolveAndVerifyDependencies", "-PauditVersion=2", option)
                self.assertEqual(output.count("Resolved and verified"), len(BUILDS), output)

        self.metadata.unlink()
        self.run_gradle("resolveAndVerifyDependencies", "-PauditVersion=2", "--write-verification-metadata=sha256")
        components = ET.parse(self.metadata).findall(".//{*}component")
        self.assertEqual(
            {(component.get("name"), component.get("version")) for component in components},
            {(build.replace("/", "_") or "root", "2") for build in BUILDS},
        )

    def test_checksum_fallback_of_unresolved_component_requires_approval(self):
        # Gradle also verifies parent POMs, imported BOMs and artifacts of other platforms against the metadata.
        self.add_metadata_artifact("parent", "parent-1.pom", reason="Artifact is not signed")
        output = self.run_gradle("check", success=False)
        self.assertIn("Unapproved checksum-only dependency artifacts", output)
        self.assertIn("fixture:parent:1\tparent-1.pom", output)

        self.extra_allowlist_lines.append("fixture:parent:1\tparent-1.pom\tLocal test fixture")
        self.write_allowlist()
        self.run_gradle("check")

    def test_checksum_fallback_for_missing_key_requires_approval(self):
        self.set_reason("root-1.jar", "A key couldn't be downloaded")
        self.run_gradle("check")

        self.extra_allowlist_lines.append("fixture:key:1\tkey-1.jar\tLocal test fixture")
        self.add_metadata_artifact("key", "key-1.jar", reason="A key couldn't be downloaded")
        self.add_metadata_artifact("failed", "failed-1.jar", reason="PGP signature verification failed!")
        self.write_allowlist()
        output = self.run_gradle("check", success=False)
        self.assertIn("fixture:failed:1\tfailed-1.jar", output)
        self.assertNotIn("fixture:key:1\tkey-1.jar", output)

    def test_checksum_without_reason_requires_approval_when_no_key_can_verify_it(self):
        self.add_metadata_artifact("unsigned", "unsigned-1.jar")
        self.add_metadata_artifact("signed", "signed-1.jar", pgp="0123456789ABCDEF0123456789ABCDEF01234567")
        self.add_metadata_artifact("trusted", "trusted-1.jar")
        self.add_metadata_artifact("ignored", "ignored-1.jar", pgp="FEDCBA9876543210FEDCBA9876543210FEDCBA98")
        self.add_metadata_artifact("other", "other-1.jar")
        tree = ET.parse(self.metadata)
        configuration = tree.getroot().find("configuration")
        ignored_keys = ET.SubElement(configuration, "ignored-keys")
        ET.SubElement(ignored_keys, "ignored-key", id="76543210FEDCBA98")
        trusted_keys = ET.SubElement(configuration, "trusted-keys")
        ET.SubElement(trusted_keys, "trusted-key", id="0123456789ABCDEF0123456789ABCDEF01234567", group="fixture",
                      name="trusted")
        ET.SubElement(trusted_keys, "trusted-key", id="0123456789ABCDEF0123456789ABCDEF01234567", group="fixture",
                      name="other", file="other-1.pom")
        tree.write(self.metadata, encoding="utf-8", xml_declaration=True)
        output = self.run_gradle("check", success=False)
        self.assertIn("fixture:unsigned:1\tunsigned-1.jar", output)
        self.assertIn("fixture:ignored:1\tignored-1.jar", output)
        self.assertIn("fixture:other:1\tother-1.jar", output)
        self.assertNotIn("fixture:signed:1\tsigned-1.jar", output)
        self.assertNotIn("fixture:trusted:1\ttrusted-1.jar", output)

    def test_allowlist_entry_for_signed_artifact_is_stale(self):
        self.add_metadata_artifact("signed", "signed-1.jar", pgp="0123456789ABCDEF0123456789ABCDEF01234567")
        self.extra_allowlist_lines.append("fixture:signed:1\tsigned-1.jar\tLocal test fixture")
        self.write_allowlist()
        output = self.run_gradle("check", success=False)
        self.assertIn("Allowlist entries that are no longer checksum-only artifacts", output)
        self.assertIn("fixture:signed:1\tsigned-1.jar", output)

    def test_verification_failure_after_successful_audit(self):
        self.run_gradle("check")
        self.assert_all_reused(self.run_gradle("check"))
        # Use a local repository so corruption cannot affect the developer's shared dependency cache.
        jar = self.root / "maven/fixture/apps_desktop/1/apps_desktop-1.jar"
        jar.write_bytes(b"untrusted replacement")
        output = self.run_gradle("check", success=False)
        self.assertIn("Dependency verification failed", output)
        self.assertIn("apps_desktop-1.jar", output)

    def test_full_clean_preserves_dependency_inventories(self):
        build_dirs = [self.root / build / "build" for build in BUILDS]
        for repeat in (False, True):
            with self.subTest(reused=repeat):
                for directory in build_dirs:
                    directory.mkdir(parents=True, exist_ok=True)
                    (directory / "output.txt").write_text("Disposable build output\n")
                output = self.run_gradle("cleanAll", "build")
                for directory in build_dirs:
                    self.assertFalse(directory.exists())
                for name, inventory in self.inventory_files.items():
                    self.assertIn(f"direct\tfixture:{name}:1\n", inventory.read_text())
                if repeat:
                    self.assert_all_reused(output)

    def test_relaxed_verification_modes_are_rejected(self):
        self.run_gradle("check")
        jar = self.root / "maven/fixture/apps_desktop/1/apps_desktop-1.jar"
        jar.write_bytes(b"untrusted replacement")
        # The property form also applies when set in a user's ~/.gradle/gradle.properties or GRADLE_OPTS.
        for option in (
            "--dependency-verification=off",
            "--dependency-verification=lenient",
            "-Dorg.gradle.dependency.verification=off",
        ):
            with self.subTest(option=option):
                output = self.run_gradle("check", option, success=False)
                self.assertIn("requires --dependency-verification=strict", output)


if __name__ == "__main__":
    unittest.main()

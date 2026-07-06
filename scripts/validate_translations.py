#!/usr/bin/env python3
"""
Validate BibleLockScreen translation files.

Run locally:    python3 scripts/validate_translations.py
Run in CI:      .github/workflows/validate-translations.yml

Exits 0 if everything is consistent, 1 if any check fails.

Checks performed
----------------
1. SCHEMA (UI strings)
   - Parse AppStrings.kt and extract the set of property names declared in
     the data class. This is the source-of-truth schema.
   - For every StringsXX.kt file, verify each assigned property name exists
     in the schema (catches typos and references to renamed/removed fields).
   - Detect duplicate assignments within a single StringsXX.kt file.

2. CROSS-FILE PARITY (UI strings <-> availableLanguages)
   - Every language code listed in `availableLanguages` must have a
     StringsXX.kt file.
   - Every StringsXX.kt file (except StringsSK.kt and StringsTemplate*)
     must be listed in `availableLanguages`.

3. VERSE JSON
   - Every verses_XX.json must be valid JSON.
   - Each entry must be an object with keys `text`, `ref`, `lang` (all strings,
     all non-empty).
   - The `lang` value of every entry must match the file's XX code.

4. CROSS-FILE PARITY (UI strings <-> verse JSON)
   - Every language with a StringsXX.kt file must also have a verses_XX.json,
     and vice versa. (Translations should be complete; a missing file in
     either direction is reported as a warning, not an error, so partial
     PRs can still merge -- see --strict to promote warnings to errors.)

5. BIBLE COPYRIGHT DISCLAIMER
   - For informational purposes only (not enforced): prints a reminder that
     verse content must come from public-domain Bible translations. The
     contributor is expected to declare the source in their PR description.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Dict, List, Set, Tuple

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent
STRINGS_DIR = REPO_ROOT / "app/src/main/java/com/daklok/biblelockscreen/strings"
ASSETS_DIR = REPO_ROOT / "app/src/main/assets"

APP_STRINGS_FILE = STRINGS_DIR / "AppStrings.kt"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class Report:
    def __init__(self) -> None:
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.infos: List[str] = []

    def err(self, msg: str) -> None:
        self.errors.append(msg)

    def warn(self, msg: str) -> None:
        self.warnings.append(msg)

    def info(self, msg: str) -> None:
        self.infos.append(msg)

    def exit_code(self, strict: bool = False) -> int:
        if self.errors:
            return 1
        if strict and self.warnings:
            return 1
        return 0

    def print_summary(self) -> None:
        if self.infos:
            print("\n--- Info ---")
            for m in self.infos:
                print(f"  [INFO] {m}")
        if self.warnings:
            print("\n--- Warnings ---")
            for m in self.warnings:
                print(f"  [WARN] {m}")
        if self.errors:
            print("\n--- Errors ---")
            for m in self.errors:
                print(f"  [ERROR] {m}")
        print()
        if self.errors:
            print(f"Result: FAIL  ({len(self.errors)} error(s), {len(self.warnings)} warning(s))")
        elif self.warnings:
            print(f"Result: PASS with warnings  ({len(self.warnings)} warning(s))")
        else:
            print(f"Result: PASS  (all checks green)")


# Regex: property assignment inside a StringsXX.kt file.
#   Matches lines like:  `    updateTime = "..."`  (leading whitespace,
#   identifier, optional whitespace, `=`). Does NOT match `val updateTime:`.
ASSIGN_RE = re.compile(r'^\s+([A-Za-z_][A-Za-z0-9_]*)\s*=', re.MULTILINE)

# Regex: property declaration inside AppStrings.kt data class body.
#   Matches:  `    val propertyName: String = ...`
DECL_RE = re.compile(r'^\s*val\s+([A-Za-z_][A-Za-z0-9_]*)\s*:\s*', re.MULTILINE)

# Regex: language-code-to-name pair in `availableLanguages`.
#   Matches:  `    "EN" to "English",`
LANG_LIST_RE = re.compile(r'^\s*"([A-Z]{2,3})"\s*to\s*"([^"]+)"\s*,?\s*$')

# Regex: language codes listed in getDefaultAppLanguage()'s when-branch.
#   Matches:  `        "SK", "EN", "CZ", ... -> sysLang`
DEFAULT_LANG_BRANCH_RE = re.compile(r'^\s*((?:"[A-Z]{2,3}"\s*,\s*)+)"[A-Z]{2,3}"\s*->\s*sysLang')

# ---------------------------------------------------------------------------
# Parsers
# ---------------------------------------------------------------------------

def _extract_paren_block(text: str, start: int) -> int:
    """
    Given text and the index of an opening `(`, return the index of the
    matching closing `)`. Handles nested parens and string literals
    (Kotlin string literals don't span newlines in this codebase, so we
    only need to track single-line strings).
    """
    depth = 0
    i = start
    in_string = False
    while i < len(text):
        c = text[i]
        if in_string:
            if c == '\\' and i + 1 < len(text):
                i += 2  # skip escaped char
                continue
            if c == '"':
                in_string = False
            i += 1
            continue
        if c == '"':
            in_string = True
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1  # unbalanced


def parse_schema(report: Report) -> Set[str]:
    """Return the set of property names declared in AppStrings.kt's data class."""
    if not APP_STRINGS_FILE.is_file():
        report.err(f"Missing file: {APP_STRINGS_FILE}")
        return set()

    text = APP_STRINGS_FILE.read_text(encoding="utf-8")
    # Find `data class AppStrings(` and walk to the matching `)`.
    header = re.search(r'data class AppStrings\(', text)
    if not header:
        report.err("Could not locate `data class AppStrings(...)` in AppStrings.kt")
        return set()
    open_paren = header.end() - 1
    close_paren = _extract_paren_block(text, open_paren)
    if close_paren == -1:
        report.err("Unbalanced parens in AppStrings data class declaration.")
        return set()
    body = text[open_paren + 1:close_paren]
    props = set(DECL_RE.findall(body))
    if not props:
        report.err("Schema parser found zero properties in AppStrings.kt -- parser bug?")
    else:
        report.info(f"Schema declares {len(props)} properties in AppStrings.kt")
    return props


def parse_language_strings_file(path: Path) -> Tuple[str, Set[str], List[str]]:
    """
    Return (language_code, assigned_property_names, list_of_unknown_lines).

    `language_code` is derived from the filename, e.g. StringsEN.kt -> "EN".
    `assigned_property_names` preserves duplicates so the caller can detect them.
    """
    # StringsEN.kt -> "EN"; StringsPT.kt -> "PT"
    m = re.match(r'^Strings([A-Z]{2,3})\.kt$', path.name)
    code = m.group(1) if m else path.stem

    assigned: List[str] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        m = ASSIGN_RE.match(line)
        if m:
            assigned.append(m.group(1))
    return code, set(), assigned  # set() is filled by caller; we return raw list


def parse_available_languages(report: Report) -> List[Tuple[str, str]]:
    """Parse the `availableLanguages = listOf(...)` block in AppStrings.kt."""
    text = APP_STRINGS_FILE.read_text(encoding="utf-8")
    m = re.search(r'val availableLanguages\s*=\s*listOf\((.*?)\n\)', text, re.DOTALL)
    if not m:
        report.err("Could not locate `val availableLanguages = listOf(...)` in AppStrings.kt")
        return []
    body = m.group(1)
    pairs: List[Tuple[str, str]] = []
    for line in body.splitlines():
        m = LANG_LIST_RE.match(line)
        if m:
            pairs.append((m.group(1), m.group(2)))
    return pairs


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------

def check_ui_strings(report: Report, schema: Set[str]) -> Dict[str, Path]:
    """
    Validate every StringsXX.kt file. Returns a dict mapping language code
    -> file path, for use by later cross-file checks.
    """
    # Regex for a value-assigned line: `    propertyName = "value"`
    # Captures the property name AND the (string) value.
    assign_value_re = re.compile(r'^\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*"([^"]*)"')

    lang_files: Dict[str, Path] = {}
    for path in sorted(STRINGS_DIR.glob("Strings*.kt")):
        if path.name == "StringsTemplate.kt":
            # template file -- skip (should normally have .kt.txt extension,
            # but be defensive in case someone renamed it)
            continue
        code = path.stem.removeprefix("Strings")
        lang_files[code] = path

        text = path.read_text(encoding="utf-8")
        assigned: List[str] = []
        empty_values: List[str] = []
        todo_values: List[str] = []
        for line in text.splitlines():
            m = assign_value_re.match(line)
            if m:
                prop, value = m.group(1), m.group(2)
                assigned.append(prop)
                if value.strip() == "":
                    empty_values.append(prop)
                elif "TODO" in value or "<TODO>" in value:
                    todo_values.append(prop)

        # Detect duplicates
        seen: Set[str] = set()
        dups: Set[str] = set()
        for p in assigned:
            if p in seen:
                dups.add(p)
            seen.add(p)
        if dups:
            report.err(f"{path.name}: duplicate property assignments: {sorted(dups)}")

        # Detect unknown properties
        unknown = seen - schema
        if unknown:
            report.err(f"{path.name}: unknown properties (not in AppStrings schema): {sorted(unknown)}")

        # Detect unfilled placeholders
        if todo_values:
            report.warn(
                f"{path.name}: {len(todo_values)} properties still contain TODO "
                f"placeholders and look unfinished: {todo_values[:5]}"
                f"{'...' if len(todo_values) > 5 else ''}"
            )
        if empty_values:
            report.warn(
                f"{path.name}: {len(empty_values)} properties have empty string "
                f"values: {empty_values[:5]}"
                f"{'...' if len(empty_values) > 5 else ''}"
            )

        if not unknown and not dups and not todo_values and not empty_values:
            report.info(f"{path.name}: {len(seen)} properties assigned, all valid.")

    return lang_files


def check_available_languages(report: Report, lang_files: Dict[str, Path]) -> None:
    """Cross-check StringsXX.kt files against availableLanguages in AppStrings.kt."""
    pairs = parse_available_languages(report)
    listed_codes: Set[str] = {c for c, _ in pairs}

    # Every StringsXX.kt (except SK, which is the default and always present)
    # should be listed in availableLanguages.
    for code, path in lang_files.items():
        if code == "SK":
            continue
        if code not in listed_codes:
            report.err(
                f"{path.name} exists but '{code}' is not registered in "
                f"`availableLanguages` (AppStrings.kt). Add it so users can "
                f"select it in the language picker."
            )

    # Every code in availableLanguages should have a StringsXX.kt file.
    for code, name in pairs:
        if code not in lang_files:
            report.err(
                f"'{code}' ({name}) is listed in `availableLanguages` but no "
                f"Strings{code}.kt file was found in {STRINGS_DIR}."
            )


def check_verse_json(report: Report) -> Dict[str, Path]:
    """Validate every verses_XX.json file. Returns dict: code -> path."""
    verse_files: Dict[str, Path] = {}
    for path in sorted(ASSETS_DIR.glob("verses_*.json")):
        # verses_EN.json -> "EN"
        m = re.match(r'^verses_([A-Z]{2,3})\.json$', path.name)
        if not m:
            report.warn(f"Verse JSON file with unexpected name: {path.name} (skipping)")
            continue
        code = m.group(1)
        verse_files[code] = path

        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            report.err(f"{path.name}: invalid JSON -- {e}")
            continue

        if not isinstance(data, list):
            report.err(f"{path.name}: top-level value must be a JSON array, got {type(data).__name__}")
            continue

        if len(data) == 0:
            report.warn(f"{path.name}: empty verse list")
            continue

        bad_entries = 0
        lang_mismatches = 0
        for i, entry in enumerate(data):
            if not isinstance(entry, dict):
                report.err(f"{path.name}: entry #{i} is not an object")
                bad_entries += 1
                continue
            missing = [k for k in ("text", "ref", "lang") if k not in entry or not isinstance(entry[k], str) or not entry[k].strip()]
            if missing:
                report.err(f"{path.name}: entry #{i} missing or empty keys: {missing}")
                bad_entries += 1
                continue
            if entry["lang"] != code:
                lang_mismatches += 1
        if lang_mismatches:
            report.err(f"{path.name}: {lang_mismatches} entries have `lang` != '{code}'")
        if bad_entries == 0 and lang_mismatches == 0:
            report.info(f"{path.name}: {len(data)} verses, all valid.")

    return verse_files


def check_ui_vs_verse_parity(report: Report, lang_files: Dict[str, Path], verse_files: Dict[str, Path]) -> None:
    """Cross-check: UI strings files vs verse JSON files should be 1:1."""
    ui_codes = set(lang_files.keys())     # SK is included; it has a StringsSK.kt file
    verse_codes = set(verse_files.keys())

    missing_verse = ui_codes - verse_codes
    missing_ui = verse_codes - ui_codes

    for code in sorted(missing_verse):
        report.warn(
            f"Strings{code}.kt exists but no verses_{code}.json was found. "
            f"Users selecting {code} will see UI in {code} but verses will "
            f"fall back to English. Add a verses_{code}.json file to complete "
            f"the translation."
        )
    for code in sorted(missing_ui):
        report.warn(
            f"verses_{code}.json exists but no Strings{code}.kt was found. "
            f"Users selecting {code} will see verses in {code} but UI in "
            f"the system default. Add a Strings{code}.kt file to complete "
            f"the translation."
        )


def check_default_language_function(report: Report, lang_files: Dict[str, Path], verse_files: Dict[str, Path]) -> None:
    """
    Verify that every language registered in `availableLanguages` is also
    handled by `getDefaultAppLanguage()`. Without this, the app won't
    auto-detect a user's system language even though the translation exists.
    """
    text = APP_STRINGS_FILE.read_text(encoding="utf-8")
    m = re.search(r'fun getDefaultAppLanguage\(\):\s*String\s*\{(.*?)\n\}', text, re.DOTALL)
    if not m:
        report.err("Could not locate `fun getDefaultAppLanguage()` in AppStrings.kt")
        return
    body = m.group(1)
    # Find every "XX" literal in the body
    codes_in_function = set(re.findall(r'"([A-Z]{2,3})"', body))

    pairs = parse_available_languages(report)
    for code, _ in pairs:
        if code not in codes_in_function:
            report.err(
                f"'{code}' is in `availableLanguages` but not handled in "
                f"`getDefaultAppLanguage()`. Add it to the when-branch so the "
                f"app auto-detects it from the system locale."
            )


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> int:
    strict = "--strict" in sys.argv[1:]

    report = Report()
    report.info(f"Repo root: {REPO_ROOT}")
    report.info(f"Strings dir: {STRINGS_DIR}")
    report.info(f"Assets dir: {ASSETS_DIR}")

    print("\n=== 1. UI strings schema ===")
    schema = parse_schema(report)
    if not schema:
        report.print_summary()
        return report.exit_code(strict)

    print("\n=== 2. UI strings files ===")
    lang_files = check_ui_strings(report, schema)

    print("\n=== 3. availableLanguages <-> StringsXX.kt parity ===")
    check_available_languages(report, lang_files)

    print("\n=== 4. getDefaultAppLanguage() coverage ===")
    check_default_language_function(report, lang_files, {})

    print("\n=== 5. Verse JSON files ===")
    verse_files = check_verse_json(report)

    print("\n=== 6. UI strings <-> verse JSON parity ===")
    check_ui_vs_verse_parity(report, lang_files, verse_files)

    print("\n=== 7. Bible copyright reminder ===")
    report.info(
        "Verse content must come from a PUBLIC-DOMAIN or freely-licensed "
        "Bible translation. See CONTRIBUTING.md for the list of approved "
        "versions per language. The contributor is responsible for "
        "declaring the source in their Pull Request description."
    )

    report.print_summary()
    return report.exit_code(strict)


if __name__ == "__main__":
    sys.exit(main())

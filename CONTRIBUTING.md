# Contributing to BibleLockScreen

First off, thank you for considering a contribution! 🙏

This document explains how to contribute **translations** to BibleLockScreen.
You don't need to be a programmer; if you can edit text files and open a
Pull Request on GitHub, you can help.

There are two kinds of translation you can contribute:

| What | Where | Difficulty |
|---|---|---|
| **UI strings** — buttons, labels, settings text | `app/src/main/java/com/daklok/biblelockscreen/strings/` | Easy |
| **Verse content** — the Bible verses shown on the lock screen | `app/src/main/assets/verses_XX.json` | Easy, but see ⚠️ below |

Both are required for a complete translation. The app currently supports:
**English, Slovak, Czech, Spanish, Italian, French, German, Hungarian, Polish.**

---

## Fallback behavior (read this — it makes partial translations okay)

**English is the default language.** If a user's system language isn't one
of the supported languages above, the app falls back to English — not
Slovak, even though Slovak is the original source language the app was
first written in.

**Missing strings fall back to English too, one string at a time.** Every
string in the app has an English default built in. A `StringsXX.kt` file
only needs to include the fields it has actually translated — any field
left out simply shows the English text in the running app instead of
failing to build or showing blank. This means:

- You do **not** need to finish translating all ~230 UI strings before
  opening a PR. A partial file with 20 translated strings is a perfectly
  good first contribution — the other ~210 will just show in English
  until someone (maybe you, in a follow-up PR) translates them.
- You also don't need to touch `AppStrings.kt` for this — the fallback is
  automatic. `AppStrings.kt` is only for adding a *new* string key that
  doesn't exist yet in any language.

This does **not** change the recommendation below to start from
`StringsTemplate.kt.txt` — it just means you're free to leave lines as
`<TODO>` (or delete them) instead of feeling like the PR isn't "done"
until every single line is translated.

---

## ⚠️ IMPORTANT: Bible copyright rules

Bible translations are **copyrighted works**. You may NOT paste verses from
copyrighted Bible versions (NIV, ESV, NLT, NRSV, CSB, NASB, MSG, GNT, etc.)
into this project. Doing so would force the maintainer to reject your PR
and could create legal problems for the project.

**Use one of these public-domain / freely-licensed sources:**

| Language | Public-domain version(s) |
|---|---|
| English | KJV, WEB (World English Bible), BBE (Bible in Basic English), YLT (Young's Literal) |
| Slovak | Roháčkov preklad (1936, public domain) — *check status before use* |
| Czech | Bible Kralická (1579–1593, public domain) |
| Spanish | Reina-Valera 1909 (public domain) |
| Italian | Diodati 1649 (public domain), Riveduta 1927 |
| French | Louis Segond 1910 (public domain) |
| German | Luther Bibel 1912 (public domain), Schlachter 1951 |
| Hungarian | Károli Biblia (1590, public domain) |
| Polish | Biblia Gdańska 1632 (public domain), Biblia Warszawska (check status) |

When in doubt: **the original maintainer's Slovak verses are sourced from
public-domain texts.** Match that standard.

In your Pull Request description, please state which Bible version you used
and confirm it is public domain.

---

## How to add a new UI translation

Each language has its own file in
[`app/src/main/java/com/daklok/biblelockscreen/strings/`](app/src/main/java/com/daklok/biblelockscreen/strings/).

### Step 1 — Pick a language code

Use an ISO 639-1 code in **uppercase**. Examples: `PT` (Portuguese),
`RU` (Russian), `UK` (Ukrainian), `NL` (Dutch), `SV` (Swedish), `TR` (Turkish),
`ZH` (Chinese), `JA` (Japanese), `RO` (Romanian), `EL` (Greek).

### Step 2 — Create the strings file

1. Copy [`StringsTemplate.kt.txt`](app/src/main/java/com/daklok/biblelockscreen/strings/StringsTemplate.kt.txt)
   and rename it to `StringsXX.kt` (replace `XX` with your code, and note
   the extension changes from `.kt.txt` to just `.kt`).
2. Open the file in any text editor (GitHub's web editor works fine). Every
   field looks like this:
   ```kotlin
   settings = "<TODO>", // "App Settings"
   ```
   The `// "App Settings"` part is the English original, there only for
   context — it's a comment, not code. Replace `<TODO>` with your
   translation (most editors let you double-click it to select it, then
   just type over it). **Do not change the property names on the left of `=`.**
3. Change the top two lines so they read:
   ```kotlin
   package com.daklok.biblelockscreen.strings

   // ptStrings -- Portuguese translation by <Your Name>
   val ptStrings = AppStrings(
   ```
   Use the **lowercase** code for the variable name (`ptStrings`, `ruStrings`,
   `ukStrings`, etc.).
4. Translate as many fields as you have time for. You don't have to finish
   every single line in one sitting — anything left as `<TODO>` (or simply
   deleted from the file) falls back to showing English in the app instead
   of breaking the build. See "Fallback behavior" above.
5. Preserve any `%s` placeholders verbatim — they are filled in at runtime
   (e.g. `"Active (every day at %s:00)"` becomes `"Ativo (todos os dias às %s:00)"`).
6. Preserve any escaped quotes (`\"`) and HTML-style markup exactly as in the
   original.

### Step 3 — Register the language

Open `AppStrings.kt` in the same folder and make two changes:

**(a)** Add your code to the `availableLanguages` list (keep alphabetical-ish order
with the existing entries):

```kotlin
val availableLanguages = listOf(
    "EN" to "English",
    "SK" to "Slovenčina",
    "CZ" to "Čeština",
    "ES" to "Español",
    "IT" to "Italiano",
    "FR" to "Français",
    "DE" to "Deutsch",
    "HU" to "Magyar",
    "PL" to "Polski",
    "PT" to "Português"   // ← add this line
)
```

**(b)** Add your code to the `getDefaultAppLanguage()` function so the app
auto-detects it from the user's system language:

```kotlin
fun getDefaultAppLanguage(): String {
    val sysLang = Locale.getDefault().language.uppercase()
    return when (sysLang) {
        "CS" -> "CZ"
        "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL", "PT" -> sysLang  // ← add "PT"
        else -> "EN"
    }
}
```

### Step 4 — Wire it into the language picker

Open `app/src/main/java/com/daklok/biblelockscreen/MainActivity.kt` and find
the `when` block that maps language codes to string instances (search for
`"EN" -> enStrings`). Add your case — and note the fallback at the bottom
is `enStrings`, not `skStrings`, even though Slovak was the original
language the app was written in:

```kotlin
"EN" -> enStrings
"SK" -> skStrings
"CZ" -> czStrings
// ... existing cases ...
"PL" -> plStrings
"PT" -> ptStrings   // ← add this line
else -> enStrings
```

### Step 5 — Add the verse content

Without verse content, users selecting your language will see UI in your
language but verses will fall back to English. To make a complete translation:

1. Copy `app/src/main/assets/verses_EN.json` to `verses_XX.json`
   (e.g. `verses_PT.json`).
2. Translate the `"text"` field of each entry into your language, using a
   **public-domain** Bible (see the ⚠️ section above).
3. Translate the `"ref"` field too (e.g. `"John 3:16"` → `"João 3:16"`).
4. Change the `"lang"` field of every entry from `"EN"` to your code (`"PT"`).

That's it — 370 verses. You don't have to do all of them in one PR; you can
submit a partial file with 20 verses and the maintainer can merge it as a
"preview" while you finish the rest.

### Step 6 — Open the Pull Request

1. Commit your changes with a message like:
   `i18n: add Portuguese translation (UI + verses)`
2. Push to your fork and open a Pull Request against `master`.
3. In the PR description, include:
    - The language code and name
    - The Bible version you used (and confirm it's public domain)
    - Your name/handle for the credits file (see below)

The CI workflow will automatically validate your files. If something is
wrong, it will leave a comment on the PR explaining what to fix.

---

## How to improve an existing translation

Find the relevant `StringsXX.kt` and/or `verses_XX.json` file, edit it
directly on GitHub, and open a PR. No special steps required.

If you spot a typo or awkward phrasing, please do report it — even small
fixes are welcome.

---

## Credits
Contributors are listed in [`TRANSLATORS.md`](/TRANSLATORS.md). When your PR
is merged, please also submit a tiny follow-up PR adding yourself to that
file under the appropriate language, or just ask the maintainer to do it.

---

## Validation script (optional, for local use)

If you'd like to check your work before pushing, you can run the validation
script locally:

```bash
python3 scripts/validate_translations.py
```

It checks:
- Every property assigned in a `StringsXX.kt` file exists in the `AppStrings`
  schema (catches typos).
- Every language code in `availableLanguages` has a corresponding
  `StringsXX.kt` file, and vice versa.
- Every `verses_XX.json` file is valid JSON and every entry has the
  required `text`, `ref`, and `lang` fields with the correct language code.
- UI strings files and verse JSON files are 1:1 (no orphan files in either
  direction).

The same checks run automatically on every Pull Request via
`.github/workflows/validate-translations.yml`.

---

## Questions?

- Open an issue with the `translation` label.
- Or just open a draft PR with your work-in-progress and ask there.

Thank you for helping make BibleLockScreen accessible to more people! 🌍
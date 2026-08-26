package com.daklok.biblelockscreen

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The favorite-card-style setting lives in SharedPreferences as a plain
 * String ([FavoriteCardStyle.PREF_KEY] → `style.name`). [SettingsBackupManager]
 * serializes *every* SharedPreferences entry generically (it iterates
 * `prefs.all`), so the style is backed up and restored automatically — no
 * special handling needed.
 *
 * These tests pin down the two properties that make that work safely:
 *  - every style's [Enum.name] round-trips back to the same style via
 *    [FavoriteCardStyle.fromPref] (so a backup made today restores
 *    correctly), and
 *  - an unknown/missing value (an older backup, or a future renamed enum)
 *    degrades to a sane default instead of crashing.
 */
class FavoriteCardStyleTest {

    @Test
    fun everyStyleRoundTripsThroughItsPrefName() {
        // This is the property the backup relies on: export writes
        // `style.name`, import hands that String back to fromPref.
        for (style in FavoriteCardStyle.entries) {
            val restored = FavoriteCardStyle.fromPref(style.name)
            assertEquals("style $style should round-trip through its name", style, restored)
        }
    }

    @Test
    fun nullPrefFallsBackToDefault() {
        // A backup from before this feature existed, or a fresh install.
        assertEquals(FavoriteCardStyle.QUOTE, FavoriteCardStyle.fromPref(null))
    }

    @Test
    fun unknownPrefFallsBackToDefault() {
        // A backup from a future/renamed enum value, restored into this
        // build. Must not crash and must pick a real style.
        assertEquals(FavoriteCardStyle.QUOTE, FavoriteCardStyle.fromPref("SOMETHING_ELSE"))
        assertEquals(FavoriteCardStyle.QUOTE, FavoriteCardStyle.fromPref(""))
    }

    @Test
    fun prefKeyIsStable() {
        // Renaming this key would silently break existing backups (the
        // stored style would no longer be found on restore). Lock it down.
        assertEquals("favorite_card_style", FavoriteCardStyle.PREF_KEY)
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import java.time.LocalDate

/**
 * Names and types of the Vault export files (docs/02-technical/13-vault-export.md).
 * The files themselves are formatted by the Rust core (corn-vault `export.rs`).
 */
object VaultExport {
    enum class LinksFormat(val extension: String, val mime: String) {
        /** The bookmarks file every browser imports. Recommended. */
        HTML("html", "text/html"),
        /** For spreadsheets and apps that take CSV. */
        CSV("csv", "text/csv"),
    }

    const val PASSWORDS_MIME = "text/csv"

    /** `corn-links-2026-11-13.html` (the phone's local date). */
    fun linksFileName(format: LinksFormat, date: LocalDate = LocalDate.now()): String = "corn-links-$date.${format.extension}"

    /** `corn-passwords-UNENCRYPTED-2026-11-13.csv`: the name itself says what's inside. */
    fun passwordsFileName(date: LocalDate = LocalDate.now()): String = "corn-passwords-UNENCRYPTED-$date.csv"
}

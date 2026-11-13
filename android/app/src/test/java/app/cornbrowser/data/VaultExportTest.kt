/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package app.cornbrowser.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultExportTest {
    private val day = LocalDate.of(2026, 1, 5)

    @Test
    fun fileNames() {
        assertEquals("corn-links-2026-01-05.html", VaultExport.linksFileName(VaultExport.LinksFormat.HTML, day))
        assertEquals("corn-links-2026-01-05.csv", VaultExport.linksFileName(VaultExport.LinksFormat.CSV, day))
        assertEquals("corn-passwords-UNENCRYPTED-2026-01-05.csv", VaultExport.passwordsFileName(day))
    }

    @Test
    fun types() {
        assertEquals("text/html", VaultExport.LinksFormat.HTML.mime)
        assertEquals("text/csv", VaultExport.LinksFormat.CSV.mime)
        assertEquals("text/csv", VaultExport.PASSWORDS_MIME)
    }
}

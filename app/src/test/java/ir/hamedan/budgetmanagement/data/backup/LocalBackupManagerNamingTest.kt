package ir.hamedan.budgetmanagement.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBackupManagerNamingTest {

    @Test
    fun `backup file name carries the app extension and a sortable timestamp`() {
        val name = LocalBackupManager.newFileName(0L)
        assertTrue(name.startsWith("cidna-backup-"))
        assertTrue(name.endsWith(".${LocalBackupManager.FILE_EXTENSION}"))
        assertTrue(Regex("""cidna-backup-\d{8}-\d{6}\.cidnabackup""").matches(name))
    }

    @Test
    fun `backups live in their own folder under Documents`() {
        assertEquals("Documents/Cidna/Backups", LocalBackupManager.DISPLAY_PATH)
    }
}
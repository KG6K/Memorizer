package com.example

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import com.example.data.SongRepository
import com.example.model.LoopConfig
import com.example.model.ScanFolderEntry
import com.example.player.AudioPlaybackEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScanFoldersRobolectricTest {

    class FakeDocumentsProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            val cols = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            val cursor = MatrixCursor(cols)
            val path = uri.toString()
            when {
                path.endsWith("/document/root_folder/children") -> {
                    cursor.addRow(arrayOf<Any>("root_track", "intro.mp3", "audio/mpeg", 1000L))
                    cursor.addRow(arrayOf<Any>("sub_folder", "Juz30", DocumentsContract.Document.MIME_TYPE_DIR, 1500L))
                }
                path.endsWith("/document/sub_folder/children") -> {
                    cursor.addRow(arrayOf<Any>("sub_track", "naba.mp3", "audio/mpeg", 2000L))
                }
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }

    @Test
    fun `SongRepository persists multiple scan folders and extracts folder display names`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = SongRepository(app)

        val entries = listOf(
            ScanFolderEntry(
                uriString = "content://com.android.externalstorage.documents/tree/primary%3AMusic%2FQuran",
                displayName = "Quran",
                trackCount = 12,
                addedAt = 5000L
            ),
            ScanFolderEntry(
                uriString = "content://com.android.externalstorage.documents/tree/primary%3ADownloads%2FLectures",
                displayName = "Lectures",
                trackCount = 4,
                addedAt = 6000L
            )
        )
        repo.saveScanFolders(entries)

        val loaded = repo.loadScanFolders()
        assertEquals(2, loaded.size)
        assertEquals("Quran", loaded[0].displayName)
        assertEquals(12, loaded[0].trackCount)
        assertEquals("Lectures", loaded[1].displayName)
        assertEquals(4, loaded[1].trackCount)

        val extractedName = repo.extractFolderDisplayName(
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAudio%2FTajweed")
        )
        assertEquals("Tajweed", extractedName)
    }

    @Test
    fun `scanSelectedFolders traverses subfolders and preserves existing loop configs`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val authority = "com.example.test.docs"
        Robolectric.buildContentProvider(FakeDocumentsProvider::class.java).create(authority)

        val treeUri = DocumentsContract.buildTreeDocumentUri(authority, "root_folder")
        val subTrackUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, "sub_track").toString()

        val savedLoop = LoopConfig(repeatCount = 8, delaySeconds = 6, playbackSpeed = 0.85f)
        val existingSong = SongRepository(app).extractSongFromUri(
            uri = Uri.parse(subTrackUri),
            folderName = "OldName",
            fallbackAddedAt = 1234L
        )!!.copy(savedLoopConfig = savedLoop, lastPlayedAt = 7777L)

        val repo = SongRepository(app)
        val entry = ScanFolderEntry(
            uriString = treeUri.toString(),
            displayName = "RootAudio",
            trackCount = 0,
            addedAt = 100L
        )

        val (songs, updatedEntries) = repo.scanSelectedFolders(
            scanFolders = listOf(entry),
            existingSongs = listOf(existingSong)
        )

        assertEquals(1, updatedEntries.size)
        assertEquals(2, updatedEntries[0].trackCount)
        assertEquals(2, songs.size)

        val rootSong = songs.first { it.uriString.endsWith("root_track") }
        val nestedSong = songs.first { it.uriString == subTrackUri }
        assertEquals("RootAudio", rootSong.folderName)
        assertEquals("Juz30", nestedSong.folderName)
        assertEquals(existingSong.id, nestedSong.id)
        assertEquals(8, nestedSong.savedLoopConfig?.repeatCount)
        assertEquals(6, nestedSong.savedLoopConfig?.delaySeconds)
        assertEquals(7777L, nestedSong.lastPlayedAt)
    }

    @Test
    fun `AudioPlaybackEngine clears scan folders on switch back to all device`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = SongRepository(app)
        val initialEntries = listOf(
            ScanFolderEntry(
                uriString = "content://com.android.externalstorage.documents/tree/primary%3AMusic",
                displayName = "Music",
                trackCount = 5,
                addedAt = 1000L
            )
        )
        repo.saveScanFolders(initialEntries)

        val engine = AudioPlaybackEngine.getInstance(app)
        val deadline = System.currentTimeMillis() + 3000L
        while (engine.uiState.value.scanFolders.isEmpty() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20L)
        }
        shadowOf(Looper.getMainLooper()).idle()

        engine.clearScanFoldersResetToAllDevice()
        val resetDeadline = System.currentTimeMillis() + 3000L
        while ((repo.loadScanFolders().isNotEmpty() || engine.uiState.value.isScanningLibrary) &&
            System.currentTimeMillis() < resetDeadline
        ) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20L)
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(engine.uiState.value.scanFolders.isEmpty())
        assertTrue(repo.loadScanFolders().isEmpty())
    }
}

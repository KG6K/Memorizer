package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.R
import com.example.model.AfterLoopAction
import com.example.model.FolderInfo
import com.example.model.LocalSong
import com.example.model.LoopConfig
import com.example.model.LoopPreset
import com.example.model.PlaybackHistoryItem
import com.example.model.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SongRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("loopwave_player_prefs", Context.MODE_PRIVATE)

    /**
     * Automatically scans device MediaStore for audio files across all supported formats
     * (MP3, WAV, FLAC, M4A, AAC, OGG, OPUS, AMR, 3GP, MIDI, WEBM, WMA, AIFF, ALAC)
     * and extracts their folders and metadata.
     */
    fun scanDeviceAudioMedia(): List<LocalSong> {
        val songs = mutableListOf<LocalSong>()
        val resolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE
        )
        // Include both standard music files and voice/recitation audio clips >= 1 second
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            resolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol) ?: "" else ""
                    val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    val nameForCheck = displayName.ifBlank { filePath.substringAfterLast('/') }
                    if (nameForCheck.isNotBlank() && !isAudioFile(nameForCheck, mimeType)) {
                        continue
                    }

                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.').ifBlank { "Audio Track" }
                    val rawArtist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: ""
                    val duration = cursor.getLong(durationCol)

                    val folderName = if (filePath.isNotBlank()) {
                        val parent = filePath.substringBeforeLast('/', "")
                        parent.substringAfterLast('/').ifBlank { "Music" }
                    } else "Music"

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId)

                    songs.add(
                        LocalSong(
                            id = "media_$mediaId",
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum,
                            durationMs = duration.coerceAtLeast(1000L),
                            uriString = contentUri.toString(),
                            isBuiltInSample = false,
                            folderName = folderName,
                            lastPlayedAt = 0L
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return songs
    }

    fun loadSongs(): List<LocalSong> {
        val rawJson = prefs.getString(KEY_SONGS, null)
        if (rawJson.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(rawJson)
            val result = mutableListOf<LocalSong>()
            var hadBuiltIn = false
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val song = parseSongJson(obj)
                if (song.isBuiltInSample || song.id.startsWith("builtin_sample_")) {
                    hadBuiltIn = true
                } else {
                    result.add(song)
                }
            }
            if (hadBuiltIn) {
                saveSongs(result)
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSongs(songs: List<LocalSong>) {
        val array = JSONArray()
        songs.forEach { song ->
            array.put(songToJson(song))
        }
        prefs.edit().putString(KEY_SONGS, array.toString()).apply()
    }

    fun loadActiveLoopConfig(): LoopConfig {
        val raw = prefs.getString(KEY_ACTIVE_LOOP_CONFIG, null) ?: return LoopConfig()
        return try {
            parseLoopConfigJson(JSONObject(raw))
        } catch (e: Exception) {
            LoopConfig()
        }
    }

    fun saveActiveLoopConfig(config: LoopConfig) {
        prefs.edit().putString(KEY_ACTIVE_LOOP_CONFIG, loopConfigToJson(config).toString()).apply()
    }

    fun loadPresets(): List<LoopPreset> {
        val builtIn = listOf(
            LoopPreset(
                id = "preset_hifz_5x_5s",
                name = "QuranCentral Hifz (5x • 5s Delay)",
                description = "Plays the verse/song 5 times with a 5-second pause between repetitions for active recitation.",
                repeatCount = 5,
                delaySeconds = 5,
                matchTrackDurationDelay = false,
                playbackSpeed = 1.0f,
                afterCompletionAction = AfterLoopAction.STOP,
                isBuiltIn = true
            ),
            LoopPreset(
                id = "preset_echo_3x",
                name = "Echo Memorization (3x • 1x Length Delay)",
                description = "Pauses for the exact duration of the audio track between each loop so you can repeat it back in full.",
                repeatCount = 3,
                delaySeconds = 0,
                matchTrackDurationDelay = true,
                playbackSpeed = 1.0f,
                afterCompletionAction = AfterLoopAction.STOP,
                isBuiltIn = true
            ),
            LoopPreset(
                id = "preset_deep_7x_10s",
                name = "Deep Retention (7x • 10s Delay)",
                description = "7 repetitions with a generous 10-second reflection pause before each replay.",
                repeatCount = 7,
                delaySeconds = 10,
                matchTrackDurationDelay = false,
                playbackSpeed = 1.0f,
                afterCompletionAction = AfterLoopAction.NEXT_SONG,
                isBuiltIn = true
            ),
            LoopPreset(
                id = "preset_slow_drill_10x",
                name = "Slow Articulation Drill (10x • 3s • 0.75x)",
                description = "Slows audio to 0.75x speed and loops 10 times with a 3-second breather.",
                repeatCount = 10,
                delaySeconds = 3,
                matchTrackDurationDelay = false,
                playbackSpeed = 0.75f,
                afterCompletionAction = AfterLoopAction.STOP,
                isBuiltIn = true
            ),
            LoopPreset(
                id = "preset_infinite_2s",
                name = "Continuous Focus Loop (∞ • 2s Delay)",
                description = "Repeats the current song indefinitely with a gentle 2-second gap between plays.",
                repeatCount = 0,
                delaySeconds = 2,
                matchTrackDurationDelay = false,
                playbackSpeed = 1.0f,
                afterCompletionAction = AfterLoopAction.STOP,
                isBuiltIn = true
            )
        )

        val customRaw = prefs.getString(KEY_CUSTOM_PRESETS, null)
        if (customRaw.isNullOrBlank()) return builtIn

        val customList = mutableListOf<LoopPreset>()
        try {
            val arr = JSONArray(customRaw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                customList.add(
                    LoopPreset(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Preset"),
                        description = obj.optString("description", ""),
                        repeatCount = obj.optInt("repeatCount", 3),
                        delaySeconds = obj.optInt("delaySeconds", 3),
                        matchTrackDurationDelay = obj.optBoolean("matchTrackDurationDelay", false),
                        playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat(),
                        afterCompletionAction = runCatching {
                            AfterLoopAction.valueOf(obj.optString("afterCompletionAction", AfterLoopAction.STOP.name))
                        }.getOrDefault(AfterLoopAction.STOP),
                        isBuiltIn = false
                    )
                )
            }
        } catch (_: Exception) {
        }
        return builtIn + customList
    }

    fun saveCustomPreset(preset: LoopPreset): List<LoopPreset> {
        val currentCustom = loadPresets().filter { !it.isBuiltIn }.toMutableList()
        currentCustom.add(0, preset.copy(isBuiltIn = false))
        persistCustomPresets(currentCustom)
        return loadPresets()
    }

    fun deleteCustomPreset(presetId: String): List<LoopPreset> {
        val currentCustom = loadPresets().filter { !it.isBuiltIn && it.id != presetId }
        persistCustomPresets(currentCustom)
        return loadPresets()
    }

    private fun persistCustomPresets(customPresets: List<LoopPreset>) {
        val arr = JSONArray()
        customPresets.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("repeatCount", p.repeatCount)
                put("delaySeconds", p.delaySeconds)
                put("matchTrackDurationDelay", p.matchTrackDurationDelay)
                put("playbackSpeed", p.playbackSpeed.toDouble())
                put("afterCompletionAction", p.afterCompletionAction.name)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, arr.toString()).apply()
    }

    fun importAudioUris(uris: List<Uri>): List<LocalSong> {
        val resolver = context.contentResolver
        val imported = mutableListOf<LocalSong>()
        for (uri in uris) {
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val folderGuess = uri.path?.substringBeforeLast('/')?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Imported Files"
            extractSongFromUri(uri, folderGuess)?.let { imported.add(it) }
        }
        return imported
    }

    fun importAudioFolder(treeUri: Uri): List<LocalSong> {
        val resolver = context.contentResolver
        try {
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }

        var folderName = "Scanned Folder"
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            folderName = docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Scanned Folder" }
        } catch (_: Exception) {
        }

        val discoveredUris = mutableListOf<Uri>()
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            collectAudioFilesFromTree(resolver, treeUri, docId, discoveredUris, maxDepth = 3)
        } catch (_: Exception) {
        }

        return discoveredUris.mapNotNull { extractSongFromUri(it, folderName) }
    }

    private fun collectAudioFilesFromTree(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        outUris: MutableList<Uri>,
        maxDepth: Int
    ) {
        if (maxDepth < 0 || outUris.size >= 150) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    collectAudioFilesFromTree(resolver, treeUri, docId, outUris, maxDepth - 1)
                } else if (isAudioFile(name, mime)) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    outUris.add(fileUri)
                }
            }
        }
    }

    private fun isAudioFile(name: String, mime: String): Boolean {
        if (mime.startsWith("audio/", ignoreCase = true) ||
            mime.equals("application/ogg", ignoreCase = true) ||
            mime.equals("application/opus", ignoreCase = true) ||
            mime.equals("application/x-flac", ignoreCase = true)
        ) {
            return true
        }
        val lower = name.lowercase()
        return lower.endsWith(".mp3") ||
            lower.endsWith(".wav") ||
            lower.endsWith(".flac") ||
            lower.endsWith(".m4a") ||
            lower.endsWith(".aac") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".ogg") ||
            lower.endsWith(".oga") ||
            lower.endsWith(".opus") ||
            lower.endsWith(".amr") ||
            lower.endsWith(".awb") ||
            lower.endsWith(".3gp") ||
            lower.endsWith(".3ga") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".mkv") ||
            lower.endsWith(".mid") ||
            lower.endsWith(".midi") ||
            lower.endsWith(".xmf") ||
            lower.endsWith(".mxmf") ||
            lower.endsWith(".imy") ||
            lower.endsWith(".ota") ||
            lower.endsWith(".rtttl") ||
            lower.endsWith(".rtx") ||
            lower.endsWith(".wma") ||
            lower.endsWith(".aiff") ||
            lower.endsWith(".aif") ||
            lower.endsWith(".alac")
    }

    fun extractSongFromUri(uri: Uri, folderName: String = "Imported Files"): LocalSong? {
        val resolver = context.contentResolver
        var displayName = ""
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = c.getString(idx) ?: ""
                }
            }
        } catch (_: Exception) {
        }

        val fallbackTitle = displayName
            .substringBeforeLast('.')
            .ifBlank { uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Audio Track" }

        var title = fallbackTitle
        var artist = "Unknown Artist"
        var album = ""
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            if (!metaTitle.isNullOrBlank()) title = metaTitle.trim()
            if (!metaArtist.isNullOrBlank()) artist = metaArtist.trim()
            if (!metaAlbum.isNullOrBlank()) album = metaAlbum.trim()
            durationMs = metaDuration?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return LocalSong(
            id = "song_${uri.toString().hashCode()}_${System.currentTimeMillis() % 10000}",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs.coerceAtLeast(1000L),
            uriString = uri.toString(),
            isBuiltInSample = false,
            folderName = folderName.ifBlank { "Music" },
            lastPlayedAt = 0L
        )
    }

    fun buildBuiltInSampleSongs(): List<LocalSong> = emptyList()

    private fun songToJson(song: LocalSong): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationMs", song.durationMs)
            put("uriString", song.uriString)
            put("isBuiltInSample", song.isBuiltInSample)
            put("addedAt", song.addedAt)
            put("folderName", song.folderName)
            put("lastPlayedAt", song.lastPlayedAt)
            if (song.savedLoopConfig != null) {
                put("savedLoopConfig", loopConfigToJson(song.savedLoopConfig))
            }
        }
    }

    private fun parseSongJson(obj: JSONObject): LocalSong {
        val loopObj = obj.optJSONObject("savedLoopConfig")
        val isSample = obj.optBoolean("isBuiltInSample", false)
        val rawAlbum = obj.optString("album", "")
        val cleanAlbum = if (rawAlbum == "Local Music" || rawAlbum == "Local Album" || rawAlbum == "Device Storage") "" else rawAlbum
        val rawArtist = obj.optString("artist", "Unknown Artist")
        val cleanArtist = if (rawArtist == "Device Audio" || rawArtist == "Local Device Audio" || rawArtist == "Local Artist") "Unknown Artist" else rawArtist
        return LocalSong(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", "Audio Track"),
            artist = cleanArtist,
            album = cleanAlbum,
            durationMs = obj.optLong("durationMs", 9000L),
            uriString = obj.optString("uriString", ""),
            isBuiltInSample = isSample,
            addedAt = obj.optLong("addedAt", System.currentTimeMillis()),
            folderName = obj.optString("folderName", "Music"),
            lastPlayedAt = obj.optLong("lastPlayedAt", 0L),
            savedLoopConfig = loopObj?.let { parseLoopConfigJson(it) }
        )
    }

    private fun loopConfigToJson(config: LoopConfig): JSONObject {
        return JSONObject().apply {
            put("repeatCount", config.repeatCount)
            put("delaySeconds", config.delaySeconds)
            put("matchTrackDurationDelay", config.matchTrackDurationDelay)
            put("sectionStartMs", config.sectionStartMs)
            if (config.sectionEndMs != null) {
                put("sectionEndMs", config.sectionEndMs)
            }
            put("playbackSpeed", config.playbackSpeed.toDouble())
            put("afterCompletionAction", config.afterCompletionAction.name)
        }
    }

    private fun parseLoopConfigJson(obj: JSONObject): LoopConfig {
        val hasEnd = obj.has("sectionEndMs") && !obj.isNull("sectionEndMs")
        return LoopConfig(
            repeatCount = obj.optInt("repeatCount", 3),
            delaySeconds = obj.optInt("delaySeconds", 3),
            matchTrackDurationDelay = obj.optBoolean("matchTrackDurationDelay", false),
            sectionStartMs = obj.optLong("sectionStartMs", 0L),
            sectionEndMs = if (hasEnd) obj.optLong("sectionEndMs") else null,
            playbackSpeed = obj.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.5f, 2.0f),
            afterCompletionAction = runCatching {
                AfterLoopAction.valueOf(obj.optString("afterCompletionAction", AfterLoopAction.STOP.name))
            }.getOrDefault(AfterLoopAction.STOP)
        )
    }

    // --- Playlists Management ---
    fun loadPlaylists(): List<Playlist> {
        val rawJson = prefs.getString(KEY_PLAYLISTS, null)
        if (rawJson.isNullOrBlank()) {
            return emptyList()
        }
        val builtInPlaylistIds = setOf("pl_memorization_drill", "pl_studio_favorites", "pl_daily_review")
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<Playlist>()
            var hadBuiltIn = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                if (id in builtInPlaylistIds) {
                    hadBuiltIn = true
                    continue
                }
                val songIdsArr = obj.optJSONArray("songIds") ?: JSONArray()
                val songIds = mutableListOf<String>()
                for (j in 0 until songIdsArr.length()) {
                    val sId = songIdsArr.getString(j)
                    if (!sId.startsWith("builtin_sample_")) {
                        songIds.add(sId)
                    }
                }
                list.add(
                    Playlist(
                        id = id,
                        name = obj.optString("name", "Playlist"),
                        description = obj.optString("description", ""),
                        songIds = songIds,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastPlayedAt = obj.optLong("lastPlayedAt", 0L)
                    )
                )
            }
            if (hadBuiltIn) {
                savePlaylists(list)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("createdAt", pl.createdAt)
                put("lastPlayedAt", pl.lastPlayedAt)
                val sArr = JSONArray()
                pl.songIds.forEach { sArr.put(it) }
                put("songIds", sArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    fun updatePlaylistLastPlayed(playlistName: String) {
        val current = loadPlaylists().toMutableList()
        val index = current.indexOfFirst { it.name.equals(playlistName, ignoreCase = true) }
        if (index >= 0) {
            current[index] = current[index].copy(lastPlayedAt = System.currentTimeMillis())
            savePlaylists(current)
        }
    }

    // --- Playback History Management ---
    fun loadHistory(): List<PlaybackHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null)
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PlaybackHistoryItem>()
            var hadBuiltIn = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val songId = obj.optString("songId", "")
                if (songId.startsWith("builtin_sample_")) {
                    hadBuiltIn = true
                    continue
                }
                list.add(
                    PlaybackHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        songId = songId,
                        songTitle = obj.optString("songTitle", ""),
                        artist = obj.optString("artist", ""),
                        folderName = obj.optString("folderName", "Music"),
                        playlistName = obj.optString("playlistName", "All Songs"),
                        playedAt = obj.optLong("playedAt", System.currentTimeMillis())
                    )
                )
            }
            if (hadBuiltIn) {
                saveHistory(list)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHistory(history: List<PlaybackHistoryItem>) {
        val arr = JSONArray()
        history.take(60).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("songId", item.songId)
                put("songTitle", item.songTitle)
                put("artist", item.artist)
                put("folderName", item.folderName)
                put("playlistName", item.playlistName)
                put("playedAt", item.playedAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun recordPlayback(song: LocalSong, folderName: String, playlistName: String): List<PlaybackHistoryItem> {
        val current = loadHistory().toMutableList()
        val newItem = PlaybackHistoryItem(
            id = "hist_${UUID.randomUUID()}",
            songId = song.id,
            songTitle = song.title,
            artist = song.artist,
            folderName = folderName.ifBlank { song.folderName },
            playlistName = playlistName.ifBlank { "All Songs" },
            playedAt = System.currentTimeMillis()
        )
        // Put newest at index 0
        current.add(0, newItem)
        saveHistory(current)
        return current
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_SONGS = "saved_local_songs_v1"
        private const val KEY_ACTIVE_LOOP_CONFIG = "active_loop_config_v1"
        private const val KEY_CUSTOM_PRESETS = "custom_loop_presets_v1"
        private const val KEY_PLAYLISTS = "saved_playlists_v1"
        private const val KEY_HISTORY = "playback_history_v1"
    }
}

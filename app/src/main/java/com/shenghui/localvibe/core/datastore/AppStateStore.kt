package com.shenghui.localvibe.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.appStateDataStore by preferencesDataStore(name = "app_state")

class AppStateStore(private val context: Context) {
    suspend fun loadFolders(): List<PersistedFolder> {
        val json = context.appStateDataStore.data.first()[FoldersKey].orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                PersistedFolder(
                    folderId = item.getString("folderId"),
                    name = item.getString("name"),
                    uri = item.getString("uri"),
                    targetType = item.getString("targetType"),
                    source = item.optString("source", "MANUAL"),
                    addedAt = item.optLong("addedAt", 0L),
                    lastScannedAt = item.optLong("lastScannedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun upsertFolder(folder: PersistedFolder) {
        val next = loadFolders()
            .filterNot { it.uri == folder.uri && it.targetType == folder.targetType }
            .plus(folder)
        saveFolders(next)
    }

    suspend fun clearFolders() {
        saveFolders(emptyList())
    }

    suspend fun getPersistedBookFiles(): List<PersistedBookFile> {
        val json = context.appStateDataStore.data.first()[BookFilesKey].orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                PersistedBookFile(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    uri = item.getString("uri"),
                    addedAt = item.optLong("addedAt", 0L),
                    progressPercent = item.optInt("progressPercent", 0),
                    source = item.optString("source", "FILE")
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun savePersistedBookFiles(files: List<PersistedBookFile>) {
        saveBookFiles(files.distinctBy { it.uri })
    }

    suspend fun upsertPersistedBookFiles(files: List<PersistedBookFile>) {
        val incomingUris = files.map { it.uri }.toSet()
        val next = getPersistedBookFiles()
            .filterNot { it.uri in incomingUris }
            .plus(files)
        saveBookFiles(next)
    }

    suspend fun clearPersistedBookFiles() {
        saveBookFiles(emptyList())
    }

    suspend fun loadBookFiles(): List<PersistedBookFile> = getPersistedBookFiles()

    suspend fun upsertBookFile(file: PersistedBookFile) {
        upsertPersistedBookFiles(listOf(file))
    }

    suspend fun upsertBookFiles(files: List<PersistedBookFile>) {
        upsertPersistedBookFiles(files)
    }

    suspend fun clearBookFiles() {
        clearPersistedBookFiles()
    }

    suspend fun loadProgress(): List<PersistedPlaybackProgress> {
        val json = context.appStateDataStore.data.first()[ProgressKey].orEmpty()
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                PersistedPlaybackProgress(
                    mediaUri = item.getString("mediaUri"),
                    mediaType = item.getString("mediaType"),
                    positionMs = item.optLong("positionMs", 0L),
                    updatedAt = item.optLong("updatedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveProgress(progress: PersistedPlaybackProgress) {
        val next = loadProgress()
            .filterNot { it.mediaUri == progress.mediaUri && it.mediaType == progress.mediaType }
            .let { existing ->
                if (progress.positionMs > 0L) existing.plus(progress) else existing
            }
        saveProgressList(next)
    }

    suspend fun clearProgress() {
        saveProgressList(emptyList())
        context.appStateDataStore.edit { prefs ->
            prefs.remove(RecentVideoUriKey)
            prefs.remove(RecentAudioUriKey)
        }
    }

    suspend fun loadRecentVideoUri(): String? {
        return context.appStateDataStore.data.first()[RecentVideoUriKey]
    }

    suspend fun saveRecentVideoUri(uri: String?) {
        context.appStateDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(RecentVideoUriKey)
            } else {
                prefs[RecentVideoUriKey] = uri
            }
        }
    }

    suspend fun loadRecentAudioUri(): String? {
        return context.appStateDataStore.data.first()[RecentAudioUriKey]
    }

    suspend fun saveRecentAudioUri(uri: String?) {
        context.appStateDataStore.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(RecentAudioUriKey)
            } else {
                prefs[RecentAudioUriKey] = uri
            }
        }
    }

    private suspend fun saveFolders(folders: List<PersistedFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("folderId", folder.folderId)
                    .put("name", folder.name)
                    .put("uri", folder.uri)
                    .put("targetType", folder.targetType)
                    .put("source", folder.source)
                    .put("addedAt", folder.addedAt)
                    .put("lastScannedAt", folder.lastScannedAt)
            )
        }
        context.appStateDataStore.edit { prefs ->
            prefs[FoldersKey] = array.toString()
        }
    }

    private suspend fun saveProgressList(progressList: List<PersistedPlaybackProgress>) {
        val array = JSONArray()
        progressList.forEach { progress ->
            array.put(
                JSONObject()
                    .put("mediaUri", progress.mediaUri)
                    .put("mediaType", progress.mediaType)
                    .put("positionMs", progress.positionMs)
                    .put("updatedAt", progress.updatedAt)
            )
        }
        context.appStateDataStore.edit { prefs ->
            prefs[ProgressKey] = array.toString()
        }
    }

    private suspend fun saveBookFiles(files: List<PersistedBookFile>) {
        val array = JSONArray()
        files.forEach { file ->
            array.put(
                JSONObject()
                    .put("id", file.id)
                    .put("name", file.name)
                    .put("uri", file.uri)
                    .put("addedAt", file.addedAt)
                    .put("progressPercent", file.progressPercent)
                    .put("source", file.source)
            )
        }
        context.appStateDataStore.edit { prefs ->
            prefs[BookFilesKey] = array.toString()
        }
    }

    private companion object {
        val FoldersKey = stringPreferencesKey("manual_folders_json")
        val BookFilesKey = stringPreferencesKey("book_files_json")
        val ProgressKey = stringPreferencesKey("playback_progress_json")
        val RecentVideoUriKey = stringPreferencesKey("recent_video_uri")
        val RecentAudioUriKey = stringPreferencesKey("recent_audio_uri")
    }
}

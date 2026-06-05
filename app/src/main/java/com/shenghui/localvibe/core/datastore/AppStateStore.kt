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

    suspend fun removeFolders(targetType: String, folderIds: Collection<String>) {
        val normalizedIds = folderIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedIds.isEmpty()) return
        val next = loadFolders().filterNot { folder ->
            folder.targetType == targetType &&
                (folder.folderId.trim() in normalizedIds || folder.uri.trim() in normalizedIds)
        }
        saveFolders(next)
    }

    suspend fun getPersistedBookFiles(): List<PersistedBookFile> {
        val json = context.appStateDataStore.data.first()[BookFilesKey].orEmpty()
        return decodeBookFiles(json)
    }

    suspend fun savePersistedBookFiles(files: List<PersistedBookFile>) {
        saveBookFiles(files.distinctBy { it.uri.trim() })
    }

    suspend fun upsertPersistedBookFiles(files: List<PersistedBookFile>) {
        val incomingUris = files.map { it.uri.trim() }.toSet()
        context.appStateDataStore.edit { prefs ->
            val next = decodeBookFiles(prefs[BookFilesKey].orEmpty())
                .filterNot { it.uri.trim() in incomingUris }
                .plus(files)
                .distinctBy { it.uri.trim() }
            prefs[BookFilesKey] = encodeBookFiles(next)
        }
    }

    suspend fun clearPersistedBookFiles() {
        saveBookFiles(emptyList())
    }

    suspend fun removePersistedBookFile(uri: String) {
        val normalizedUri = uri.trim()
        context.appStateDataStore.edit { prefs ->
            val next = decodeBookFiles(prefs[BookFilesKey].orEmpty())
                .filterNot { it.uri.trim() == normalizedUri }
            prefs[BookFilesKey] = encodeBookFiles(next)
        }
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

    suspend fun loadBookProgress(): List<PersistedBookProgress> {
        val json = context.appStateDataStore.data.first()[BookProgressKey].orEmpty()
        return decodeBookProgress(json)
    }

    suspend fun saveBookProgress(progress: PersistedBookProgress) {
        val normalizedUri = progress.uri.trim()
        if (normalizedUri.isBlank()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeBookProgress(prefs[BookProgressKey].orEmpty())
                .filterNot { it.uri.trim() == normalizedUri }
                .plus(progress.copy(uri = normalizedUri))
                .distinctBy { it.uri.trim() }
            prefs[BookProgressKey] = encodeBookProgress(next)
        }
    }

    suspend fun removeBookProgress(uri: String) {
        val normalizedUri = uri.trim()
        context.appStateDataStore.edit { prefs ->
            val next = decodeBookProgress(prefs[BookProgressKey].orEmpty())
                .filterNot { it.uri.trim() == normalizedUri }
            prefs[BookProgressKey] = encodeBookProgress(next)
        }
    }

    suspend fun clearBookProgress() {
        context.appStateDataStore.edit { prefs ->
            prefs.remove(BookProgressKey)
        }
    }

    suspend fun loadHiddenAudioUris(): Set<String> {
        val json = context.appStateDataStore.data.first()[HiddenAudioUrisKey].orEmpty()
        if (json.isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(json)
            buildSet {
                repeat(array.length()) { index ->
                    val value = array.opt(index)
                    val uri = when (value) {
                        is JSONObject -> value.optString("uri")
                        else -> value?.toString().orEmpty()
                    }.trim()
                    if (uri.isNotBlank()) add(uri)
                }
            }
        }.getOrDefault(emptySet())
    }

    suspend fun hideAudioUris(uris: Collection<String>) {
        val normalizedUris = uris.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedUris.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeHiddenAudioUris(prefs[HiddenAudioUrisKey].orEmpty())
                .plus(normalizedUris)
                .toSet()
            prefs[HiddenAudioUrisKey] = encodeHiddenAudioUris(next)
        }
    }

    suspend fun clearHiddenAudioUris() {
        context.appStateDataStore.edit { prefs ->
            prefs.remove(HiddenAudioUrisKey)
        }
    }

    suspend fun loadHiddenVideoUris(): Set<String> {
        val json = context.appStateDataStore.data.first()[HiddenVideoUrisKey].orEmpty()
        if (json.isBlank()) return emptySet()
        return decodeHiddenVideoUris(json)
    }

    suspend fun hideVideoUris(uris: Collection<String>) {
        val normalizedUris = uris.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedUris.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeHiddenVideoUris(prefs[HiddenVideoUrisKey].orEmpty())
                .plus(normalizedUris)
                .toSet()
            prefs[HiddenVideoUrisKey] = encodeHiddenVideoUris(next)
        }
    }

    suspend fun clearHiddenVideoUris() {
        context.appStateDataStore.edit { prefs ->
            prefs.remove(HiddenVideoUrisKey)
        }
    }

    suspend fun loadHiddenVideoFolderIds(): Set<String> {
        val json = context.appStateDataStore.data.first()[HiddenVideoFolderIdsKey].orEmpty()
        if (json.isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(json)
            buildSet {
                repeat(array.length()) { index ->
                    val value = array.opt(index)
                    val folderId = when (value) {
                        is JSONObject -> value.optString("folderId")
                        else -> value?.toString().orEmpty()
                    }.trim()
                    if (folderId.isNotBlank()) add(folderId)
                }
            }
        }.getOrDefault(emptySet())
    }

    suspend fun hideVideoFolderIds(folderIds: Collection<String>) {
        val normalizedIds = folderIds.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedIds.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeHiddenVideoFolderIds(prefs[HiddenVideoFolderIdsKey].orEmpty())
                .plus(normalizedIds)
                .toSet()
            prefs[HiddenVideoFolderIdsKey] = encodeHiddenVideoFolderIds(next)
        }
    }

    suspend fun clearHiddenVideoFolderIds() {
        context.appStateDataStore.edit { prefs ->
            prefs.remove(HiddenVideoFolderIdsKey)
        }
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
        context.appStateDataStore.edit { prefs ->
            prefs[BookFilesKey] = encodeBookFiles(files)
        }
    }

    private fun decodeBookFiles(json: String): List<PersistedBookFile> {
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

    private fun encodeBookFiles(files: List<PersistedBookFile>): String {
        val array = JSONArray()
        files.distinctBy { it.uri.trim() }.forEach { file ->
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
        return array.toString()
    }

    private fun decodeBookProgress(json: String): List<PersistedBookProgress> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                PersistedBookProgress(
                    uri = item.getString("uri"),
                    paragraphIndex = item.optInt("paragraphIndex", 0).coerceAtLeast(0),
                    totalParagraphs = item.optInt("totalParagraphs", 0).coerceAtLeast(0),
                    updatedAt = item.optLong("updatedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeBookProgress(progressList: List<PersistedBookProgress>): String {
        val array = JSONArray()
        progressList.distinctBy { it.uri.trim() }.forEach { progress ->
            array.put(
                JSONObject()
                    .put("uri", progress.uri)
                    .put("paragraphIndex", progress.paragraphIndex)
                    .put("totalParagraphs", progress.totalParagraphs)
                    .put("updatedAt", progress.updatedAt)
            )
        }
        return array.toString()
    }

    private fun decodeHiddenAudioUris(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(json)
            buildSet {
                repeat(array.length()) { index ->
                    val value = array.opt(index)
                    val uri = when (value) {
                        is JSONObject -> value.optString("uri")
                        else -> value?.toString().orEmpty()
                    }.trim()
                    if (uri.isNotBlank()) add(uri)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun encodeHiddenAudioUris(uris: Set<String>): String {
        val array = JSONArray()
        uris.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { uri ->
                array.put(
                    JSONObject()
                        .put("uri", uri)
                        .put("mediaType", "AUDIO")
                        .put("hiddenAt", System.currentTimeMillis())
                )
        }
        return array.toString()
    }

    private fun decodeHiddenVideoUris(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(json)
            buildSet {
                repeat(array.length()) { index ->
                    val value = array.opt(index)
                    val uri = when (value) {
                        is JSONObject -> value.optString("uri")
                        else -> value?.toString().orEmpty()
                    }.trim()
                    if (uri.isNotBlank()) add(uri)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun encodeHiddenVideoUris(uris: Set<String>): String {
        val array = JSONArray()
        uris.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { uri ->
                array.put(
                    JSONObject()
                        .put("uri", uri)
                        .put("mediaType", "VIDEO")
                        .put("hiddenAt", System.currentTimeMillis())
                )
            }
        return array.toString()
    }

    private fun decodeHiddenVideoFolderIds(json: String): Set<String> {
        if (json.isBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(json)
            buildSet {
                repeat(array.length()) { index ->
                    val value = array.opt(index)
                    val folderId = when (value) {
                        is JSONObject -> value.optString("folderId")
                        else -> value?.toString().orEmpty()
                    }.trim()
                    if (folderId.isNotBlank()) add(folderId)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun encodeHiddenVideoFolderIds(folderIds: Set<String>): String {
        val array = JSONArray()
        folderIds.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { folderId ->
                array.put(
                    JSONObject()
                        .put("folderId", folderId)
                        .put("mediaType", "VIDEO")
                        .put("hiddenAt", System.currentTimeMillis())
                )
            }
        return array.toString()
    }

    private companion object {
        val FoldersKey = stringPreferencesKey("manual_folders_json")
        val BookFilesKey = stringPreferencesKey("book_files_json")
        val BookProgressKey = stringPreferencesKey("book_progress_json")
        val HiddenAudioUrisKey = stringPreferencesKey("hidden_audio_uris_json")
        val HiddenVideoUrisKey = stringPreferencesKey("hidden_video_uris_json")
        val HiddenVideoFolderIdsKey = stringPreferencesKey("hidden_video_folder_ids_json")
        val ProgressKey = stringPreferencesKey("playback_progress_json")
        val RecentVideoUriKey = stringPreferencesKey("recent_video_uri")
        val RecentAudioUriKey = stringPreferencesKey("recent_audio_uri")
    }
}

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

    suspend fun loadFavoriteAudioUris(): Set<String> {
        val json = context.appStateDataStore.data.first()[FavoriteAudioUrisKey].orEmpty()
        if (json.isBlank()) return emptySet()
        return decodeAudioUriSet(json)
    }

    suspend fun saveFavoriteAudioUris(uris: Set<String>) {
        context.appStateDataStore.edit { prefs ->
            prefs[FavoriteAudioUrisKey] = encodeAudioUriSet(uris)
        }
    }

    suspend fun loadAudioRecentRecords(): List<PersistedAudioRecentRecord> {
        val json = context.appStateDataStore.data.first()[RecentAudioRecordsKey].orEmpty()
        if (json.isBlank()) return emptyList()
        return decodeAudioRecentRecords(json)
    }

    suspend fun saveAudioRecentRecords(records: List<PersistedAudioRecentRecord>) {
        context.appStateDataStore.edit { prefs ->
            prefs[RecentAudioRecordsKey] = encodeAudioRecentRecords(records)
        }
    }

    suspend fun loadHiddenVideoUris(): Set<String> {
        val json = context.appStateDataStore.data.first()[HiddenVideoUrisKey].orEmpty()
        if (json.isBlank()) return emptySet()
        return decodeHiddenVideoUris(json)
    }

    suspend fun loadHiddenVideoRecords(): List<PersistedVideoVisibilityRecord> {
        val json = context.appStateDataStore.data.first()[HiddenVideoUrisKey].orEmpty()
        return decodeVideoVisibilityRecords(json, idKey = "uri")
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

    suspend fun hideVideoFiles(files: Collection<com.shenghui.localvibe.core.scanner.LocalMediaFile>) {
        val records = files
            .filter { it.type == com.shenghui.localvibe.core.scanner.LocalMediaType.VIDEO }
            .map { file ->
                PersistedVideoVisibilityRecord(
                    id = file.uri.trim(),
                    name = file.name.ifBlank { file.uri.trim() },
                    uri = file.uri.trim(),
                    path = file.parentFolderName,
                    recordedAt = System.currentTimeMillis()
                )
            }
            .filter { it.uri.isNotBlank() }
        if (records.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[HiddenVideoUrisKey].orEmpty(), idKey = "uri")
                .filterNot { existing -> records.any { it.uri == existing.uri } }
                .plus(records)
            prefs[HiddenVideoUrisKey] = encodeVideoVisibilityRecords(next, idKey = "uri")
        }
    }

    suspend fun removeHiddenVideoUris(uris: Collection<String>) {
        val normalizedUris = uris.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedUris.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[HiddenVideoUrisKey].orEmpty(), idKey = "uri")
                .filterNot { it.uri.trim() in normalizedUris }
            prefs[HiddenVideoUrisKey] = encodeVideoVisibilityRecords(next, idKey = "uri")
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

    suspend fun loadHiddenVideoFolderRecords(): List<PersistedVideoVisibilityRecord> {
        val json = context.appStateDataStore.data.first()[HiddenVideoFolderIdsKey].orEmpty()
        return decodeVideoVisibilityRecords(json, idKey = "folderId")
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

    suspend fun hideVideoFolderRecords(folders: Collection<PersistedVideoVisibilityRecord>) {
        val records = folders.filter { it.id.isNotBlank() }
        if (records.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[HiddenVideoFolderIdsKey].orEmpty(), idKey = "folderId")
                .filterNot { existing -> records.any { it.id == existing.id } }
                .plus(records)
            prefs[HiddenVideoFolderIdsKey] = encodeVideoVisibilityRecords(next, idKey = "folderId")
        }
    }

    suspend fun removeHiddenVideoFolderIds(folderIds: Collection<String>) {
        val normalizedIds = folderIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedIds.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[HiddenVideoFolderIdsKey].orEmpty(), idKey = "folderId")
                .filterNot { it.id.trim() in normalizedIds || it.uri.trim() in normalizedIds }
            prefs[HiddenVideoFolderIdsKey] = encodeVideoVisibilityRecords(next, idKey = "folderId")
        }
    }

    suspend fun clearHiddenVideoFolderIds() {
        context.appStateDataStore.edit { prefs ->
            prefs.remove(HiddenVideoFolderIdsKey)
        }
    }

    suspend fun loadUnavailableVideoRecords(): List<PersistedVideoVisibilityRecord> {
        val json = context.appStateDataStore.data.first()[UnavailableVideoRecordsKey].orEmpty()
        return decodeVideoVisibilityRecords(json, idKey = "uri")
    }

    suspend fun upsertUnavailableVideoRecord(file: com.shenghui.localvibe.core.scanner.LocalMediaFile) {
        if (file.type != com.shenghui.localvibe.core.scanner.LocalMediaType.VIDEO) return
        val uri = file.uri.trim()
        if (uri.isBlank()) return
        val record = PersistedVideoVisibilityRecord(
            id = uri,
            name = file.name.ifBlank { uri },
            uri = uri,
            path = file.parentFolderName,
            recordedAt = System.currentTimeMillis()
        )
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[UnavailableVideoRecordsKey].orEmpty(), idKey = "uri")
                .filterNot { it.uri == uri }
                .plus(record)
            prefs[UnavailableVideoRecordsKey] = encodeVideoVisibilityRecords(next, idKey = "uri")
        }
    }

    suspend fun removeUnavailableVideoUris(uris: Collection<String>) {
        val normalizedUris = uris.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (normalizedUris.isEmpty()) return
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoVisibilityRecords(prefs[UnavailableVideoRecordsKey].orEmpty(), idKey = "uri")
                .filterNot { it.uri.trim() in normalizedUris }
            prefs[UnavailableVideoRecordsKey] = encodeVideoVisibilityRecords(next, idKey = "uri")
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

    suspend fun loadVideoFolderPlaybackSpeeds(): Map<String, Float> {
        val json = context.appStateDataStore.data.first()[VideoFolderPlaybackSpeedKey].orEmpty()
        return decodeVideoFolderPlaybackSpeeds(json)
    }

    suspend fun saveVideoFolderPlaybackSpeed(folderKey: String, speed: Float) {
        val normalizedKey = folderKey.trim()
        if (normalizedKey.isBlank()) return
        val safeSpeed = speed.takeIf(::isValidVideoFolderPlaybackSpeed) ?: DEFAULT_VIDEO_PLAYBACK_SPEED
        context.appStateDataStore.edit { prefs ->
            val next = decodeVideoFolderPlaybackSpeeds(prefs[VideoFolderPlaybackSpeedKey].orEmpty())
                .plus(normalizedKey to safeSpeed)
            prefs[VideoFolderPlaybackSpeedKey] = encodeVideoFolderPlaybackSpeeds(next)
        }
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

    private fun decodeAudioUriSet(json: String): Set<String> {
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

    private fun encodeAudioUriSet(uris: Set<String>): String {
        val array = JSONArray()
        uris.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { uri ->
                array.put(JSONObject().put("uri", uri))
            }
        return array.toString()
    }

    private fun decodeAudioRecentRecords(json: String): List<PersistedAudioRecentRecord> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val value = array.opt(index)
                when (value) {
                    is JSONObject -> PersistedAudioRecentRecord(
                        uri = value.optString("uri").trim(),
                        playedAt = value.optLong("playedAt", 0L)
                    )
                    else -> PersistedAudioRecentRecord(
                        uri = value?.toString().orEmpty().trim(),
                        playedAt = 0L
                    )
                }
            }
                .filter { it.uri.isNotBlank() }
                .sortedByDescending { it.playedAt }
                .distinctBy { it.uri }
                .take(MAX_AUDIO_RECENT_RECORDS)
        }.getOrDefault(emptyList())
    }

    private fun encodeAudioRecentRecords(records: List<PersistedAudioRecentRecord>): String {
        val array = JSONArray()
        records
            .filter { it.uri.trim().isNotBlank() }
            .sortedByDescending { it.playedAt }
            .distinctBy { it.uri.trim() }
            .take(MAX_AUDIO_RECENT_RECORDS)
            .forEach { record ->
                array.put(
                    JSONObject()
                        .put("uri", record.uri.trim())
                        .put("playedAt", record.playedAt)
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

    private fun decodeVideoFolderPlaybackSpeeds(json: String): Map<String, Float> {
        if (json.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(json)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val folderKey = keys.next().trim()
                    if (folderKey.isBlank()) continue
                    val speed = root.optDouble(folderKey, Double.NaN).toFloat()
                    put(
                        folderKey,
                        speed.takeIf(::isValidVideoFolderPlaybackSpeed)
                            ?: DEFAULT_VIDEO_PLAYBACK_SPEED
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeVideoFolderPlaybackSpeeds(speeds: Map<String, Float>): String {
        val root = JSONObject()
        speeds.forEach { (folderKey, speed) ->
            val normalizedKey = folderKey.trim()
            if (normalizedKey.isNotBlank() && isValidVideoFolderPlaybackSpeed(speed)) {
                root.put(normalizedKey, speed)
            }
        }
        return root.toString()
    }

    private fun isValidVideoFolderPlaybackSpeed(speed: Float): Boolean {
        return !speed.isNaN() &&
            speed >= MIN_VIDEO_PLAYBACK_SPEED &&
            speed <= MAX_VIDEO_PLAYBACK_SPEED
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

    private fun decodeVideoVisibilityRecords(
        json: String,
        idKey: String
    ): List<PersistedVideoVisibilityRecord> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val value = array.opt(index)
                if (value is JSONObject) {
                    val id = value.optString(idKey).ifBlank {
                        value.optString("uri").ifBlank { value.optString("folderId") }
                    }.trim()
                    PersistedVideoVisibilityRecord(
                        id = id,
                        name = value.optString("name").ifBlank { id },
                        uri = value.optString("uri").ifBlank { id },
                        path = value.optString("path").takeIf { it.isNotBlank() },
                        recordedAt = value.optLong(
                            when (idKey) {
                                "folderId" -> "hiddenAt"
                                else -> "recordedAt"
                            },
                            value.optLong("hiddenAt", System.currentTimeMillis())
                        )
                    )
                } else {
                    val id = value?.toString().orEmpty().trim()
                    PersistedVideoVisibilityRecord(
                        id = id,
                        name = id,
                        uri = id,
                        path = null,
                        recordedAt = 0L
                    )
                }
            }.filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun encodeVideoVisibilityRecords(
        records: List<PersistedVideoVisibilityRecord>,
        idKey: String
    ): String {
        val array = JSONArray()
        records.distinctBy { it.id.trim() }.forEach { record ->
            val normalizedId = record.id.trim()
            if (normalizedId.isBlank()) return@forEach
            array.put(
                JSONObject()
                    .put(idKey, normalizedId)
                    .put("name", record.name.ifBlank { normalizedId })
                    .put("uri", record.uri.ifBlank { normalizedId })
                    .put("path", record.path.orEmpty())
                    .put(
                        if (idKey == "folderId") "hiddenAt" else "recordedAt",
                        record.recordedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
            )
        }
        return array.toString()
    }

    private companion object {
        val FoldersKey = stringPreferencesKey("manual_folders_json")
        val BookFilesKey = stringPreferencesKey("book_files_json")
        val BookProgressKey = stringPreferencesKey("book_progress_json")
        val HiddenAudioUrisKey = stringPreferencesKey("hidden_audio_uris_json")
        val FavoriteAudioUrisKey = stringPreferencesKey("favorite_audio_uris_json")
        val RecentAudioRecordsKey = stringPreferencesKey("recent_audio_records_json")
        val HiddenVideoUrisKey = stringPreferencesKey("hidden_video_uris_json")
        val HiddenVideoFolderIdsKey = stringPreferencesKey("hidden_video_folder_ids_json")
        val UnavailableVideoRecordsKey = stringPreferencesKey("unavailable_video_records_json")
        val ProgressKey = stringPreferencesKey("playback_progress_json")
        val VideoFolderPlaybackSpeedKey = stringPreferencesKey("video_folder_playback_speed_json")
        val RecentVideoUriKey = stringPreferencesKey("recent_video_uri")
        val RecentAudioUriKey = stringPreferencesKey("recent_audio_uri")
        const val MIN_VIDEO_PLAYBACK_SPEED = 0.25f
        const val MAX_VIDEO_PLAYBACK_SPEED = 5f
        const val DEFAULT_VIDEO_PLAYBACK_SPEED = 1f
        const val MAX_AUDIO_RECENT_RECORDS = 100
    }
}

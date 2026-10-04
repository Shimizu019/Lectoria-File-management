package com.lectoria.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lectoria.data.model.LectureFile
import java.io.File

/** What happened when the user tapped a file. */
sealed interface FileOpenResult {
    /** The file was handed over to another app. */
    data object Opened : FileOpenResult

    /** Nothing on this device can handle that file type. */
    data object NoCompatibleApp : FileOpenResult

    /** The file is no longer in Lectoria's storage. */
    data object FileMissing : FileOpenResult
}

/** Message to show for a result, or null when there is nothing to report. */
val FileOpenResult.errorMessage: String?
    get() = when (this) {
        FileOpenResult.Opened -> null
        FileOpenResult.NoCompatibleApp -> "No compatible app found to open this file."
        FileOpenResult.FileMissing -> "That file is no longer on this device."
    }

/**
 * Opens an imported lecture file in whatever app the student already has
 * installed (PDF viewer, PowerPoint, Word, ...).
 *
 * Files live in app-private storage, so a `file://` Uri cannot be shared. A
 * FileProvider turns the tapped file into a temporary `content://` Uri, and the
 * intent grants read access to that one file only. Nothing is uploaded and no
 * internet access is involved.
 */
object FileOpener {

    private fun authority(context: Context): String =
        "${context.packageName}.fileprovider"

    /**
     * Turns a stored file into a shareable content uri.
     * @throws IllegalArgumentException if the file is outside the shared folder.
     */
    internal fun fileContentUri(context: Context, file: LectureFile): Uri =
        FileProvider.getUriForFile(
            context,
            authority(context),
            File(file.path)
        )

    /**
     * Builds the ACTION_VIEW intent for one file.
     *
     * [shareUri] exists so tests can exercise the intent logic without the
     * platform's FileProvider resource loading; production always uses the default.
     */
    fun buildViewIntent(
        context: Context,
        file: LectureFile,
        shareUri: (Context, LectureFile) -> Uri = ::fileContentUri
    ): Intent {
        val contentUri = shareUri(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, file.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Needed only if we ever run without an Activity in front of us.
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Tries to open [file] in another app. Never throws: a missing file or a
     * device with no suitable app is reported as a result instead.
     */
    fun open(
        context: Context,
        file: LectureFile,
        shareUri: (Context, LectureFile) -> Uri = ::fileContentUri
    ): FileOpenResult {
        if (!File(file.path).isFile) return FileOpenResult.FileMissing

        val intent = try {
            buildViewIntent(context, file, shareUri)
        } catch (e: IllegalArgumentException) {
            // File is not inside the folder the FileProvider shares.
            return FileOpenResult.FileMissing
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            return FileOpenResult.NoCompatibleApp
        }

        return try {
            context.startActivity(intent)
            FileOpenResult.Opened
        } catch (e: ActivityNotFoundException) {
            // resolveActivity can be out of date while the app is starting.
            FileOpenResult.NoCompatibleApp
        }
    }
}
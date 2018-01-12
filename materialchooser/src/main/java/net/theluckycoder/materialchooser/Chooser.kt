package net.theluckycoder.materialchooser

import android.app.Activity
import android.content.Intent
import android.os.Environment

class Chooser(private val activity: Activity,
              private val requestCode: Int,
              private var rootPath: String = Environment.getExternalStorageDirectory().absolutePath,
              private var startPath: String = rootPath,
              private var fileExtension: String = "",
              private var showHiddenFiles: Boolean = false,
              @ChooserType private var chooserType: Int = FILE_CHOOSER) {

    companion object Constants {
        internal const val CHOOSER_TYPE = "chooserType"
        internal const val ROOT_DIR_PATH = "rootDirPath"
        internal const val START_DIR_PATH = "startDirPath"
        internal const val FILE_EXTENSION = "fileExtension"
        internal const val SHOW_HIDDEN_FILES = "showHiddenFiles"

        const val RESULT_PATH = "resultPath"

        const val FILE_CHOOSER = 0
        const val FOLDER_CHOOSER = 1
    }

    /**
     * Select a file or a folder
     *
     * @param chooserType select between file chooser or directory chooser
     * *    Default: FILE_CHOOSER
     */
    fun setChooserType(@ChooserType chooserType: Int): Chooser {
        this.chooserType = chooserType
        return this
    }

    /**
     * Set the root directory of the picker
     *
     * @param rootPath the user can't go any higher than this
     * *      Default: External Storage
     */
    fun setRootPath(rootPath: String): Chooser {
        this.rootPath = rootPath
        return this
    }

    /**
     * Set the start directory of the picker
     *
     * @param startPath where the user starts
     * *    Default: Root Path
     */
    fun setStartPath(startPath: String): Chooser {
        this.startPath = startPath
        return this
    }

    /**
     * Filter files trough extensions
     *
     * @param extension file extension in string format
     * *    Example: "txt"
     */
    fun setFileExtension(extension: String): Chooser {
        this.fileExtension = extension
        return this
    }

    /**
     * Show or hide hidden files
     *
     * @param show show files and folders that begin with '.'
     * *    Default: false
     */
    fun setShowHiddenFiles(show: Boolean): Chooser {
        showHiddenFiles = show
        return this
    }

    /**
     * Start the chooser activity
     */
    fun start() {
        Intent(activity, ChooserActivity::class.java).apply {
            putExtra(ROOT_DIR_PATH, rootPath)
            putExtra(START_DIR_PATH, startPath)
            putExtra(FILE_EXTENSION, fileExtension)
            putExtra(SHOW_HIDDEN_FILES, showHiddenFiles)
            putExtra(CHOOSER_TYPE, chooserType)
        }.run {
            activity.startActivityForResult(this, requestCode)
        }
    }
}
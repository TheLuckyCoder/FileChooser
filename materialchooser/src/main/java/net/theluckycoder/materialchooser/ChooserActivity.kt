package net.theluckycoder.materialchooser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RestrictTo
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import java.io.File
import java.util.ArrayList

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ChooserActivity : AppCompatActivity() {

    private val mSwipeRefreshLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
    }
    private val mItemList = ArrayList<FileItem>()
    private val mAdapter by lazy(LazyThreadSafetyMode.NONE) {
        FilesAdapter(mItemList, { item ->
            if (item.isFolder) {
                mCurrentDir = File(item.path)
                updateAdapter()
            } else {
                finishWithResult(item.path)
            }
        })
    }
    private var mShowHiddenFiles = false
    private var mIsFileChooser = true
    private var mRootDirPath = Environment.getExternalStorageDirectory().absolutePath
    private var mCurrentDir = File(mRootDirPath)
    private var mFileExtension = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra(Chooser.USE_NIGHT_THEME, false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chooser)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = mAdapter

        mRootDirPath = intent.getStringExtra(Chooser.ROOT_DIR_PATH) ?: mRootDirPath
        val startPath = intent.getStringExtra(Chooser.START_DIR_PATH) ?: mRootDirPath
        if (startPath != mRootDirPath) mCurrentDir = File(startPath)
        mFileExtension = intent.getStringExtra(Chooser.FILE_EXTENSION) ?: ""
        mShowHiddenFiles = intent.getBooleanExtra(Chooser.SHOW_HIDDEN_FILES, false)
        mIsFileChooser = intent.getIntExtra(Chooser.CHOOSER_TYPE, 0) == 0

        if (!mIsFileChooser) {
            val selectFolderBtn: Button = findViewById(R.id.btn_select_folder)

            selectFolderBtn.visibility = View.VISIBLE
            selectFolderBtn.setOnClickListener {
                finishWithResult(mCurrentDir.absolutePath + "/")
            }
        }

        mCurrentDir.mkdirs()
        updateAdapter()

        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent)
        mSwipeRefreshLayout.setOnRefreshListener { updateAdapter() }

        checkForStoragePermission()
    }

    override fun onBackPressed() {
        if (mCurrentDir.absolutePath != mRootDirPath && mCurrentDir.parentFile != null) {
            mCurrentDir = mCurrentDir.parentFile
            updateAdapter()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chooser, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_chooser_refresh -> {
                mSwipeRefreshLayout.isRefreshing = true
                updateAdapter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == 100) {
            if (grantResults.size < 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e("Storage Permission", "Permission not granted")
                finish()
            } else {
                mCurrentDir.mkdirs()
                updateAdapter()
            }
        }
    }

    private fun updateAdapter() {
        mItemList.clear()
        mItemList.addAll(getListedFiles())

        if (mCurrentDir.absolutePath != mRootDirPath) {
            val parentFolder = mCurrentDir.parent ?: mRootDirPath
            mItemList.add(0, FileItem(getString(R.string.chooser_parent_directory),
                parentFolder, true, true))
        }

        mAdapter.notifyDataSetChanged()
        mSwipeRefreshLayout.isRefreshing = false
    }

    private fun getListedFiles(): List<FileItem> {
        val listedFilesArray: Array<File>? = mCurrentDir.listFiles()
        title = mCurrentDir.absolutePath.replace(Environment.getExternalStorageDirectory().absolutePath,
            getString(R.string.chooser_device))

        val dirsList = ArrayList<FileItem>()
        val filesList = ArrayList<FileItem>()

        if (listedFilesArray == null || listedFilesArray.isEmpty()) return dirsList

        listedFilesArray.forEach {
            if (!it.canRead() || (!mShowHiddenFiles && it.name.startsWith("."))) return@forEach

            when {
                it.isDirectory -> dirsList.add(FileItem(it.name, it.absolutePath, true))
                mFileExtension.isEmpty() -> filesList.add(FileItem(it.name, it.absolutePath, false))
                it.extension == mFileExtension -> filesList.add(FileItem(it.name, it.absolutePath, false))
            }
        }


        if (mIsFileChooser) dirsList.addAll(filesList)
        dirsList.sort()

        return dirsList
    }

    private fun finishWithResult(path: String) {
        val intent = Intent().apply {
            putExtra(Chooser.RESULT_PATH, path)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun checkForStoragePermission() {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            Toast.makeText(this, R.string.chooser_permission_required_desc, Toast.LENGTH_LONG).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.chooser_permission_required)
                .setMessage(R.string.chooser_permission_required_desc)
                .setCancelable(false)
                .setIcon(R.drawable.ic_folder)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
                }.show()
        }
    }
}

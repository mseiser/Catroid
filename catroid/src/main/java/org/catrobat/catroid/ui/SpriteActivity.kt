/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.VisualPlacementBrick
import org.catrobat.catroid.formulaeditor.UserData
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.pocketmusic.PocketMusicActivity
import org.catrobat.catroid.soundrecorder.SoundRecorderActivity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.TestResult
import org.catrobat.catroid.ui.controller.RecentBrickListManager
import org.catrobat.catroid.ui.fragment.AddBrickFragment
import org.catrobat.catroid.ui.fragment.BrickCategoryFragment
import org.catrobat.catroid.ui.fragment.FormulaEditorFragment
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog
import org.catrobat.catroid.ui.recyclerview.dialog.dialoginterface.NewItemInterface
import org.catrobat.catroid.ui.recyclerview.dialog.textwatcher.DuplicateInputTextWatcher
import org.catrobat.catroid.ui.recyclerview.fragment.ListSelectorFragment
import org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.SnackbarUtil
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.visualplacement.VisualPlacementActivity
import java.io.File
import java.io.IOException

class SpriteActivity : BaseActivity() {
    private lateinit var onNewSpriteListener: NewItemInterface<Sprite>
    private lateinit var onNewLookListener: NewItemInterface<LookData>
    private lateinit var onNewSoundListener: NewItemInterface<SoundInfo>
    private lateinit var projectManager: ProjectManager
    private var currentLookData: LookData? = null
    private var isUndoMenuItemVisible = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) {
            return
        }
        projectManager = ProjectManager.getInstance()
        setContentView(R.layout.activity_sprite)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = createActionBarTitle()
        if (RecentBrickListManager.getInstance().getRecentBricks(true).size == 0) {
            RecentBrickListManager.getInstance().loadRecentBricks()
        }
        var fragmentPosition = FRAGMENT_SCRIPTS
        val bundle = intent.extras
        if (bundle != null) {
            fragmentPosition = bundle.getInt(EXTRA_FRAGMENT_POSITION, FRAGMENT_SCRIPTS)
        }
        loadFragment(fragmentPosition)
        this.addTabLayout(fragmentPosition)
    }

    fun createActionBarTitle(): String {
        return if (projectManager.currentProject != null &&
            projectManager.currentProject?.sceneList?.size == 1) {
            projectManager.currentSprite.name
        } else {
            projectManager.currentlyEditedScene.name + ": " + projectManager.currentSprite?.name
        }
    }

    val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_script_activity, menu)
        optionsMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    fun showUndo(visible: Boolean) {
        optionsMenu.findItem(R.id.menu_undo).isVisible = visible
        if (visible) {
            ProjectManager.getInstance().changedProject(projectManager.currentProject.name)
        }
    }

    fun checkForChange() {
        if (optionsMenu.findItem(R.id.menu_undo).isVisible) {
            ProjectManager.getInstance().changedProject(projectManager.currentProject.name)
        } else {
            ProjectManager.getInstance().resetChangedFlag(projectManager.currentProject)
        }
    }

    fun setUndoMenuItemVisibility(isVisible: Boolean) {
        isUndoMenuItemVisible = isVisible
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (currentFragment is ScriptFragment) {
            menu.findItem(R.id.comment_in_out).isVisible = true
            showUndo(isUndoMenuItemVisible)
        } else if (currentFragment is LookListFragment) {
            showUndo(isUndoMenuItemVisible)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val isDragAndDropActiveInFragment = (currentFragment is ScriptFragment
            && (currentFragment as ScriptFragment).isCurrentlyMoving)
        if (item.itemId == android.R.id.home && isDragAndDropActiveInFragment) {
            (currentFragment as ScriptFragment).highlightMovingItem()
            return true
        }
        if (item.itemId == R.id.menu_undo && currentFragment is LookListFragment) {
            setUndoMenuItemVisibility(false)
            showUndo(isUndoMenuItemVisible)
            val fragment = currentFragment
            if (fragment is LookListFragment && !fragment.undo() && currentLookData != null) {
                fragment.deleteItem(currentLookData)
                currentLookData!!.dispose()
                currentLookData = null
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        saveProject()
        RecentBrickListManager.getInstance().saveRecentBrickList()
    }

    override fun onBackPressed() {
        saveProject()
        val currentFragment = currentFragment
        if (currentFragment is ScriptFragment) {
            if (currentFragment.isCurrentlyMoving) {
                currentFragment.cancelMove()
                return
            }
            if (currentFragment.isFinderOpen) {
                currentFragment.closeFinder()
                return
            }
            if (currentFragment.isCurrentlyHighlighted) {
                currentFragment.cancelHighlighting()
                return
            }
        } else if (currentFragment is FormulaEditorFragment) {
            currentFragment.exitFormulaEditorFragment()
            return
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            if (currentFragment is BrickCategoryFragment) {
                SnackbarUtil.showHintSnackbar(this, R.string.hint_scripts)
            } else if (currentFragment is AddBrickFragment) {
                SnackbarUtil.showHintSnackbar(this, R.string.hint_category)
            }
            supportFragmentManager.popBackStack()
            return
        }
        super.onBackPressed()
    }

    private fun saveProject() {
        ProjectSaver(ProjectManager.getInstance().currentProject, applicationContext).saveProjectAsync()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == TestResult.STAGE_ACTIVITY_TEST_SUCCESS
            || resultCode == TestResult.STAGE_ACTIVITY_TEST_FAIL
        ) {
            val message = data!!.getStringExtra(TestResult.TEST_RESULT_MESSAGE)
            ToastUtil.showError(this, message)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val testResult = ClipData.newPlainText(
                "TestResult",
                """ ${ProjectManager.getInstance().currentProject.name}$message""".trimIndent()
            )
            clipboard.setPrimaryClip(testResult)
        }
        if (resultCode != RESULT_OK) {
            if (SettingsFragment.isCastSharedPreferenceEnabled(this)
                && projectManager.currentProject.isCastProject
                && !CastManager.getInstance().isConnected
            ) {
                CastManager.getInstance().openDeviceSelectorOrDisconnectDialog(this)
            }
            return
        }
        val uri: Uri?
        when (requestCode) {
            SPRITE_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addSpriteFromUri(uri)
            }
            SPRITE_LIBRARY -> {
                val fileData = data?.getStringExtra(WebViewActivity.MEDIA_FILE_PATH) ?: return
                uri = Uri.fromFile(File(fileData))
                addSpriteFromUri(uri)
            }
            SPRITE_FILE -> {
                uri = data?.data ?: return
                addSpriteFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            SPRITE_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addSpriteFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            BACKGROUND_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addBackgroundFromUri(uri)
            }
            BACKGROUND_LIBRARY -> {
                val fileData = data?.getStringExtra(WebViewActivity.MEDIA_FILE_PATH) ?: return
                uri = Uri.fromFile(File(fileData))
                addBackgroundFromUri(uri)
            }
            BACKGROUND_FILE -> {
                uri = data!!.data
                addBackgroundFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            BACKGROUND_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addBackgroundFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            LOOK_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addLookFromUri(uri)
                setUndoMenuItemVisibility(true)
            }
            LOOK_LIBRARY -> {
                val fileData = data?.getStringExtra(WebViewActivity.MEDIA_FILE_PATH) ?: return
                uri = Uri.fromFile(File(fileData))
                addLookFromUri(uri)
                setUndoMenuItemVisibility(true)
            }
            LOOK_FILE -> {
                uri = data!!.data
                addLookFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
                setUndoMenuItemVisibility(true)
            }
            LOOK_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addLookFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
                setUndoMenuItemVisibility(true)
            }
            SOUND_RECORD, SOUND_FILE -> {
                uri = data?.data ?: return
                addSoundFromUri(uri)
            }
            SOUND_LIBRARY -> {
                val fileData = data?.getStringExtra(WebViewActivity.MEDIA_FILE_PATH) ?: return
                uri = Uri.fromFile(File(fileData))
                addSoundFromUri(uri)
            }
            REQUEST_CODE_VISUAL_PLACEMENT -> {
                val extras = data?.extras ?: return
                val xCoordinate =
                    extras.getInt(VisualPlacementActivity.X_COORDINATE_BUNDLE_ARGUMENT)
                val yCoordinate =
                    extras.getInt(VisualPlacementActivity.Y_COORDINATE_BUNDLE_ARGUMENT)
                val brickHash = extras.getInt(EXTRA_BRICK_HASH)
                val fragment = currentFragment
                var brick: Brick? = null
                if (fragment is ScriptFragment) {
                    brick = fragment.findBrickByHash(brickHash)
                } else if (fragment is FormulaEditorFragment) {
                    brick = fragment.formulaBrick
                }
                if (brick != null) {
                    (brick as VisualPlacementBrick).setCoordinates(xCoordinate, yCoordinate)
                    if (fragment is FormulaEditorFragment) {
                        fragment.updateFragmentAfterVisualPlacement()
                    }
                }
                setUndoMenuItemVisibility(extras.getBoolean(VisualPlacementActivity.CHANGED_COORDINATES))
            }
        }
    }

    fun registerOnNewSpriteListener(listener: NewItemInterface<Sprite>) {
        onNewSpriteListener = listener
    }

    fun registerOnNewLookListener(listener: NewItemInterface<LookData>) {
        onNewLookListener = listener
    }

    fun registerOnNewSoundListener(listener: NewItemInterface<SoundInfo>) {
        onNewSoundListener = listener
    }

    private fun addSpriteFromUri(
        uri: Uri?,
        imageExtension: String = Constants.DEFAULT_IMAGE_EXTENSION
    ) {
        val resolvedName: String
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val lookFileName: String?
        val useDefaultSpriteName = (resolvedFileName == null
            || StorageOperations.getSanitizedFileName(resolvedFileName) == Constants.TMP_IMAGE_FILE_NAME)
        if (useDefaultSpriteName) {
            resolvedName = getString(R.string.default_sprite_name)
            lookFileName = resolvedName + imageExtension
        } else {
            resolvedName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }
        val lookDataName: String =
            UniqueNameProvider().getUniqueNameInNameables(resolvedName, projectManager.currentlyEditedScene.spriteList)
        val builder = TextInputDialog.Builder(this)
        builder.setHint(getString(R.string.sprite_name_label))
            .setText(lookDataName)
            .setTextWatcher(DuplicateInputTextWatcher(projectManager.currentlyEditedScene.spriteList))
            .setPositiveButton(
                getString(R.string.ok)
            ) { _: DialogInterface?, textInput: String? ->
                val sprite = Sprite(textInput)
                projectManager.currentlyEditedScene.addSprite(sprite)
                try {
                    val imageDirectory = File(
                        projectManager.currentlyEditedScene.directory,
                        Constants.IMAGE_DIRECTORY_NAME
                    )
                    val file = StorageOperations
                        .copyUriToDir(
                            contentResolver,
                            uri,
                            imageDirectory,
                            lookFileName
                        )
                    Utils.removeExifData(imageDirectory, lookFileName)
                    val lookData = LookData(textInput, file)
                    sprite.lookList.add(lookData)
                    lookData.collisionInformation.calculate()
                } catch (e: IOException) {
                    Log.e(TAG, Log.getStackTraceString(e))
                }
                onNewSpriteListener.addItem(sprite)
                val currentFragment = currentFragment
                if (currentFragment is ScriptFragment) {
                    currentFragment.notifyDataSetChanged()
                }
            }
        builder.setTitle(R.string.new_sprite_dialog_title)
            .setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
                try {
                    if (Constants.MEDIA_LIBRARY_CACHE_DIRECTORY.exists()) {
                        StorageOperations.deleteDir(Constants.MEDIA_LIBRARY_CACHE_DIRECTORY)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, Log.getStackTraceString(e))
                }
            }
            .show()
    }

    private fun addBackgroundFromUri(
        uri: Uri?,
        imageExtension: String = Constants.DEFAULT_IMAGE_EXTENSION
    ) {
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        var lookDataName: String
        val lookFileName: String?
        val useSpriteName = (resolvedFileName == null
            || StorageOperations.getSanitizedFileName(resolvedFileName) == Constants.TMP_IMAGE_FILE_NAME)
        if (useSpriteName) {
            lookDataName = projectManager.currentSprite.name
            lookFileName = lookDataName + imageExtension
        } else {
            lookDataName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }
        lookDataName =
            UniqueNameProvider().getUniqueNameInNameables(lookDataName, projectManager.currentSprite.lookList)
        try {
            val imageDirectory = File(projectManager.currentlyEditedScene.directory, Constants.IMAGE_DIRECTORY_NAME)
            val file = StorageOperations.copyUriToDir(
                contentResolver, uri, imageDirectory,
                lookFileName
            )
            Utils.removeExifData(imageDirectory, lookFileName)
            val look = LookData(lookDataName, file)
            projectManager.currentSprite.lookList.add(look)
            look.collisionInformation.calculate()
            onNewLookListener.addItem(look)
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
    }

    private fun addLookFromUri(
        uri: Uri?,
        imageExtension: String = Constants.DEFAULT_IMAGE_EXTENSION
    ) {
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        var lookDataName: String
        val lookFileName: String?
        val useSpriteName = (resolvedFileName == null
            || StorageOperations.getSanitizedFileName(resolvedFileName) == Constants.TMP_IMAGE_FILE_NAME)
        if (useSpriteName) {
            lookDataName = projectManager.currentSprite.name
            lookFileName = lookDataName + imageExtension
        } else {
            lookDataName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }
        lookDataName =
            UniqueNameProvider().getUniqueNameInNameables(lookDataName, projectManager.currentSprite.lookList)
        try {
            val imageDirectory = File(projectManager.currentlyEditedScene.directory, Constants.IMAGE_DIRECTORY_NAME)
            val file =
                StorageOperations.copyUriToDir(contentResolver, uri, imageDirectory, lookFileName)
            Utils.removeExifData(imageDirectory, lookFileName)
            val look = LookData(lookDataName, file)
            currentLookData = look
            projectManager.currentSprite.lookList.add(look)
            look.collisionInformation.calculate()
            onNewLookListener.addItem(look)
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
    }

    private fun addSoundFromUri(uri: Uri?) {
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        var soundInfoName: String
        val soundFileName: String?
        val useSpriteName = resolvedFileName == null
        if (useSpriteName) {
            soundInfoName = projectManager.currentSprite.name
            soundFileName = soundInfoName + Constants.DEFAULT_SOUND_EXTENSION
        } else {
            soundInfoName = StorageOperations.getSanitizedFileName(resolvedFileName)
            soundFileName = resolvedFileName
        }
        soundInfoName =
            UniqueNameProvider().getUniqueNameInNameables(soundInfoName, projectManager.currentSprite.soundList)
        try {
            val soundDirectory = File(projectManager.currentlyEditedScene.directory, Constants
                .SOUND_DIRECTORY_NAME)
            val file =
                StorageOperations.copyUriToDir(contentResolver, uri, soundDirectory, soundFileName)
            val sound = SoundInfo(soundInfoName, file)
            projectManager.currentSprite.soundList.add(sound)
            onNewSoundListener.addItem(sound)
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
    }

    fun handleAddSpriteButton() {
        val root = View.inflate(this, R.layout.dialog_new_look, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_sprite_dialog_title)
            .setView(root)
            .create()
        root.findViewById<View>(R.id.dialog_new_look_paintroid).setOnClickListener {
            ImportFromPocketPaintLauncher(this)
                .startActivityForResult(SPRITE_POCKET_PAINT)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_media_library)
            .setOnClickListener {
                ImportFormMediaLibraryLauncher(this, FlavoredConstants.LIBRARY_LOOKS_URL)
                    .startActivityForResult(SPRITE_LIBRARY)
                alertDialog.dismiss()
            }
        root.findViewById<View>(R.id.dialog_new_look_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "image/*", getString(R.string.select_look_from_gallery))
                .startActivityForResult(SPRITE_FILE)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_camera).setOnClickListener {
            ImportFromCameraLauncher(this)
                .startActivityForResult(SPRITE_CAMERA)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    fun handleAddBackgroundButton() {
        val root = View.inflate(this, R.layout.dialog_new_look, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_look_dialog_title)
            .setView(root)
            .create()
        val mediaLibraryUrl: String = if (projectManager.isCurrentProjectLandscapeMode) {
            FlavoredConstants.LIBRARY_BACKGROUNDS_URL_LANDSCAPE
        } else {
            FlavoredConstants.LIBRARY_BACKGROUNDS_URL_PORTRAIT
        }
        root.findViewById<View>(R.id.dialog_new_look_paintroid).setOnClickListener {
            ImportFromPocketPaintLauncher(this)
                .startActivityForResult(BACKGROUND_POCKET_PAINT)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_media_library)
            .setOnClickListener {
                ImportFormMediaLibraryLauncher(this, mediaLibraryUrl)
                    .startActivityForResult(BACKGROUND_LIBRARY)
                alertDialog.dismiss()
            }
        root.findViewById<View>(R.id.dialog_new_look_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "image/*", getString(R.string.select_look_from_gallery))
                .startActivityForResult(BACKGROUND_FILE)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_camera).setOnClickListener {
            ImportFromCameraLauncher(this)
                .startActivityForResult(BACKGROUND_CAMERA)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    fun handleAddLookButton() {
        val root = View.inflate(this, R.layout.dialog_new_look, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_look_dialog_title)
            .setView(root)
            .create()
        val mediaLibraryUrl: String = if (projectManager.currentSprite == projectManager.currentlyEditedScene.backgroundSprite) {
            if (projectManager.isCurrentProjectLandscapeMode) {
                FlavoredConstants.LIBRARY_BACKGROUNDS_URL_LANDSCAPE
            } else {
                FlavoredConstants.LIBRARY_BACKGROUNDS_URL_PORTRAIT
            }
        } else {
            FlavoredConstants.LIBRARY_LOOKS_URL
        }
        root.findViewById<View>(R.id.dialog_new_look_paintroid).setOnClickListener {
            ImportFromPocketPaintLauncher(this)
                .startActivityForResult(LOOK_POCKET_PAINT)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_media_library)
            .setOnClickListener {
                ImportFormMediaLibraryLauncher(this, mediaLibraryUrl)
                    .startActivityForResult(LOOK_LIBRARY)
                alertDialog.dismiss()
            }
        root.findViewById<View>(R.id.dialog_new_look_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "image/*", getString(R.string.select_look_from_gallery))
                .startActivityForResult(LOOK_FILE)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_camera).setOnClickListener {
            ImportFromCameraLauncher(this)
                .startActivityForResult(LOOK_CAMERA)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    fun handleAddSoundButton() {
        val root = View.inflate(this, R.layout.dialog_new_sound, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_sound_dialog_title)
            .setView(root)
            .create()
        root.findViewById<View>(R.id.dialog_new_sound_recorder).setOnClickListener {
            startActivityForResult(Intent(this, SoundRecorderActivity::class.java), SOUND_RECORD)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_sound_media_library)
            .setOnClickListener {
                ImportFormMediaLibraryLauncher(this, FlavoredConstants.LIBRARY_SOUNDS_URL)
                    .startActivityForResult(SOUND_LIBRARY)
                alertDialog.dismiss()
            }
        root.findViewById<View>(R.id.dialog_new_sound_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "audio/*", getString(R.string.sound_select_source))
                .startActivityForResult(SOUND_FILE)
            alertDialog.dismiss()
        }
        if (BuildConfig.FEATURE_POCKETMUSIC_ENABLED) {
            root.findViewById<View>(R.id.dialog_new_sound_pocketmusic).visibility = View.VISIBLE
            root.findViewById<View>(R.id.dialog_new_sound_pocketmusic)
                .setOnClickListener {
                    startActivity(Intent(this, PocketMusicActivity::class.java))
                    alertDialog.dismiss()
                }
        }
        alertDialog.show()
    }

    fun handleAddUserListButton() {
        val view = View.inflate(this, R.layout.dialog_new_user_data, null)
        val addToProjectUserDataRadioButton = view.findViewById<RadioButton>(R.id.global)
        val lists: MutableList<UserData<*>> = ArrayList()
        lists.addAll(projectManager.currentProject.userLists)
        lists.addAll(projectManager.currentSprite.userLists)
        val textWatcher = DuplicateInputTextWatcher(lists)
        val builder = TextInputDialog.Builder(this)
            .setTextWatcher(textWatcher)
            .setPositiveButton(
                getString(R.string.ok)
            ) { _: DialogInterface?, textInput: String? ->
                val addToProjectUserData = addToProjectUserDataRadioButton.isChecked
                val userList = UserList(textInput)
                if (addToProjectUserData) {
                    projectManager.currentProject.addUserList(userList)
                } else {
                    projectManager.currentSprite.addUserList(userList)
                }
                if (currentFragment is ListSelectorFragment) {
                    (currentFragment as ListSelectorFragment).notifyDataSetChanged()
                }
            }
        val alertDialog = builder.setTitle(R.string.formula_editor_list_dialog_title)
            .setView(view)
            .create()
        alertDialog.show()
    }

    fun handlePlayButton() {
        val currentFragment = currentFragment
        if (currentFragment is ScriptFragment) {
            if (currentFragment.isCurrentlyHighlighted) {
                currentFragment.cancelHighlighting()
            }
            if (currentFragment.isCurrentlyMoving) {
                currentFragment.highlightMovingItem()
                return
            }
        }
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }
        StageActivity.handlePlayButton(projectManager, this)
    }

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        val fragment = currentFragment
        if (fragment.isFragmentWithTablayout()) {
           removeTabs()
        }
        return super.startActionMode(callback)
    }

    override fun onActionModeFinished(mode: ActionMode) {
        val fragment = currentFragment
        if (fragment.isFragmentWithTablayout() && (fragment !is ScriptFragment || !fragment.isFinderOpen)) {
           addTabs()
        }
        super.onActionModeFinished(mode)
    }

    fun setCurrentSprite(sprite: Sprite) {
        projectManager.currentSprite = sprite
    }

    fun setCurrentSceneAndSprite(scene: Scene, sprite: Sprite) {
        projectManager.currentlyEditedScene = scene
        projectManager.currentSprite = sprite
    }

    fun removeTabs() {
        this.removeTabLayout()
    }

    fun addTabs() {
        this.addTabLayout(currentFragment.getTabPositionInSpriteActivity())
    }

    companion object {
        val TAG: String = SpriteActivity::class.java.simpleName
        const val FRAGMENT_SCRIPTS = 0
        const val FRAGMENT_LOOKS = 1
        const val FRAGMENT_SOUNDS = 2
        const val SPRITE_POCKET_PAINT = 0
        const val SPRITE_LIBRARY = 1
        const val SPRITE_FILE = 2
        const val SPRITE_CAMERA = 3
        const val BACKGROUND_POCKET_PAINT = 4
        const val BACKGROUND_LIBRARY = 5
        const val BACKGROUND_FILE = 6
        const val BACKGROUND_CAMERA = 7
        const val LOOK_POCKET_PAINT = 8
        const val LOOK_LIBRARY = 9
        const val LOOK_FILE = 10
        const val LOOK_CAMERA = 11
        const val SOUND_RECORD = 12
        const val SOUND_LIBRARY = 13
        const val SOUND_FILE = 14
        const val REQUEST_CODE_VISUAL_PLACEMENT = 2019
        const val EDIT_LOOK = 2020
        const val EXTRA_FRAGMENT_POSITION = "fragmentPosition"
        const val EXTRA_BRICK_HASH = "BRICK_HASH"
        const val EXTRA_X_TRANSFORM = "X"
        const val EXTRA_Y_TRANSFORM = "Y"
        const val EXTRA_TEXT = "TEXT"
        const val EXTRA_TEXT_COLOR = "TEXT_COLOR"
        const val EXTRA_TEXT_SIZE = "TEXT_SIZE"
        const val EXTRA_TEXT_ALIGNMENT = "TEXT_ALIGNMENT"
    }
}
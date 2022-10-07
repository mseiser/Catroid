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

package org.catrobat.catroid.merge

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.databinding.ActivityRecyclerBinding
import org.catrobat.catroid.ui.BaseActivity
import org.catrobat.catroid.ui.BottomBar
import org.catrobat.catroid.ui.recyclerview.fragment.ProjectListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.RecyclerViewFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SceneListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SpriteListFragment

class SelectLocalImportActivity : BaseActivity() {
    private lateinit var binding: ActivityRecyclerBinding
    private lateinit var listFragment: RecyclerViewFragment<*>
    val requested: ImportType
        get() {
           return intent.getSerializableExtra(Constants.EXTRA_IMPORT_REQUEST_KEY) as ImportType
        }
    val currentFragmentType: ImportType
        get() {
            return intent.getSerializableExtra(Constants.EXTRA_FRAGMENT_TYPE_KEY) as ImportType
        }

    enum class ImportType {
        PROJECT, SCENE, SPRITE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclerBinding.inflate(layoutInflater)
        if (isFinishing) {
            return
        }
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar.toolbar)
        BottomBar.hideBottomBar(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        loadSelector(currentFragmentType)
    }

    override fun onResume() {
        super.onResume()
        BottomBar.hideBottomBar(this)
        loadSelector(currentFragmentType)
    }

    fun loadSelector(type: ImportType?) {
        listFragment = when (type) {
            ImportType.PROJECT -> ProjectListFragment()
            ImportType.SCENE -> SceneListFragment()
            ImportType.SPRITE -> SpriteListFragment()
            else -> throw IllegalStateException(TAG + R.string.reject_import)
        }
        loadFragment(listFragment.javaClass.simpleName)
    }

    private fun loadFragment(tag: String) {
        intent?.apply {
            if (action != null) {
                val data = Bundle()
                data.putParcelable("intent", intent)
                listFragment.arguments = data
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, listFragment, tag)
            .commit()
    }

    override fun onBackPressed() {
        when (currentFragmentType) {
            ImportType.SPRITE ->
                if (sourceProject?.hasMultipleScenes() == true) {
                    loadSelector(ImportType.SCENE)
                } else {
                    loadSelector(ImportType.PROJECT)
                }
            ImportType.SCENE -> loadSelector(ImportType.PROJECT)
            ImportType.PROJECT -> finish()
        }
    }

    fun loadNext(type: ImportType) {
        when (type) {
            ImportType.PROJECT ->
                if (sourceProject?.hasMultipleScenes() == true) {
                    loadSelector(ImportType.SCENE)
                } else {
                    sourceScene = sourceProject?.defaultScene
                    when (requested) {
                        ImportType.SCENE -> finish()
                        ImportType.SPRITE -> loadSelector(ImportType.SPRITE)
                        else -> throw IllegalStateException(TAG + R.string.reject_import)
                    }
                }
            ImportType.SCENE ->
                when (requested) {
                    ImportType.SCENE -> finish()
                    ImportType.SPRITE -> loadSelector(ImportType.SPRITE)
                    else -> throw IllegalStateException(TAG + R.string.reject_import)
                }
        }
    }

    override fun finish() {
        val intent = Intent()
        if (requested == ImportType.SCENE) {
            sourceSprites = sourceScene?.spriteList?.map { it.name } as ArrayList<String>
        }

        if (sourceProject != null && sourceScene != null && sourceSprites != null) {
            intent.putExtra(Constants.EXTRA_PROJECT_PATH, sourceProject?.directory?.absoluteFile)
            intent.putExtra(Constants.EXTRA_SCENE_NAME, sourceScene?.name)
            intent.putExtra(Constants.EXTRA_SPRITE_NAMES, sourceSprites)
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_CANCELED)
        }
        super.finish()
    }

    companion object {
        var sourceProject: Project? = null
        var sourceScene: Scene? = null
        var sourceSprites: ArrayList<String>? = null
        val TAG: String = SelectLocalImportActivity::class.java.simpleName
    }
}

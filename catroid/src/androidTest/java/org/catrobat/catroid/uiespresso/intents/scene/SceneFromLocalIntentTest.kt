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

package org.catrobat.catroid.uiespresso.intents.scene

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.DefaultProjectHandler
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.merge.SelectLocalImportActivity
import org.catrobat.catroid.test.merge.MergeTestUtils
import org.catrobat.catroid.testsuites.annotations.Cat
import org.catrobat.catroid.testsuites.annotations.Level
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.uiespresso.util.matchers.IndexMatchers
import org.catrobat.catroid.uiespresso.util.rules.FragmentActivityTestRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.koin.java.KoinJavaComponent
import java.io.File

class SceneFromLocalIntentTest {
    private lateinit var project: Project
    private lateinit var localProject: Project
    private var expectedIntent: Matcher<Intent>? = null
    private var projectManager = KoinJavaComponent.inject(ProjectManager::class.java).value

    private val projectName = javaClass.simpleName
    private val tmpPath = File(
        Constants.CACHE_DIRECTORY.absolutePath, "Pocket Code Test Temp"
    )

    @get:Rule
    var baseActivityTestRule = FragmentActivityTestRule(
        ProjectActivity::class.java,
        ProjectActivity.EXTRA_FRAGMENT_POSITION,
        ProjectActivity.FRAGMENT_SCENES
    )

    @Before
    fun setUp() {
        createProjects(projectName)
        baseActivityTestRule.launchActivity()
        Intents.init()

        expectedIntent = allOf(
            IntentMatchers.hasExtra(
                Constants.EXTRA_IMPORT_REQUEST_KEY,
                SelectLocalImportActivity.ImportType.SCENE
            ),
            IntentMatchers.hasExtra(
                Constants.EXTRA_FRAGMENT_TYPE_KEY,
                SelectLocalImportActivity.ImportType.PROJECT
            )
        )

        if (!tmpPath.exists()) {
            tmpPath.mkdirs()
        }

        val resultData = Intent()
        resultData.putExtra(
            Constants.EXTRA_PROJECT_PATH,
            localProject.directory.absoluteFile
        )
        resultData.putExtra(Constants.EXTRA_SCENE_NAME, localProject.defaultScene.name)
        resultData.putExtra(
            Constants.EXTRA_SPRITE_NAMES,
            localProject.defaultScene.spriteList.map {it.name} as ArrayList<String>
        )

        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        Intents.intending(expectedIntent).respondWith(result)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        Intents.release()
        baseActivityTestRule.finishActivity()
        StorageOperations.deleteDir(tmpPath)
        StorageOperations.deleteDir(File(FlavoredConstants.DEFAULT_ROOT_DIRECTORY, projectName))
    }

    @Category(Cat.AppUi::class, Level.Smoke::class)
    @Test
    fun testMergeSceneWithEmptyScene() {
        val originalMerges = ArrayList<Sprite>()
        val importSprites = ArrayList<Sprite>()
        localProject.defaultScene.spriteList.forEach {
            if (it.name == "Background") {
                originalMerges.add(project.sceneList[1].getSprite(it.name))
            } else {
                importSprites.add(it)
            }
        }
        onView(IndexMatchers().withIndex(withId(R.id.settings_button), 1)).perform(click())
        onView(withText(R.string.from_local)).perform(click())
        Intents.intended(expectedIntent)
        MergeTestUtils().assertSceneMerge(importSprites, originalMerges, project, project
            .sceneList[1], localProject)
    }

    @Category(Cat.AppUi::class, Level.Smoke::class)
    @Test
    fun testMergeScene() {
        val originalMerges = ArrayList<Sprite>()
        val importSprites = ArrayList<Sprite>()
        localProject.defaultScene.spriteList.forEach {
            val sprite = project.sceneList[2].getSprite(it.name)
            if (sprite != null) {
                originalMerges.add(sprite)
            } else {
                importSprites.add(it)
            }
        }
        onView(IndexMatchers().withIndex(withId(R.id.settings_button), 2)).perform(click())
        onView(withText(R.string.from_local)).perform(click())
        Intents.intended(expectedIntent)
        MergeTestUtils().assertSceneMerge(importSprites, originalMerges, project, project
            .sceneList[2], localProject)
    }

    @Category(Cat.AppUi::class, Level.Smoke::class)
    @Test
    fun testMergeSceneWithEqualScene() {
        val originalMerges = ArrayList<Sprite>()
        val importSprites = ArrayList<Sprite>()
        localProject.defaultScene.spriteList.forEach {
            val sprite = project.sceneList[3].getSprite(it.name)
            if (sprite != null) {
                originalMerges.add(sprite)
            } else {
                importSprites.add(it)
            }
        }
        onView(IndexMatchers().withIndex(withId(R.id.settings_button), 3)).perform(click())
        onView(withText(R.string.from_local)).perform(click())
        Intents.intended(expectedIntent)
        MergeTestUtils().assertSceneMerge(importSprites, originalMerges, project, project.sceneList[3],
            localProject)
    }

    private fun createProjects(projectName: String) {
        project = Project(ApplicationProvider.getApplicationContext(), projectName)
        val emptyScene = Scene("emptyScene", project)
        project.addScene(emptyScene)
        val sceneWithSprites = Scene("sceneWithSprites", project)
        val sprite1 = Sprite("sprite1")
        sprite1.addScript(StartScript())
        sprite1.userVariables.add(UserVariable("var1", 1))
        val sprite2 = Sprite("Clouds1")
        sceneWithSprites.addSprite(sprite1)
        sceneWithSprites.addSprite(sprite2)
        project.addScene(sceneWithSprites)
        projectManager.currentProject = project
        projectManager.currentlyEditedScene = project.defaultScene
        localProject = DefaultProjectHandler.createAndSaveDefaultProject(
            "local",
            ApplicationProvider.getApplicationContext(),
            false
        )

        val scene = localProject.defaultScene
        scene.name = "defaultScene"
        project.addScene(scene)
        XstreamSerializer.getInstance().saveProject(localProject)
        XstreamSerializer.getInstance().saveProject(project)
    }
}

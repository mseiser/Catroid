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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.DefaultProjectHandler
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.merge.SelectLocalImportActivity
import org.catrobat.catroid.testsuites.annotations.Cat
import org.catrobat.catroid.testsuites.annotations.Level
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.uiespresso.util.rules.FragmentActivityTestRule
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf
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
            ProjectActivity.FRAGMENT_SPRITES
        )

        @Before
        fun setUp() {
            createProjects(projectName)
            baseActivityTestRule.launchActivity()
            Intents.init()

            expectedIntent = AllOf.allOf(
                IntentMatchers.hasExtra(
                    Constants.EXTRA_IMPORT_REQUEST_KEY,
                    SelectLocalImportActivity.ImportType.SPRITE
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
            resultData.putExtra(Constants.EXTRA_SCENE_NAMES, localProject.defaultScene.name)
            resultData.putExtra(
                Constants.EXTRA_SPRITE_NAMES,
                arrayListOf(
                    localProject.defaultScene.backgroundSprite.name,
                    localProject.defaultScene.spriteList[1].name
                )
            )

            val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, resultData)
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

        }

        @Category(Cat.AppUi::class, Level.Smoke::class)
        @Test
        fun testMergeSceneWithEqualScene() {

        }

        @Category(Cat.AppUi::class, Level.Smoke::class)
        @Test
        fun testMergeScene() {

        }

        private fun createProjects(projectName: String) {
            project = Project(ApplicationProvider.getApplicationContext(), projectName)
            project.defaultScene.addSprite(Sprite("test"))
            projectManager.currentProject = project
            projectManager.currentlyEditedScene = project.defaultScene
            projectManager.currentSprite = project.defaultScene.getSprite("test")
            XstreamSerializer.getInstance().saveProject(project)
            localProject = DefaultProjectHandler.createAndSaveDefaultProject(
                "local",
                ApplicationProvider.getApplicationContext(),
                false
            )

            XstreamSerializer.getInstance().saveProject(localProject)
        }
}

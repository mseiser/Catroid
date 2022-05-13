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

package org.catrobat.catroid.uiespresso.ui.dialog


import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.test.utils.TestUtils
import org.catrobat.catroid.ui.ProjectActivity
import org.catrobat.catroid.uiespresso.util.UiTestUtils
import org.catrobat.catroid.uiespresso.util.rules.BaseActivityTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.java.KoinJavaComponent.inject
import java.io.File

class ImageResolutionAlertDialogTest {
    private lateinit var project : Project
    private var projectManager = inject(ProjectManager::class.java)
    @get:Rule
    var activityTestRule = BaseActivityTestRule(
        ProjectActivity::class.java, true, false
    )

    @Before
    @Throws(Exception::class)
    fun setUp() {
        project = UiTestUtils.createEmptyProject("testProject")
        projectManager.value.currentProject = project
        activityTestRule.launchActivity(null)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        TestUtils.deleteProjects(project.name)
    }

    @Test
    fun testLargeUploadAlertDialog() {
        val largeImageFilePath = "../../../../../../../." +
            "./test/resources/images/very-high-resolution-image.png"
        val file = File(largeImageFilePath)
        val intent = Intent()
        intent.data = Uri.fromFile(file)

        activityTestRule.activity.onActivityResult(ProjectActivity.SPRITE_FILE,
                                                   AppCompatActivity.RESULT_OK,intent)
        onView(ViewMatchers.withText(
            activityTestRule.activity.getString(R.string.Image_resolution_exceeds_memory)))
            .check(matches(isDisplayed()))
        onView(ViewMatchers.withText(
            activityTestRule.activity.getString(R.string.error_upload_high_resolution_image)))
            .check(matches(isDisplayed()))
        onView(withId(R.id.dialog_button)).perform(click())
    }
}

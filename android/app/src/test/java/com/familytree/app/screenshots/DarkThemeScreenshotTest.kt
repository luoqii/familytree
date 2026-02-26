package com.familytree.app.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.familytree.app.ui.screens.HomeScreen
import com.familytree.app.ui.screens.SettingsScreen
import com.familytree.app.ui.theme.FamilyTreeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 深色主题截图测试
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xxhdpi-night")
class DarkThemeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_dark() {
        composeTestRule.setContent {
            FamilyTreeTheme(darkTheme = true, dynamicColor = false) {
                HomeScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/HomeScreen_dark.png")
    }

    @Test
    fun settingsScreen_dark() {
        composeTestRule.setContent {
            FamilyTreeTheme(darkTheme = true, dynamicColor = false) {
                SettingsScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/SettingsScreen_dark.png")
    }
}

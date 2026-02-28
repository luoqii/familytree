package com.familytree.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.familytree.app.ui.screens.SettingsScreen
import com.familytree.app.ui.theme.FamilyTreeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-xxhdpi")
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysImportButton() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("导入 GEDCOM").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysExportButton() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("导出 GEDCOM").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysImportSubtitle() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("从 GEDCOM 5.5 文件导入家族数据").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysExportSubtitle() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("将家族数据导出为 GEDCOM 5.5 文件").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hasImportTestTag() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithTag("import_gedcom_button").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hasExportTestTag() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithTag("export_gedcom_button").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysDataSection() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("数据").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysSettingsTitle() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAboutSection() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithText("关于").assertIsDisplayed()
        composeTestRule.onNodeWithText("关于家族树").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hasScreenTestTag() {
        composeTestRule.setContent {
            FamilyTreeTheme(dynamicColor = false) {
                SettingsScreen()
            }
        }

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }
}

/*
 * Copyright (C) 2022 FlamingoOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flamingo.matlogx.ui

import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.flamingo.matlogx.ui.screens.LogcatScreen
import com.flamingo.matlogx.ui.screens.SettingsScreen
import com.flamingo.matlogx.ui.states.rememberLogcatScreenState
import com.flamingo.matlogx.ui.theme.LogcatTheme

class LogcatActivity : ComponentActivity() {

    private val documentTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            if (it == null) {
                finish()
                return@registerForActivityResult
            }
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, flags)
        }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatTheme {
                val systemUiController = rememberSystemUiController()
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                LaunchedEffect(primaryColor, surfaceColor) {
                    systemUiController.setStatusBarColor(primaryColor)
                    systemUiController.setNavigationBarColor(surfaceColor)
                }
                val navHostController = rememberAnimatedNavController()
                AnimatedNavHost(
                    navController = navHostController,
                    startDestination = Routes.HOME
                ) {
                    composable(
                        Routes.HOME,
                        exitTransition = {
                            when (targetState.destination.route) {
                                Routes.SETTINGS -> slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.Start,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                        popEnterTransition = {
                            when (initialState.destination.route) {
                                Routes.SETTINGS -> slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.End,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                    ) {
                        LogcatScreen(
                            onBackPressed = { finish() },
                            logcatScreenState = rememberLogcatScreenState(navHostController = navHostController)
                        )
                    }
                    composable(
                        Routes.SETTINGS,
                        enterTransition = {
                            when (initialState.destination.route) {
                                Routes.HOME -> slideIntoContainer(
                                    AnimatedContentScope.SlideDirection.Start,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        },
                        popExitTransition = {
                            when (targetState.destination.route) {
                                Routes.HOME -> slideOutOfContainer(
                                    AnimatedContentScope.SlideDirection.End,
                                    tween(ANIMATION_DURATION)
                                )
                                else -> null
                            }
                        }
                    ) {
                        SettingsScreen(navController = navHostController)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasValidUriPerms = contentResolver.persistedUriPermissions.firstOrNull()?.let {
            it.isReadPermission && it.isWritePermission
        } == true
        if (!hasValidUriPerms) {
            documentTreeLauncher.launch(null)
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 500
    }
}
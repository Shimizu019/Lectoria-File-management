package com.lectoria.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lectoria.ui.classes.ClassesScreen
import com.lectoria.ui.files.FilesScreen
import com.lectoria.ui.subjects.SubjectsScreen

/** Route patterns for the three screens. */
object LectoriaRoutes {
    const val CLASSES = "classes"
    const val SUBJECTS = "subjects/{className}"
    const val FILES = "files/{className}/{subjectName}"

    const val ARG_CLASS_NAME = "className"
    const val ARG_SUBJECT_NAME = "subjectName"
}

private fun NavHostController.toSubjects(className: String) {
    navigate("subjects/${Uri.encode(className)}")
}

private fun NavHostController.toFiles(className: String, subjectName: String) {
    navigate("files/${Uri.encode(className)}/${Uri.encode(subjectName)}")
}

/**
 * Classes -> Subjects -> Files.
 */
@Composable
fun LectoriaNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = LectoriaRoutes.CLASSES,
        modifier = modifier
    ) {
        composable(LectoriaRoutes.CLASSES) {
            ClassesScreen(
                onOpenClass = { className -> navController.toSubjects(className) }
            )
        }

        composable(
            route = LectoriaRoutes.SUBJECTS,
            arguments = listOf(
                navArgument(LectoriaRoutes.ARG_CLASS_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val className = backStackEntry.arguments
                ?.getString(LectoriaRoutes.ARG_CLASS_NAME)
                .orEmpty()

            SubjectsScreen(
                className = className,
                onOpenSubject = { subjectName ->
                    navController.toFiles(className, subjectName)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = LectoriaRoutes.FILES,
            arguments = listOf(
                navArgument(LectoriaRoutes.ARG_CLASS_NAME) { type = NavType.StringType },
                navArgument(LectoriaRoutes.ARG_SUBJECT_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val className = backStackEntry.arguments
                ?.getString(LectoriaRoutes.ARG_CLASS_NAME)
                .orEmpty()
            val subjectName = backStackEntry.arguments
                ?.getString(LectoriaRoutes.ARG_SUBJECT_NAME)
                .orEmpty()

            FilesScreen(
                className = className,
                subjectName = subjectName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
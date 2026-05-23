package com.project.markmyday.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.markmyday.ui.screens.*
import com.project.markmyday.ui.Auth.AuthenticationScreen
import com.project.markmyday.viewmodel.AuthViewModel
import com.project.markmyday.viewmodel.AuthResult
import com.project.markmyday.viewmodel.TeacherViewModel
import com.project.markmyday.viewmodel.StudentViewModel
import com.project.markmyday.viewmodel.LocalSettingsViewModel

sealed class Screen(val route: String) {
    object Authentication : Screen("auth")

    object StudentDashboard : Screen("student_dashboard/{name}/{role}/{studentId}/{uid}")
    object TeacherDashboard : Screen("teacher_dashboard/{name}/{role}/{section}/{subject}")
    object AdminDashboard : Screen("admin_dashboard/{name}/{role}")
    object Notifications : Screen("notifications/{role}")
    object Admissions : Screen("admissions")
    object StaffManagement : Screen("staff_management")
    object AdminAttendanceReport : Screen("admin_attendance_report")
    object AdminAttendanceStats : Screen("admin_attendance_stats")
    object StudentAttendanceReport : Screen("student_attendance_report/{uid}") {
        fun createRoute(uid: String) = "student_attendance_report/$uid"
    }
    object StudentAttendanceDashboard : Screen("student_attendance_dashboard/{studentId}") {
        fun createRoute(studentId: String) = "student_attendance_dashboard/$studentId"
    }
    object StudentLeave : Screen("student_leave")
    object AttendanceMarking : Screen("attendance_marking")
    object StudentManagement : Screen("student_management")
    object MyHomeStudents : Screen("myhome_students")
    object AdminLeaveManagement : Screen("admin_leave_management")
    object TeacherLeaveView : Screen("teacher_leave_view")
    object AdminCreateNotification : Screen("admin_create_notification")
    object QuizQuestionUpload : Screen("quiz_question_upload")
    object CourseManager : Screen("course_manager")
    object QuizTaking : Screen("quiz_taking/{subject}/{className}/{userName}/{studentId}") {
        fun createRoute(subject: String, className: String, userName: String, studentId: String) = 
            "quiz_taking/$subject/$className/${Uri.encode(userName)}/$studentId"
    }
    object QuizList : Screen("quiz_list/{className}/{userName}/{studentId}") {
        fun createRoute(className: String, userName: String, studentId: String) = 
            "quiz_list/$className/${Uri.encode(userName)}/$studentId"
    }
    object Leaderboard : Screen("leaderboard")
    object AdminTimetableClasses : Screen("admin_timetable_classes")
    object AdminTimetableGrid : Screen("admin_timetable_grid/{className}") {
        fun createRoute(className: String) = "admin_timetable_grid/$className"
    }
    
    // Bottom Bar Screens
    object Happenings : Screen("happenings")
    object Learning : Screen("SKP")
    object Reports : Screen("marks")
    object GlobalUpdates : Screen("global_updates")
    object Login : Screen("login")
    object Settings : Screen("settings")
    object Terms : Screen("terms")
    object About : Screen("about")
}

@Composable
fun AppNavigation(
    startDestination: String = Screen.Authentication.route,
    initialDashboardRoute: String? = null
) {
    val navController = rememberNavController()
    // Define AuthViewModel here to share it across all composables in this NavHost
    val authViewModel: AuthViewModel = viewModel()
    
    // Use a fixed start destination to avoid "not a direct child" errors
    // If an initial route is provided (e.g. from session check), navigate there once
    LaunchedEffect(initialDashboardRoute) {
        initialDashboardRoute?.let {
            navController.navigate(it) {
                popUpTo(Screen.Authentication.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    
    // Track the last dashboard route to know where "Home" should go
    var lastDashboardRoute by rememberSaveable { mutableStateOf(initialDashboardRoute ?: "") }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Authentication.route) {
            AuthenticationScreen(onLoginSuccess = { name, role, studentId, section, subject, uid ->
                // Routing logic based on user role
                val encodedName = Uri.encode(name)
                val encodedRole = Uri.encode(role)
                val encodedSection = Uri.encode(section ?: "N/A")
                val encodedSubject = Uri.encode(subject ?: "N/A")
                val encodedStudentId = Uri.encode(studentId ?: "N/A")

                val destination = when (role.lowercase()) {
                    "principal", "headmaster", "admin" -> "admin_dashboard/$encodedName/$encodedRole"
                    "teacher" -> "teacher_dashboard/$encodedName/$encodedRole/$encodedSection/$encodedSubject"
                    "student" -> {
                        val displayRole = if (section != null && section != "N/A") "Class $section" else role
                        val encodedDisplayRole = Uri.encode(displayRole)
                        "student_dashboard/$encodedName/$encodedDisplayRole/$encodedStudentId/$uid"
                    }
                    else -> "student_dashboard/$encodedName/$encodedRole/$encodedStudentId/$uid"
                }

                if (lastDashboardRoute.isEmpty()) {
                    lastDashboardRoute = destination
                }

                navController.navigate(destination) {
                    popUpTo(Screen.Authentication.route) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }



        // Screen.Login is no longer used as a separate route
        
        composable(
            Screen.StudentDashboard.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.StringType },
                navArgument("uid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Student"
            val role = backStackEntry.arguments?.getString("role") ?: "Student"
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            
            // Sync lastDashboardRoute if it's empty (e.g. on direct navigation)
            val currentRoute = Screen.StudentDashboard.route
                .replace("{name}", Uri.encode(name))
                .replace("{role}", Uri.encode(role))
                .replace("{studentId}", Uri.encode(studentId))
                .replace("{uid}", uid)
            
            if (lastDashboardRoute != currentRoute) {
                lastDashboardRoute = currentRoute
            }

            StudentDashboard(
                userName = name,
                userRole = role,
                studentId = studentId,
                uid = uid,
                onNotificationClick = { navController.navigate("notifications/${Uri.encode(role)}") },
                onTileClick = { id ->
                    when (id) {
                        "updates" -> navController.navigate(Screen.GlobalUpdates.route)
                        "results" -> {
                            val classNum = role.filter { it.isDigit() }.ifEmpty { "10" }
                            navController.navigate(Screen.Leaderboard.route + "?role=student&userClass=$classNum")
                        }
                        "notifications" -> navController.navigate("notifications/${Uri.encode(role)}")
                        "exams" -> {
                            val classNum = role.filter { it.isDigit() }.ifEmpty { "" }
                            if (classNum.isNotEmpty()) {
                                navController.navigate(Screen.QuizList.createRoute(classNum, name, studentId))
                            } else {
                                // Fallback
                                navController.navigate(Screen.QuizList.createRoute("10", name, studentId))
                            }
                        }
                        "leaderboard" -> {
                            val classNum = role.filter { it.isDigit() }.ifEmpty { "10" }
                            navController.navigate(Screen.Leaderboard.route + "?role=student&userClass=$classNum")
                        }
                        "settings" -> navController.navigate(Screen.Settings.route)
                    }
                },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) },
                navController = navController
            )
        }
        
        composable(
            Screen.TeacherDashboard.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
                navArgument("section") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Teacher"
            val role = backStackEntry.arguments?.getString("role") ?: "Teacher"
            val section = backStackEntry.arguments?.getString("section") ?: "N/A"
            val subject = backStackEntry.arguments?.getString("subject") ?: "N/A"

            // Sync lastDashboardRoute
            val currentRoute = Screen.TeacherDashboard.route
                .replace("{name}", Uri.encode(name))
                .replace("{role}", Uri.encode(role))
                .replace("{section}", Uri.encode(section))
                .replace("{subject}", Uri.encode(subject))

            if (lastDashboardRoute != currentRoute) {
                lastDashboardRoute = currentRoute
            }

            TeacherDashboard(
                userName = name,
                userRole = role,
                homeSection = section,
                subject = subject,
                onNotificationClick = { navController.navigate("notifications/${Uri.encode(role)}") },
                onTileClick = { id ->
                    when (id) {
                        "updates" -> navController.navigate(Screen.GlobalUpdates.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                        "notifications" -> navController.navigate("notifications/${Uri.encode(role)}")
                        "exams" -> navController.navigate(Screen.QuizQuestionUpload.route)
                        "results" -> navController.navigate(Screen.Leaderboard.route + "?role=teacher&userClass=$section")
                        "admissions" -> navController.navigate(Screen.Admissions.route)
                        "leave" -> navController.navigate(Screen.TeacherLeaveView.route)
                        "attendance" -> navController.navigate(Screen.AttendanceMarking.route)
                        "course_manager" -> navController.navigate(Screen.CourseManager.route)
                        "myhome_students" -> navController.navigate(Screen.MyHomeStudents.route)
                        // Add more routing as screens are implemented
                    }
                },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }
        
        composable(
            Screen.AdminDashboard.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Admin"
            val role = backStackEntry.arguments?.getString("role") ?: "Administrator"
            val context = androidx.compose.ui.platform.LocalContext.current

            // Sync lastDashboardRoute
            val currentRoute = Screen.AdminDashboard.route
                .replace("{name}", Uri.encode(name))
                .replace("{role}", Uri.encode(role))

            if (lastDashboardRoute != currentRoute) {
                lastDashboardRoute = currentRoute
            }

            AdminDashboard(
                userName = name,
                userRole = role,
                onNotificationClick = { navController.navigate("notifications/${Uri.encode(role)}") },
                onTileClick = { id -> 
                    when (id) {
                        "admissions" -> navController.navigate(Screen.Admissions.route)
                        "notices" -> navController.navigate(Screen.AdminCreateNotification.route)
                        "updates" -> navController.navigate(Screen.GlobalUpdates.route)
                        "reports" -> navController.navigate(Screen.Leaderboard.route + "?role=admin&userClass=ALL")
                        "leave" -> navController.navigate(Screen.AdminLeaveManagement.route)
                        "add_staff" -> {
                            context.startActivity(android.content.Intent(context, AddStaffActivity::class.java))
                        }
                        "add_student" -> {
                            context.startActivity(android.content.Intent(context, AddStudentActivity::class.java))
                        }
                        "create_timetable" -> {
                            context.startActivity(android.content.Intent(context, CreateTimetableActivity::class.java))
                        }
                        "timetable" -> navController.navigate(Screen.AdminTimetableClasses.route)
                        "staff_management" -> navController.navigate(Screen.StaffManagement.route)
                        "attendance_reports" -> navController.navigate(Screen.AdminAttendanceReport.route)
                        "attendance_stats" -> navController.navigate(Screen.AdminAttendanceStats.route)
                        "attendance_overview" -> {
                            context.startActivity(android.content.Intent(context, AdminAttendanceOverviewActivity::class.java))
                        }
                        "students" -> navController.navigate(Screen.StudentManagement.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                        "quiz_upload" -> navController.navigate(Screen.QuizQuestionUpload.route)
                        "course_manager" -> navController.navigate(Screen.CourseManager.route)
                        // Add more routing as screens are implemented
                    }
                },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) },
                navController = navController
            )
        }
        
        composable(Screen.StaffManagement.route) {
            val teacherViewModel: TeacherViewModel = viewModel()
            val teachers by teacherViewModel.allTeachers.collectAsState()
            
            StaffManagementScreen(
                teachers = teachers,
                onEditTeacher = { teacherViewModel.updateTeacher(it) },
                onDeleteTeacher = { teacherViewModel.deleteTeacher(it.teacherId) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminAttendanceReport.route) {
            AdminAttendanceReportScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AdminAttendanceStats.route) {
            AdminAttendanceStatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AdminTimetableClasses.route) {
            AdminTimetableClassesScreen(
                onClassSelected = { className ->
                    navController.navigate(Screen.AdminTimetableGrid.createRoute(className))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.AdminTimetableGrid.route,
            arguments = listOf(navArgument("className") { type = NavType.StringType })
        ) { backStackEntry ->
            val className = backStackEntry.arguments?.getString("className") ?: ""
            AdminTimetableGridScreen(
                className = className,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StudentAttendanceReport.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            StudentAttendanceReportScreen(
                studentUid = uid,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.StudentAttendanceDashboard.route,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            StudentAttendanceDashboardScreen(
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StudentLeave.route) {
            LeaveScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AttendanceMarking.route) {
            AttendanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.MyHomeStudents.route) {
            MyHomeStudents(onBack = { navController.popBackStack() })
        }

        composable(Screen.StudentManagement.route) {
            val studentViewModel: StudentViewModel = viewModel()
            val students by studentViewModel.allStudents.collectAsState()
            
            StudentManagementScreen(
                students = students,
                onEditStudent = { studentViewModel.updateStudent(it) },
                onDeleteStudent = { studentViewModel.deleteStudent(it.studentId) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Admissions.route) {
            AdmissionsScreen(
                role = "Staff",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminLeaveManagement.route) {
            AdminLeaveManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TeacherLeaveView.route) {
            TeacherLeaveViewScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Screen.Notifications.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "Student"
            NotificationScreen(role = role, onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AdminCreateNotification.route) {
            AdminCreateNotificationScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.QuizQuestionUpload.route) {
            QuizQuestionUploadScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CourseManager.route) {
            CourseManagerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.QuizList.route,
            arguments = listOf(
                navArgument("className") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val className = backStackEntry.arguments?.getString("className") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: "Student"
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            QuizListScreen(
                className = className,
                onQuizSelected = { subject ->
                    navController.navigate(Screen.QuizTaking.createRoute(subject, className, userName, studentId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QuizTaking.route,
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            val className = backStackEntry.arguments?.getString("className") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: "Student"
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            QuizTakingScreen(
                subject = subject,
                className = className,
                userName = userName,
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Leaderboard.route + "?role={role}&userClass={userClass}",
            arguments = listOf(
                navArgument("role") { type = NavType.StringType; defaultValue = "student" },
                navArgument("userClass") { type = NavType.StringType; defaultValue = "10" }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "student"
            val userClass = backStackEntry.arguments?.getString("userClass") ?: "10"
            LeaderboardScreen(
                role = role,
                userClass = userClass,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }

        // New Bottom Bar Routes
        composable(Screen.Happenings.route) {
            val authState by authViewModel.authState.collectAsState()
            val role = if (authState is AuthResult.Success) (authState as AuthResult.Success).role else "Student"
            
            NotificationScreen(
                role = role, 
                onBackClick = { /* No back on main tabs */ },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }

        composable(Screen.Learning.route) {
            val authState by authViewModel.authState.collectAsState()
            val roleInfo = remember(authState) {
                if (authState is AuthResult.Success) {
                    val success = authState as AuthResult.Success
                    val section = success.homeSection
                    if (section != null && section != "N/A") "Class $section" else success.role
                } else "Student"
            }
            
            CourseLibraryScreen(
                userRole = roleInfo,
                onNotificationClick = { 
                    navController.navigate("notifications/${Uri.encode(roleInfo)}") 
                },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }

        composable(Screen.Reports.route) {
            val authState by authViewModel.authState.collectAsState()
            val roleInfo = remember(authState) {
                if (authState is AuthResult.Success) (authState as AuthResult.Success).role else "Student"
            }
            
            ReportsScreen(
                onNotificationClick = { 
                    navController.navigate("notifications/${Uri.encode(roleInfo)}")
                },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }

        composable(Screen.GlobalUpdates.route) {
            GlobalUpdateScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> handleBottomNav(route, navController, lastDashboardRoute) }
            )
        }

        composable(Screen.Settings.route) {
            com.project.markmyday.settings.SettingsScreen(
                viewModel = LocalSettingsViewModel.current,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Authentication.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.Terms.route) {
            TermsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun handleBottomNav(route: String, navController: NavHostController, lastDashboardRoute: String) {
    // Map internal route names to actual screen routes if needed
    val actualRoute = when (route) {
        "marks" -> {
            val role = if (lastDashboardRoute.contains("admin")) "admin" 
                      else if (lastDashboardRoute.contains("teacher")) "teacher" 
                      else "student"
            Screen.Leaderboard.route + "?role=$role&userClass=ALL"
        }
        "dashboard" -> lastDashboardRoute
        else -> route
    }
    
    if (navController.currentBackStackEntry?.destination?.route == actualRoute) {
        return
    }

    navController.navigate(actualRoute) {
        popUpTo(lastDashboardRoute) {
            saveState = true
        }
        
        launchSingleTop = true
        restoreState = true
    }
}

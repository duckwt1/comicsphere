package com.android.dacs3.utliz

sealed class Screens(val route: String) {
    object SplashScreen : Screens("splash_screen")
    object HistoryScreen : Screens("history_screen")
    object FavouriteScreen : Screens("favourite_screen")
    object ExploreScreen : Screens("explore_screen")
    object ProfileScreen : Screens("profile_screen")
    object LoginScreen : Screens("login_screen")
    object SignUpScreen : Screens("signup_screen")
    object ForgotPasswordScreen : Screens("forgot_password")
    object SettingsScreen : Screens("settings_screen")
    object VipScreen : Screens("vip_screen")
    object DetailsScreen : Screens("details_screen/{id}") {
        fun createRoute(id: String): String = "details_screen/$id"
    }

    object ChapterScreen : Screens("chapter_screen/{mangaId}/{chapterId}/{language}/{pageIndex}") {
        fun createRoute(mangaId: String, chapterId: String, language: String, pageIndex: Int = 0): String =
            "chapter_screen/$mangaId/$chapterId/$language/$pageIndex"
    }

    object SearchScreen : Screens("search_screen")
    object NotificationsScreen : Screens("notifications_screen")
    object EditProfileScreen : Screens("edit_profile_screen")
    object HelpScreen : Screens("help_screen")
}

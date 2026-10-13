// Suppress benign IntelliJ/KSP headless AWT-EventQueue-0 race condition in CLI environments
val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
    if (thread.name.contains("AWT-EventQueue") && throwable is NullPointerException) {
        // Silently ignore benign KSP IntelliJ decompiler race on headless event queue
        return@setDefaultUncaughtExceptionHandler
    }
    prevHandler?.uncaughtException(thread, throwable)
}

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "BUSHRA AI"

include(":app")

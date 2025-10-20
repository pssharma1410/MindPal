pluginManagement {
    repositories {
//        maven("https://jitpack.io")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }

        mavenCentral()
//        maven("https://jitpack.io")
        gradlePluginPortal()

    }
}
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//
//
//
////        maven { url = uri("https://maven.scijava.org/content/repositories/public/") }
////        maven("https://jitpack.io")
//    }
//}
dependencyResolutionManagement {
     repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This line might be present
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // <-- ADD THIS LINE
    }
}

rootProject.name = "MindPal"
include(":app")

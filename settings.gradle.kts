pluginManagement {
    repositories {
        google() // ✅ 不做任何限制
        gradlePluginPortal() // ✅ 这个一定要放在这里
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Learn IELTS"
include(":app")

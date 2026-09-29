@file:Suppress("UnstableApiUsage")


// Fail fast when Gradle runs on a JRE without jlink (e.g. Cursor/VS Code Red Hat Java extension).
run {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val jlink = File(System.getProperty("java.home"), if (isWindows) "bin/jlink.exe" else "bin/jlink")
    check(jlink.isFile && jlink.canExecute()) {
        buildString {
            appendLine("Gradle JVM is missing jlink at ${jlink.absolutePath}.")
            appendLine("Android builds need a full JDK (Android Studio JBR, Temurin 21, etc.).")
            append("Android Studio: Settings → Build, Execution, Deployment → Build Tools → Gradle → ")
            appendLine("Gradle JDK → Embedded JDK or JDK 21.")
            append("CLI: export JAVA_HOME to a full JDK, or set org.gradle.java.home in ~/.gradle/gradle.properties.")
        }
    }
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Fokus Launcher"
include(":app")
 
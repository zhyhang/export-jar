// reference: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html#configuration.repositories

rootProject.name = "export-jar"
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
        }
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}



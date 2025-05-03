import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
// reference: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts
plugins {
    // version config in: gradle/libs.versions.toml
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin. See official introduce: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
// final effected target JVM version in task{} section setting
// https://docs.gradle.org/current/userguide/toolchains.html
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
// All repositories are configured in settings.gradle.kts
repositories {
    // Aliyun Public Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/public/")
    }
    // Gradle Plugin Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
    }
    // Spring Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/spring/")
    }
    // Spring Plugin Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/spring-plugin/")
    }
    // Google Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/google/")
    }
    // JCenter Repository
    maven {
        url = uri("https://maven.aliyun.com/repository/jcenter/")
    }

    // Maven Central Repository
    mavenCentral()

    // JetBrains IntelliJ Platform Repositories
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {

    // test, read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#testing
    // how write unit test: https://plugins.jetbrains.com/docs/intellij/testing-plugins.html
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(properties("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(properties("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        // TestFrameworkType.Platform.JUnit4 to TestFrameworkType.Platform
        // TestFrameworkType.Platform.JUnit5 to TestFrameworkType.JUnit5
        // TestFrameworkType.Platform.Bundled to TestFrameworkType.Bundled
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map {
            listOf(
                it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html#intellijPlatform-pluginVerification
    // ./gradlew clean verifyPlugin -V -i to run verify task and report store in ./build/reports
    pluginVerification {
        // The list of free arguments is passed directly to the IntelliJ Plugin Verifier CLI tool.
        // Cli args supports:  https://github.com/JetBrains/intellij-plugin-verifier#common-options
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        failureLevel = VerifyPluginTask.FailureLevel.NONE
        ides {
            ides(properties("verifyPluginUseIdes").get().split(','))
            // also can use following lines to specify verify using IDEs
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2020.2")
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1")
//            local(file("/path/to/ide/"))
//            recommended()
//            select {
//                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
//                channels = listOf(ProductRelease.Channel.RELEASE)
//                sinceBuild = properties("pluginSinceBuild")
//                untilBuild = "241.*"
//            }
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}


tasks {
    // Set the JVM compatibility versions, javaVersion setting in gradle.properties
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it.get()
            targetCompatibility = it.get()
        }
        withType<KotlinCompile> {
            compilerOptions.jvmTarget = JvmTarget.fromTarget(it.get())
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    // Configure UI tests plugin
    // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIdeForUiTests
    // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#testIdeUi
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    /**
     * testIdeUi task not registered default (real state is not available as my test) in v2.0.1
     * use runIdeForUiTests instead, as well need register in following intellijPlatformTesting section
     * https://github.com/JetBrains/intellij-platform-gradle-plugin/releases/tag/v2.0.1
    testIdeUi {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }
    */
}
intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") { // this task is same as Run Plugin task in idea, as result could not register it
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}

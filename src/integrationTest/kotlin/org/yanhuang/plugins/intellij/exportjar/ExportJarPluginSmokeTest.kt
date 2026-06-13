package org.yanhuang.plugins.intellij.exportjar

import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import kotlin.io.path.Path

/**
 * Smoke UI/integration test using the Starter framework + Driver.
 *
 * Verifies that the freshly built plugin installs into a real IntelliJ IDEA Community
 * instance and the IDE starts up (welcome screen) without recording any IDE-side
 * exception. The CIServer override below turns any IDE exception into a test failure.
 *
 * Run with: ./gradlew integrationTest   (downloads & launches a real IDE; needs a display)
 */
class ExportJarPluginSmokeTest {

    init {
        di = DI {
            extend(di)
            bindSingleton<CIServer>(overrides = true) {
                object : CIServer by NoCIServer {
                    override fun reportTestFailure(
                        testName: String,
                        message: String,
                        details: String,
                        linkToLogs: String?
                    ) {
                        fail { "$testName fails: $message. \n$details" }
                    }
                }
            }
        }
    }

    @Test
    fun pluginLoadsAndIdeStarts() {
        Starter.newContext(
            testName = "exportJarSmoke",
            testCase = TestCase(
                IdeProductProvider.IC,
                projectInfo = NoProject
            ).withVersion("2025.1")
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            // Smoke check: the IDE started with the plugin installed and the driver
            // connected. Any IDE-side startup exception is turned into a test failure
            // by the CIServer override above, so reaching here cleanly is the assertion.
            // (No waitForIndicators: this uses NoProject, i.e. the welcome screen, where
            //  there is no project indexing to wait on.)
            isConnected
        }
    }
}

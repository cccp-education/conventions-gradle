package build

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit coverage for [TestDependencies] (CNV-10.7 gap): the routing policy and
 * the fallback coordinates are pure, so they can be asserted without a Gradle
 * build.
 */
class TestDependenciesTest {

    @Test
    fun `launcher routes to the runtime only configuration`() {
        assertEquals(
            "testRuntimeOnly",
            TestDependencies.targetConfiguration(
                TestDependencies.RUNTIME_ONLY_KEY,
                "testImplementation",
                "testRuntimeOnly"
            )
        )
    }

    @Test
    fun `every other junit dependency routes to the implementation configuration`() {
        val runtimeOnly = "testRuntimeOnly"
        val implementation = "testImplementation"

        TestDependencies.JUNIT_FALLBACKS.keys
            .filter { it != TestDependencies.RUNTIME_ONLY_KEY }
            .forEach { key ->
                assertEquals(
                    implementation,
                    TestDependencies.targetConfiguration(key, implementation, runtimeOnly),
                    "$key must be an implementation dependency"
                )
            }
    }

    @Test
    fun `cucumber fallbacks are valid maven coordinates`() {
        TestDependencies.CUCUMBER_FALLBACKS.forEach { (key, coordinates) ->
            assertValidCoordinates(key, coordinates)
        }
        assertEquals("7.34.3", versionOf("cucumber-java"))
        assertEquals("7.34.3", versionOf("cucumber-junit-platform-engine"))
        assertEquals("7.34.3", versionOf("cucumber-picocontainer"))
        assertEquals("7.34.3", versionOf("cucumber-java8"))
        assertEquals("1.14.3", versionOf("junit-platform-suite"))
    }

    @Test
    fun `junit fallbacks are valid maven coordinates`() {
        TestDependencies.JUNIT_FALLBACKS.forEach { (key, coordinates) ->
            assertValidCoordinates(key, coordinates)
        }
        assertEquals("2.4.10", versionOf("kotlin-test-junit5"))
        assertEquals("5.12.2", versionOf("junit-jupiter"))
        assertEquals("1.14.3", versionOf(TestDependencies.RUNTIME_ONLY_KEY))
        assertEquals("5.12.2", versionOf("junit-platform-params"))
        assertEquals("3.25.3", versionOf("assertj-core"))
    }

    private fun versionOf(key: String): String =
        (TestDependencies.CUCUMBER_FALLBACKS[key] ?: TestDependencies.JUNIT_FALLBACKS[key])
            .orEmpty()
            .substringAfterLast(':')

    private fun assertValidCoordinates(key: String, coordinates: String) {
        val parts = coordinates.split(':')
        assertEquals(3, parts.size, "$key fallback '$coordinates' must be group:artifact:version")
        assertTrue(parts.all { it.isNotBlank() }, "$key fallback '$coordinates' has a blank part")
    }
}

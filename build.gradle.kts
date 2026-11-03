import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

val mmpGroup = providers.gradleProperty("GROUP").orElse("com.github.e-hai.MmpKit-Android")
val mmpVersion = providers.gradleProperty("VERSION_NAME").orElse("1.0.0")

subprojects {
    if (!name.startsWith("mmp-")) return@subprojects

    pluginManager.withPlugin("com.android.library") {
        apply(plugin = "maven-publish")

        group = mmpGroup.get()
        version = mmpVersion.get()

        extensions.configure<LibraryExtension>("android") {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    register<MavenPublication>("release") {
                        from(components["release"])
                        groupId = project.group.toString()
                        artifactId = project.name
                        version = project.version.toString()

                        pom {
                            name.set(project.name)
                            description.set("MmpKit-Android — unified Android MMP attribution SDK wrappers")
                            url.set("https://github.com/e-hai/MmpKit-Android")

                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }

                            developers {
                                developer {
                                    id.set("e-hai")
                                    name.set("e-hai")
                                }
                            }

                            scm {
                                connection.set("scm:git:git://github.com/e-hai/MmpKit-Android.git")
                                developerConnection.set("scm:git:ssh://github.com/e-hai/MmpKit-Android.git")
                                url.set("https://github.com/e-hai/MmpKit-Android")
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.myapplication.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.tasks.JacocoReport

class MyCustomPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply("kotlin-android")
        project.extensions.create<MyPluginOptionExtension>("myPluginOptions")

        val androidExtension = project.extensions.getByName("android")
        if (androidExtension is BaseExtension) {
            androidExtension.applyAndroidSettings(project)
            androidExtension.enableJava8(project)
            androidExtension.configureJacoco(project)
        }
    }

    private fun BaseExtension.configureJacoco(project: Project) {
        project.afterEvaluate {
            val jacocoOptions = project.extensions.getByType<MyPluginOptionExtension>().jacoco

            if (jacocoOptions.isEnabled) {
                project.plugins.apply("jacoco")
                when (this@configureJacoco) {
                    is LibraryExtension -> configureJacocoTasks(
                        project,
                        libraryVariants,
                        jacocoOptions
                    )
                    is AppExtension -> configureJacocoTasks(
                        project,
                        applicationVariants,
                        jacocoOptions
                    )
                }
            }
        }
    }

    private fun configureJacocoTasks(
        project: Project,
        variants: DomainObjectSet<out BaseVariant>,
        options: JacocoOptions
    ) {
        variants.all {
            val variantName = name
            val isDebuggable = this.buildType.isDebuggable

            if (!isDebuggable) {
                project.logger.info("Skipping Jacoco for $name because it is not debuggable.")
                return@all
            }

            project.tasks.register<JacocoReport>("jacoco${variantName.capitalize()}Report") {
                dependsOn(project.tasks["test${variantName.capitalize()}UnitTest"])
                val coverageSourceDirs = "src/main/java"

                val javaClasses = project
                    .fileTree("${project.buildDir}/intermediates/javac/$variantName") {
                        setExcludes(options.excludes)
                    }

                val kotlinClasses = project
                    .fileTree("${project.buildDir}/tmp/kotlin-classes/$variantName") {
                        setExcludes(options.excludes)
                    }

                // Using the default Jacoco exec file output path.
                val execFile = "jacoco/test${variantName.capitalize()}UnitTest.exec"

                executionData.setFrom(
                    project.fileTree("${project.buildDir}") {
                        setIncludes(listOf(execFile))
                    }
                )

                // Do not run task if there's no execution data.
                setOnlyIf { executionData.files.any{ it.exists()} }

                classDirectories.setFrom(javaClasses, kotlinClasses)
                sourceDirectories.setFrom(coverageSourceDirs)
                additionalSourceDirs.setFrom(coverageSourceDirs)

                reports.html.required.set(true)
                reports.xml.required.set(false)
                reports.csv.required.set(false)
            }
        }
    }


    private fun BaseExtension.enableJava8(project: Project) {
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        project.dependencies {
            add("implementation", "androidx.core:core-ktx:1.6.0")
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:1.5.21")

            add("androidTestImplementation","androidx.test:core:1.2.0")
            add("androidTestImplementation","junit:junit:4:12")
            add("androidTestImplementation","androidx.test.ext:junit:1.1.1")

            add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:1.1.5")
        }
    }

    private fun BaseExtension.applyAndroidSettings(project: Project) {
        compileSdkVersion(30)
        defaultConfig {
            targetSdk = 30
            minSdk = 23

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        project.configurations.all {
            resolutionStrategy.failOnVersionConflict()
            resolutionStrategy.preferProjectModules()
        }
    }

    private fun BaseExtension.applyProguardSettings() {
        val proguardFile = "proguard-rules.pro"
        when (this) {
            is LibraryExtension -> defaultConfig {
                consumerProguardFiles(proguardFile)
            }
            is AppExtension -> buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        proguardFile
                    )
                }
            }
        }
    }
}
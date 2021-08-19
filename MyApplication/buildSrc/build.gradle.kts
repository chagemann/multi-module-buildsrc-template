plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.1.0-alpha08")
    implementation("org.jacoco:org.jacoco.core:0.8.5")

    implementation(gradleApi())

}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

gradlePlugin {
    plugins {
        register("my-custom-plugin") {
            id = "my-custom-plugin"
            implementationClass = "com.myapplication.plugin.MyCustomPlugin"
            version = "1.0.0"
        }
    }
}
import org.jreleaser.model.Active

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jreleaser")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "lib.atomofiron.insets"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    testImplementation("junit:junit:4.13.2")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = properties["GROUP"].toString()
            artifactId = properties["POM_ARTIFACT_ID"].toString()
            version = properties["VERSION_NAME"].toString()

            pom {
                name.set(project.properties["POM_NAME"].toString())
                description.set(project.properties["POM_DESCRIPTION"].toString())
                url.set("https://github.com/atomofiron/android-extended-insets")
                issueManagement {
                    url.set("https://github.com/atomofiron/android-extended-insets/issues")
                }

                scm {
                    url.set("https://github.com/atomofiron/android-extended-insets")
                    connection.set("scm:git://github.com/atomofiron/android-extended-insets.git")
                    developerConnection.set("scm:git://github.com/atomofiron/android-extended-insets.git")
                }

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/license/mit")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("atomofiron")
                        name.set("Jaroslav Nesterov")
                        email.set("atomofiron@gmail.com")
                        url.set("https://atomofiron.github.io")
                    }
                }

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

version = properties["VERSION_NAME"].toString()
description = properties["POM_DESCRIPTION"].toString()

jreleaser {
    project {
        inceptionYear = "2024"
        author("@atomofiron")
    }
    gitRootSearch = true
    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }
    release {
        github {
            skipRelease = true
            skipTag = true
        }
    }
    release {
        github {
            skipTag = true
            sign = true
            branch = "develop"
            branchPush = "develop"
            overwrite = true
        }
    }
    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                setAuthorization("Basic")
                applyMavenCentralRules = false // Wait for fix: https://github.com/kordamp/pomchecker/issues/21
                sign = true
                checksums = true
                sourceJar = true
                javadocJar = true
                retryDelay = 60
            }
        }
    }
}

import com.google.protobuf.gradle.proto
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.net.URL

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.protobuf)
}

version = project.findProperty("node.android.version") as String
val versionCodeValue = (project.findProperty("node.android.version.code") as String).toInt()
val sharedVersion = project.findProperty("shared.version") as String

kotlin {
    // using JDK21 for full bisq2 compatibility
    jvmToolchain(21)
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        val androidMain by getting {
            androidMain.dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
            }
            androidUnitTest.dependencies {
                implementation(libs.mock.io)
                implementation(libs.kotlin.test.junit.v180)
                implementation(libs.junit)

                implementation(libs.roboelectric)
                implementation(libs.androidx.test)
                implementation(libs.androidx.test.espresso)
                implementation(libs.androidx.test.junit)
            }
            androidInstrumentedTest.dependencies {
                implementation(libs.mock.io)
                implementation(libs.kotlin.test.junit.v180)
                implementation(libs.junit)

                implementation(libs.androidx.test.espresso)
                implementation(libs.androidx.test)
                implementation(libs.androidx.test.junit)
            }
            kotlin.srcDirs(
                "src/androidMain/kotlin",
                "${layout.buildDirectory}/generated/source/proto/debug/java",
                "${layout.buildDirectory}/generated/source/proto/release/java"
            )
        }
    }
}

val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

android {
    namespace = "network.bisq.mobile.node"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            if (localProperties["KEYSTORE_PATH"] != null) {
                storeFile = file(localProperties["KEYSTORE_PATH"] as String)
                storePassword = localProperties["KEYSTORE_PASSWORD"] as String
                keyAlias = localProperties["KEY_ALIAS"] as String
                keyPassword = localProperties["KEY_PASSWORD"] as String
            }
        }
    }

    sourceSets {
        // TODO can we pick just the protos for each build type? release vs debug
        getByName("debug") {
            java {
                srcDir("src/main/resources")
                srcDir("${layout.buildDirectory}/generated/source/proto/debug/java")
                srcDir("${layout.buildDirectory}/generated/source/proto/release/java")
                proto {
                    srcDir("${layout.buildDirectory}/extracted-include-protos/debug")
                }
            }
        }
        getByName("release") {
            java {
                srcDir("src/release/resources")
                srcDir("${layout.buildDirectory}/generated/source/proto/debug/java")
                srcDir("${layout.buildDirectory}/generated/source/proto/release/java")
                proto {
                    srcDir("${layout.buildDirectory}/extracted-include-protos/debug")
                }
            }
        }
    }

    defaultConfig {
        applicationId = "network.bisq.mobile.node"
        minSdk = libs.versions.android.node.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        multiDexEnabled = true
        versionCode = versionCodeValue
        versionName = project.version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // the following exclude are needed to avoid protobuf hanging build when merging release resources for java
            // Exclude the conflicting META-INF files
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/NOTICE.md")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/NOTICE.markdown")
            pickFirsts.add("**/protobuf/**/*.class")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
            isDebuggable = false
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = project.name
            val version = variant.versionName
            val fileName = "$appName-$version.apk"
            output.outputFileName = fileName
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        // for bisq2 jars full compatibility
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    // needed for aab files renaming
    setProperty("archivesBaseName", getArtifactName(defaultConfig))
}

// Compatible with macOS on Apple Silicon
val archSuffix = if (Os.isFamily(Os.FAMILY_MAC)) ":osx-x86_64" else ""

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2$archSuffix"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.inputs.dir("${layout.buildDirectory.get()}/extracted-include-protos/debug")
            task.builtins {
                create("java")
            }
        }
    }
}
dependencies {
    implementation(project(":shared:presentation"))
    implementation(project(":shared:domain"))
    debugImplementation(compose.uiTooling)

    // bisq2 core dependencies
    implementation(libs.androidx.multidex)
    implementation(libs.google.guava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.typesafe.config)

    implementation(libs.bouncycastle)
    implementation(libs.bouncycastle.pg)

    implementation(libs.bisq.core.common)
    implementation(libs.bisq.core.i18n)
    implementation(libs.bisq.core.persistence)
    implementation(libs.bisq.core.security)
    // # bisq:core:network#
    implementation(libs.bisq.core.network.network)
    implementation(libs.bisq.core.network.network.identity)
    implementation(libs.bisq.core.network.socks5.socket.channel)
    implementation(libs.bisq.core.network.i2p)
    implementation(libs.chimp.jsocks)
    implementation(libs.failsafe)
    implementation(libs.apache.httpcomponents.httpclient)
    // ##### network ######
    implementation(libs.bisq.core.identity)
    implementation(libs.bisq.core.account)
    implementation(libs.bisq.core.settings)
    implementation(libs.bisq.core.bonded.roles)
    implementation(libs.bisq.core.user)
    implementation(libs.bisq.core.contract)
    implementation(libs.bisq.core.offer)
    implementation(libs.bisq.core.trade)
    implementation(libs.bisq.core.support)
    implementation(libs.bisq.core.application)
    implementation(libs.bisq.core.chat)
    implementation(libs.bisq.core.presentation)
    implementation(libs.bisq.core.bisq.easy)

    // protobuf
    implementation(libs.protobuf.gradle.plugin)
    implementation(libs.protoc)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.logging.kermit)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // For tar.gz extraction in the build script
    implementation("org.apache.commons:commons-compress:1.24.0")
    
    // Add ASM dependency for Commons Compress
    implementation("org.ow2.asm:asm:9.6")
}

// Define a proper task class for configuration cache compatibility
abstract class DownloadTorBinariesTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    val torVersion = "0.4.7.13" // Check available versions
    
    @get:Input
    val architectures = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    
    init {
        outputDir.set(project.layout.projectDirectory.dir("src/main/assets/tor"))
    }
    
    @TaskAction
    fun download() {
        val assetsDir = outputDir.get().asFile
        assetsDir.mkdirs()
        
        architectures.forEach { arch ->
            val destDir = File(assetsDir, arch)
            destDir.mkdirs()
            
            val torBinary = File(destDir, "tor")
            if (!torBinary.exists()) {
                println("Downloading Tor binary for $arch")
                try {
                    // Guardian Project's pre-compiled binaries
                    val url = URL("https://guardianproject.info/releases/tor-android-binary-$torVersion-$arch.zip")
                    
                    // Download and extract the zip file
                    val tempFile = File.createTempFile("tor-", ".zip")
                    tempFile.deleteOnExit()
                    
                    url.openStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Extract using Apache Commons Compress
                    tempFile.inputStream().use { fileInputStream ->
                        org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(fileInputStream).use { zipIn ->
                            var entry = zipIn.nextZipEntry
                            while (entry != null) {
                                if (entry.name.endsWith("/tor") || entry.name == "tor") {
                                    torBinary.outputStream().use { output ->
                                        zipIn.copyTo(output)
                                    }
                                    torBinary.setExecutable(true)
                                    println("Extracted Tor binary for $arch successfully")
                                    break
                                }
                                entry = zipIn.nextZipEntry
                            }
                        }
                    }
                    
                    tempFile.delete()
                } catch (e: Exception) {
                    println("Failed to download Tor binary for $arch: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                println("Tor binary for $arch already exists")
            }
        }
    }
}

// Register the task using the proper class
tasks.register<DownloadTorBinariesTask>("downloadTorBinaries")

// Make sure the download task runs before the build
tasks.named("preBuild") {
    dependsOn("downloadTorBinaries")
}

// ensure tests run on the same Java version as the main code
tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21)) // Update from 17 to 21
        }
    )
}

fun getArtifactName(defaultConfig: com.android.build.gradle.internal.dsl.DefaultConfig): String {
    return "Bisq-${defaultConfig.versionName}_${defaultConfig.versionCode}"
}

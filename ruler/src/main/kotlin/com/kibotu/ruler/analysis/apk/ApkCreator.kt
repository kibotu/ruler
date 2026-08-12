package com.kibotu.ruler.analysis.apk

import com.android.SdkConstants
import com.android.bundle.Commands.BuildApksResult
import com.android.bundle.Devices
import com.android.prefs.AndroidLocationsSingleton
import com.android.repository.api.ProgressIndicatorAdapter
import com.android.sdklib.repository.AndroidSdkHandler
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.device.DeviceSpecParser
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.kibotu.ruler.analysis.DeviceSpec
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import java.util.Optional

/**
 * Splits an app bundle into the APKs that one device receives.
 *
 * @param androidSdkDir Android SDK root, used to locate aapt2. Falls back to `ANDROID_HOME`.
 */
class ApkCreator(private val androidSdkDir: File? = null) {

    /**
     * @param bundleFile The AAB to split.
     * @param deviceSpec The device to build for.
     * @param targetDir Where to write the APKs. Ruler deletes the current contents.
     * @return Each module of the bundle, mapped to its APKs.
     */
    fun createSplitApks(
        bundleFile: File,
        deviceSpec: DeviceSpec,
        targetDir: File,
    ): Map<String, List<File>> {
        targetDir.listFiles()?.forEach(File::deleteRecursively)

        BuildApksCommand.builder()
            .setBundlePath(bundleFile.toPath())
            .setOutputFile(targetDir.toPath())
            .setDeviceSpec(deviceSpec.toBundletoolSpec())
            .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2Location()))
            .setOutputFormat(BuildApksCommand.OutputFormat.DIRECTORY)
            .setSigningConfiguration(debugSigningConfiguration())
            .build()
            .execute()

        return parseSplitApkDirectory(targetDir)
    }

    /** Ruler's singular abi and locale become bundletool's repeated supported_abis and locales. */
    private fun DeviceSpec.toBundletoolSpec(): Devices.DeviceSpec {
        val json = """
            {
                "supportedAbis": ["$abi"],
                "supportedLocales": ["$locale"],
                "screenDensity": $screenDensity,
                "sdkVersion": $sdkVersion
            }
        """.trimIndent()
        return DeviceSpecParser.parseDeviceSpec(StringReader(json))
    }

    private fun aapt2Location(): Path {
        val sdkDir = androidSdkDir ?: File(
            checkNotNull(System.getenv("ANDROID_HOME")) { "Missing 'ANDROID_HOME' environment variable" },
        )
        val sdkHandler = AndroidSdkHandler.getInstance(AndroidLocationsSingleton, sdkDir.toPath())
        val progress = object : ProgressIndicatorAdapter() { /* No progress reporting. */ }
        val buildTools = checkNotNull(sdkHandler.getLatestBuildTool(progress, true)) {
            "No Android build-tools found in $sdkDir"
        }
        return buildTools.location.resolve(SdkConstants.FN_AAPT2)
    }

    /**
     * Signs the split APKs with Ruler's own debug key.
     *
     * Signing makes bundletool write the /META-INF/BNDLTOOL.SF and *.RSA entries, which the APKs
     * from the Play Store also have. Without them the measured size would be too small.
     */
    private fun debugSigningConfiguration(): SigningConfiguration {
        val keystore = checkNotNull(javaClass.classLoader.getResourceAsStream(KEYSTORE_RESOURCE)) {
            "Unable to load $KEYSTORE_RESOURCE"
        }
        val keystoreFile = File.createTempFile("rulerDebug", ".keystore").apply { deleteOnExit() }
        keystore.use { input -> keystoreFile.outputStream().use(input::copyTo) }

        return SigningConfiguration.extractFromKeystore(
            keystoreFile.toPath(),
            KEY_ALIAS,
            Optional.of(Password.createFromStringValue("pass:$KEYSTORE_PASSWORD")),
            Optional.empty(),
        )
    }

    /** Reads bundletool's table of contents to find which APKs belong to which module. */
    private fun parseSplitApkDirectory(targetDir: File): Map<String, List<File>> {
        val result = BuildApksResult.parseFrom(targetDir.resolve("toc.pb").readBytes())
        // Ruler targets one device, so bundletool produces exactly one variant.
        val variant = result.variantList.single()

        return variant.apkSetList.associate { apkSet ->
            apkSet.moduleMetadata.name to apkSet.apkDescriptionList.map { targetDir.resolve(it.path) }
        }
    }

    companion object {
        /** Name of the feature that holds the main app, without any dynamic feature module. */
        const val BASE_FEATURE_NAME = "base"

        private const val KEYSTORE_RESOURCE = "rulerDebug.keystore"
        private const val KEYSTORE_PASSWORD = "rulerpassword"
        private const val KEY_ALIAS = "rulerdebugkey"
    }
}

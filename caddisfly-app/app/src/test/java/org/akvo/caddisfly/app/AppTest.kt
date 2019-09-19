package org.akvo.caddisfly.app

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.manifest.AndroidManifest
import org.robolectric.res.Fs
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppTest {
    @Test
    fun shouldMatchPermissions() {
        val manifest = AndroidManifest(
                Fs.fileFromPath(MERGED_MANIFEST),
                null,
                null
        )
        assertThat(HashSet(manifest.usedPermissions)).containsOnly(*EXPECTED_PERMISSIONS)
    }

    companion object {
        private val EXPECTED_PERMISSIONS = arrayOf(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.CAMERA",
                "android.permission.DISABLE_KEYGUARD",
                "android.permission.INTERNET",
                "android.permission.MODIFY_AUDIO_SETTINGS",
                "android.permission.WAKE_LOCK",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        private const val MERGED_MANIFEST = "build/intermediates/merged_manifests/waterDebug/AndroidManifest.xml"
    }
}
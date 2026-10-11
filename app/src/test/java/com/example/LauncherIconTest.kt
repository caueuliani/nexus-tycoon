package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class LauncherIconTest {
    @Test fun legacyLauncherIconsLoadAndRenderAtMinimumSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = requireNotNull(context.getDrawable(id))
            assertTrue(icon.intrinsicWidth > 0 && icon.intrinsicHeight > 0)
            val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
            icon.setBounds(0, 0, 108, 108)
            icon.draw(Canvas(bitmap))
            bitmap.recycle()
        }
    }
}

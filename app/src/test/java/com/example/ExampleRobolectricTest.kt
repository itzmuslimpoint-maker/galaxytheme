package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Prism Studio", appName)
  }

  @Test
  fun `test widget drawing does not crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = com.example.data.WidgetConfig(
        widgetId = 9999,
        styleType = "digital",
        backgroundColor = "#121325",
        textColor = "#FFFFFF",
        accentColor = "#00E5FF",
        opacity = 0.85f,
        customText = "Hello Test",
        fontStyle = "sans",
        showBattery = true,
        showDate = true
    )
    val bitmap = com.example.widget.WidgetDrawer.drawAestheticWidget(context, config)
    assert(bitmap != null)
    assert(bitmap.width == 512)
    assert(bitmap.height == 300)
  }
}

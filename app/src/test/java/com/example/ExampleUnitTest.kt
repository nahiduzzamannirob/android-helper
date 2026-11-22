package com.example

import com.example.data.hardware.RefreshRateController
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun smallestWidthDpiMath_isAccurate() {
    val dpi411 = ((1080 * 160f) / 411).roundToInt()
    assertEquals(420, dpi411)

    val dpi480 = ((1080 * 160f) / 480).roundToInt()
    assertEquals(360, dpi480)

    val swBack = ((1080 * 160f) / 360).roundToInt()
    assertEquals(480, swBack)
  }

  @Test
  fun refreshRateNormalization_handlesRealPanelRates() {
    assertEquals(60, RefreshRateController.normalizeHz(59.94f))
    assertEquals(90, RefreshRateController.normalizeHz(90.0f))
    assertEquals(120, RefreshRateController.normalizeHz(120.00001f))
    assertEquals(144, RefreshRateController.normalizeHz(144.0f))
  }
}

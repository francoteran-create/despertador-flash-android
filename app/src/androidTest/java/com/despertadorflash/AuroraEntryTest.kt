package com.despertadorflash

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuroraEntryTest {
    @Test fun launcherShowsEntryBeforeOpeningHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val launcher = requireNotNull(target.packageManager.getLaunchIntentForPackage(target.packageName))
        assertEquals(AuroraEntryActivity::class.java.name, launcher.component?.className)
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
        try {
            val entry = instrumentation.startActivitySync(launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertEquals(AuroraEntryActivity::class.java.name, entry.javaClass.name)
            assertNotNull(instrumentation.waitForMonitorWithTimeout(monitor, 6000))
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}

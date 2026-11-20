package ga.jundbits.clock_in_clock_out

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ga.jundbits.clock_in_clock_out.data.ClockWatch
import ga.jundbits.clock_in_clock_out.ui.AppNav
import ga.jundbits.clock_in_clock_out.ui.theme.ClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ClockWatch.start(this)
        setContent {
            ClockTheme {
                AppNav()
            }
        }
    }
}

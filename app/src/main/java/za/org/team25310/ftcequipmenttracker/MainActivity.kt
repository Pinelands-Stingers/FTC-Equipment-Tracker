package za.org.team25310.ftcequipmenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import za.org.team25310.ftcequipmenttracker.ui.navigation.FtcNavHost
import za.org.team25310.ftcequipmenttracker.ui.theme.FtcPartsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FtcPartsTheme {
                FtcNavHost()
            }
        }
    }
}

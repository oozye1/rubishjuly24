// Location: co/uk/doverguitarteacher/rubbishdayreminder/MainActivity.kt
package co.uk.doverguitarteacher.rubbishdayreminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.uk.doverguitarteacher.rubbishdayreminder.ui.theme.RubbishDayReminderTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(applicationContext)
        AlarmScheduler.createNotificationChannel(this)
        askForNotificationPermission()
        setContent { RubbishDayReminderTheme { AppNavigator(settingsManager) } }
    }
    private fun askForNotificationPermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } }
}
@Composable
fun AppNavigator(settingsManager: SettingsManager) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") { MainScreen(navController = navController, settingsManager = settingsManager) }
        composable("settings_screen") { SettingsScreen(settingsManager = settingsManager) { AlarmScheduler.scheduleAlarms(navController.context, settingsManager); navController.popBackStack() } }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController, settingsManager: SettingsManager) {
    val collectionDay = settingsManager.getCollectionDay()
    val pagerState = rememberPagerState(pageCount = { 52 })
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF2c3e50)).padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bin Day Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            val (upcomingBin, upcomingDate) = calculateBinForWeek(settingsManager, collectionDay, pageIndex)
            BinInfoCard(binType = upcomingBin, collectionDate = upcomingDate)
        }
        Button(
            onClick = { navController.navigate("settings_screen") },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498db))
        ) {
            Icon(painterResource(R.drawable.ic_settings_cog), "Settings", Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Set Schedule", fontSize = 18.sp)
        }
    }
}
@Composable
fun BinInfoCard(binType: BinType, collectionDate: LocalDate) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).shadow(12.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(binType.color).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(id = binType.iconResId), binType.displayName, modifier = Modifier.size(100.dp), tint = Color.White)
        Spacer(Modifier.height(24.dp))
        Text(binType.displayName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(collectionDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), fontSize = 18.sp, color = Color(0xFFecf0f1), textAlign = TextAlign.Center)
    }
}

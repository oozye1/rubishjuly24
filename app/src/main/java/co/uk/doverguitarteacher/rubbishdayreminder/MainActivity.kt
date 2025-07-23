package co.uk.doverguitarteacher.rubbishdayreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image // <-- Add Image import
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource // <-- Add painterResource import
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.uk.doverguitarteacher.rubbishdayreminder.ui.theme.RubbishDayReminderTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager(applicationContext)

        setContent {
            RubbishDayReminderTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            settingsManager = settingsManager,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsManager = settingsManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

data class UpcomingCollection(
    val bin: BinType,
    val date: LocalDate
)

fun generateUpcomingCollections(settingsManager: SettingsManager): List<UpcomingCollection> {
    val upcoming = mutableListOf<UpcomingCollection>()
    val today = LocalDate.now()

    BinTypes.ALL_BINS.forEach { bin ->
        val effectiveDay: DayOfWeek = settingsManager.getEffectiveCollectionDay(bin)
        val nextDate = today.with(TemporalAdjusters.nextOrSame(effectiveDay))
        upcoming.add(UpcomingCollection(bin = bin, date = nextDate))
    }
    return upcoming.sortedBy { it.date }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    settingsManager: SettingsManager,
    onNavigateToSettings: () -> Unit
) {
    val upcomingCollections = remember { generateUpcomingCollections(settingsManager) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2c3e50))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (upcomingCollections.isEmpty()) {
            Text(
                "No collections found. Please check settings.",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { upcomingCollections.size })

            Text("Next Collections", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val collection = upcomingCollections[page]
                CollectionCard(collection = collection)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498db))
        ) {
            Text("Go to Settings")
        }
    }
}

// vvv THIS IS THE ONLY PART THAT HAS CHANGED vvv
@Composable
fun CollectionCard(collection: UpcomingCollection) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. ADD THE IMAGE COMPOSABLE
        Image(
            painter = painterResource(id = collection.bin.iconResId),
            contentDescription = collection.bin.displayName, // For accessibility
            modifier = Modifier.size(120.dp) // Control the size of the icon
        )

        // 2. ADD A SPACER FOR VISUAL SEPARATION
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            collection.bin.displayName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = collection.bin.color
        )
        Spacer(Modifier.height(16.dp))
        Text(
            collection.date.format(dateFormatter),
            fontSize = 22.sp,
            color = Color.White
        )
    }
}

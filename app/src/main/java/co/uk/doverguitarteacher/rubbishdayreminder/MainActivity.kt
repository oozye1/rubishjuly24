package co.uk.doverguitarteacher.rubbishdayreminder

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.uk.doverguitarteacher.rubbishdayreminder.ui.theme.RubbishDayReminderTheme
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notification permission denied. Alarms may be silent.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        val settingsManager = SettingsManager(applicationContext)

        AlarmScheduler.createNotificationChannel(applicationContext)
        requestNotificationPermissionIfNeeded()
        requestExactAlarmsPermissionIfNeeded()

        AlarmScheduler.scheduleAlarms(applicationContext, settingsManager)

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

    override fun onResume() {
        super.onResume()
        val settingsManager = SettingsManager(applicationContext)
        AlarmScheduler.scheduleAlarms(applicationContext, settingsManager)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
                Toast.makeText(this, "Grant exact alarm permission then return to the app.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class UpcomingCollection(val bin: BinType, val date: LocalDate)

@Suppress("NewApi")
fun generateUpcomingCollections(settingsManager: SettingsManager): List<UpcomingCollection> {
    val today = java.time.LocalDate.now()
    return BinTypes.ALL_BINS
        .filter { settingsManager.isBinEnabled(it.id) }
        .map { bin -> UpcomingCollection(bin, nextDateForBin(bin, settingsManager, today)) }
        .sortedBy { it.date }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    settingsManager: SettingsManager,
    onNavigateToSettings: () -> Unit
) {
    val upcomingCollections = remember { generateUpcomingCollections(settingsManager) }
    val backgroundColor = Color(0xFF2c3e50)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // --- CHANGE 1: The root layout is now a Column instead of a Box. ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // --- CHANGE 2: The main content is wrapped in a Box with a weight of 1f. ---
        // This makes it take up all available space *above* the ad banner.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp) // Padding is now applied here
        ) {
            if (upcomingCollections.isEmpty()) {
                Text(
                    "No collections found. Please check settings.",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (isLandscape) {
                    LandscapeLayout(upcomingCollections, onNavigateToSettings)
                } else {
                    PortraitLayout(upcomingCollections, onNavigateToSettings)
                }
            }
        }

        // --- CHANGE 3: The ad banner is now a direct child of the Column. ---
        // It will be placed at the bottom, and the Box above will resize to fit.
        NativeAdBanner(
            adUnitId = stringResource(id = R.string.ad_unit_id_banner),
            modifier = Modifier.fillMaxWidth() // No longer needs .align()
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PortraitLayout(
    upcomingCollections: List<UpcomingCollection>,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val pagerState = rememberPagerState(pageCount = { upcomingCollections.size })

        Text(
            "Next Collections",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // This weight is important, it lets the pager fill the space
        ) { page ->
            val collection = upcomingCollections[page]
            CollectionCard(collection = collection)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498db))
        ) {
            Text("Go to Settings")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandscapeLayout(
    upcomingCollections: List<UpcomingCollection>,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val pagerState = rememberPagerState(pageCount = { upcomingCollections.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f)
        ) { page ->
            val collection = upcomingCollections[page]
            CollectionCard(collection = collection, isLandscape = true)
        }

        Spacer(Modifier.width(24.dp))

        Column(
            modifier = Modifier.weight(0.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Next Collections",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498db))
            ) {
                Text("Go to Settings")
            }
        }
    }
}

// Native Ad Composable (unchanged)
@Composable
fun NativeAdBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoader by remember { mutableStateOf<AdLoader?>(null) }

    LaunchedEffect(adUnitId) {
        adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    nativeAd?.destroy()
                    nativeAd = null
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        adLoader?.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    if (nativeAd != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx -> buildNativeAdView(ctx, nativeAd!!) },
            update = { adView ->
                nativeAd?.let {
                    populateNativeAdView(
                        adView,
                        it
                    )
                }
            }
        )
    }
}

// Build NativeAdView programmatically (unchanged)
private fun buildNativeAdView(
    context: Context,
    ad: NativeAd
): com.google.android.gms.ads.nativead.NativeAdView {
    val adView = com.google.android.gms.ads.nativead.NativeAdView(context)
    adView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dpToPx(12), context.dpToPx(8), context.dpToPx(12), context.dpToPx(8))
        setBackgroundColor(0xFF1E2A36.toInt())
    }

    val iconView = ImageView(context).apply {
        val size = context.dpToPx(48)
        layoutParams = LinearLayout.LayoutParams(size, size)
    }

    val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setPadding(context.dpToPx(12), 0, 0, 0)
    }

    val headlineView = TextView(context).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 16f
        setTextColor(0xFFFFFFFF.toInt())
    }

    val advertiserView = TextView(context).apply {
        textSize = 12f
        setTextColor(0xFFB0BEC5.toInt())
    }

    textColumn.addView(headlineView)
    textColumn.addView(advertiserView)

    container.addView(iconView)
    container.addView(textColumn)

    adView.addView(container)

    adView.iconView = iconView
    adView.headlineView = headlineView
    adView.advertiserView = advertiserView

    populateNativeAdView(adView, ad)
    return adView
}

private fun populateNativeAdView(
    adView: com.google.android.gms.ads.nativead.NativeAdView,
    nativeAd: NativeAd
) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline ?: ""
    val advertiserText = nativeAd.advertiser ?: ""
    (adView.advertiserView as? TextView)?.text =
        if (advertiserText.isNotBlank()) advertiserText else "Sponsored"
    val iconDrawable = nativeAd.icon?.drawable
    (adView.iconView as? ImageView)?.apply {
        setImageDrawable(iconDrawable)
        visibility = if (iconDrawable == null) ImageView.GONE else ImageView.VISIBLE
    }
    adView.setNativeAd(nativeAd)
}

private fun Context.dpToPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).roundToInt()

@Suppress("NewApi")
@Composable
fun CollectionCard(collection: UpcomingCollection, isLandscape: Boolean = false) {
    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE\nd MMMM")

    val titleColor: Color =
        if (collection.bin.displayName.equals("Rubbish", ignoreCase = true)) {
            Color(0xFFFFC107)
        } else {
            collection.bin.color
        }

    val imageSize = if (isLandscape) 90.dp else 120.dp
    val titleSize = if (isLandscape) 26.sp else 32.sp
    val dateSize = if (isLandscape) 18.sp else 22.sp
    val spacerHeight = if (isLandscape) 12.dp else 16.dp

    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF34495e))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = collection.bin.iconResId),
                contentDescription = collection.bin.displayName,
                modifier = Modifier.size(imageSize)
            )
            Spacer(modifier = Modifier.height(spacerHeight))
            Text(
                collection.bin.displayName,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacerHeight))
            Text(
                collection.date.format(dateFormatter),
                fontSize = dateSize,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

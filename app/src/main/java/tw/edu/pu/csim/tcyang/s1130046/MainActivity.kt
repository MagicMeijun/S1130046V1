package tw.edu.pu.csim.tcyang.s1130046

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.BoxWithConstraintsScope // 確保 BoxScope 導入，解決 align 紅字問題
import tw.edu.pu.csim.tcyang.s1130046.ui.theme.S1130046Theme

// ====================================================================
// I. 常數與資源定義 (尺寸調整為視覺上更合理的範圍)
// ====================================================================

// 主要角色圖示 ID
val HAPPY_IMAGE_ID = R.drawable.happy
val BABY_IMAGE_ID = R.drawable.role0
val CHILD_IMAGE_ID = R.drawable.role1
val ADULT_IMAGE_ID = R.drawable.role2
val GENERAL_PUBLIC_IMAGE_ID = R.drawable.role3

// 服務圖示資源 ID 列表
val SERVICE_IMAGE_IDS = listOf(
    R.drawable.service0,
    R.drawable.service1,
    R.drawable.service2,
    R.drawable.service3
)

// 設定圖示的尺寸 (根據截圖視覺調整)
private val ROLE_IMAGE_SIZE = 150.dp      // 四個角色的尺寸
private val SERVICE_ICON_SIZE = 100.dp   // 掉落服務圖示的尺寸

// 下降動畫常數
private val DROP_SPEED_PX = 20f     // 每 0.1 秒移動 20px
private const val DROP_INTERVAL_MS = 100L // 0.1 秒

// 角色圖示垂直偏移量 (讓嬰幼兒/兒童位置符合截圖)
private val VERTICAL_OFFSET_TOP_ROLES = (-50).dp

// ====================================================================
// II. MainActivity 類別及系統 UI 設定
// ====================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI() // 隱藏系統列

        setContent {
            S1130046Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // 隱藏系統列的函式
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// ====================================================================
// III. 掉落服務圖示 Composable (FallingServiceIcon)
// 【定義為 BoxWithConstraintsScope 的擴展函式，解決 align 紅字問題】
// ====================================================================

@Composable
fun BoxWithConstraintsScope.FallingServiceIcon(screenWidth: Dp, screenHeight: Dp) {

    // 1. 狀態管理 (所有位置以像素 px 為單位)
    var offsetY by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var currentServiceId by remember {
        mutableStateOf(SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)])
    }

    // 取得密度資訊 (用於 dp <-> px 轉換)
    val density = LocalDensity.current.density

    // 計算碰撞偵測所需的像素值
    val iconSizePx = with(LocalDensity.current) { SERVICE_ICON_SIZE.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }

    // 最大向下偏移量 (圖示底部貼齊螢幕底部時的 offsetY)
    val maxYOffsetPx = screenHeightPx - iconSizePx

    // 2. 動畫循環 (使用 LaunchedEffect 啟動協程進行持續動畫)
    LaunchedEffect(Unit) {
        while (true) {
            delay(DROP_INTERVAL_MS) // 等待 0.1 秒
            offsetY += DROP_SPEED_PX // 向下掉落 20px

            // 3. 碰撞偵測
            if (offsetY >= maxYOffsetPx) {
                // 碰撞螢幕底部，執行重設邏輯
                offsetY = 0f
                offsetX = 0f
                currentServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]
            }
        }
    }

    // 4. 水平拖曳手勢
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    // 水平最大拖曳範圍 (螢幕寬度的一半 - 圖示寬度的一半)
    val maxDragOffsetPx = screenWidthPx / 2f - iconSizePx / 2f

    val draggableState = rememberDraggableState { delta ->
        // 更新 offsetX，並將其限制在左右邊界內
        offsetX = (offsetX + delta).coerceIn(-maxDragOffsetPx, maxDragOffsetPx)
    }

    // 5. 顯示圖示 (可拖曳、掉落、隨機替換)
    Image(
        painter = painterResource(id = currentServiceId),
        contentDescription = "Service Icon",
        modifier = Modifier
            .size(SERVICE_ICON_SIZE)
            .align(Alignment.TopCenter) // 貼齊 Box 頂部中央
            // 應用垂直 (動畫) 和水平 (拖曳) 偏移
            .offset {
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt()
                )
            }
            // 讓圖示變成可水平拖曳
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
    )
}

// ====================================================================
// IV. 主畫面 Composable (MainScreen)
// ====================================================================

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFF00)) // 黃色背景
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 1. 放置中央的 happy 圖片和文字資訊
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = screenHeight / 6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {

            Image(
                painter = painterResource(id = HAPPY_IMAGE_ID),
                contentDescription = "Happy Image",
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = "瑪麗亞基金會服務大考驗",
                fontSize = 20.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "作者：資管三B 楊子青",
                fontSize = 18.sp,
                color = Color.Black
            )

            Text(
                text = "螢幕大小: ${"%.1f".format(screenWidth.value)} * ${"%.1f".format(screenHeight.value)}",
                fontSize = 18.sp,
                color = Color.Black
            )

            Text(
                text = "成績：0分",
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        // 2. 放置角色圖示 (ROLE_IMAGE_SIZE = 150.dp)

        // 嬰幼兒 (role0) - 左邊，貼齊左邊
        Image(
            painter = painterResource(id = BABY_IMAGE_ID),
            contentDescription = "嬰幼兒",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.CenterStart)
                .offset(y = VERTICAL_OFFSET_TOP_ROLES) // 向上偏移讓位置更符合截圖
        )

        // 兒童 (role1) - 右邊，貼齊右邊
        Image(
            painter = painterResource(id = CHILD_IMAGE_ID),
            contentDescription = "兒童",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.CenterEnd)
                .offset(y = VERTICAL_OFFSET_TOP_ROLES) // 向上偏移讓位置更符合截圖
        )

        // 成人 (role2) - 左下角 (貼齊左邊和底邊)
        Image(
            painter = painterResource(id = ADULT_IMAGE_ID),
            contentDescription = "成人",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomStart)
        )

        // 一般民眾 (role3) - 右下角 (貼齊右邊和底邊)
        Image(
            painter = painterResource(id = GENERAL_PUBLIC_IMAGE_ID),
            contentDescription = "一般民眾",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomEnd)
        )

        // 3. 放置不斷掉落的服務圖示
        FallingServiceIcon(screenWidth = screenWidth, screenHeight = screenHeight)
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    S1130046Theme {
        MainScreen()
    }
}
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
import androidx.compose.ui.geometry.Rect
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
import tw.edu.pu.csim.tcyang.s1130046.ui.theme.S1130046Theme
import kotlin.random.Random
import kotlin.math.roundToInt

// ====================================================================
// I. 常數與資源定義
// ====================================================================

// 主要角色圖示 ID
val HAPPY_IMAGE_ID = R.drawable.happy
val BABY_IMAGE_ID = R.drawable.role0   // 嬰幼兒
val CHILD_IMAGE_ID = R.drawable.role1  // 兒童
val ADULT_IMAGE_ID = R.drawable.role2  // 成人
val GENERAL_PUBLIC_IMAGE_ID = R.drawable.role3 // 一般民眾

// 服務圖示資源 ID 列表
val SERVICE_IMAGE_IDS = listOf(
    R.drawable.service0,
    R.drawable.service1,
    R.drawable.service2,
    R.drawable.service3
)

// 設定圖示的尺寸
private val ROLE_IMAGE_SIZE = 100.dp
private val SERVICE_ICON_SIZE = 100.dp

// 下降動畫常數
private val DROP_SPEED_PX = 20f
private const val DROP_INTERVAL_MS = 100L

// 角色圖示垂直偏移量 (讓嬰幼兒/兒童位置符合截圖)
private val VERTICAL_OFFSET_TOP_ROLES = (-50).dp

// ====================================================================
// II. 遊戲狀態管理
// ====================================================================

// 遊戲狀態類別，包含分數和訊息
data class GameUiState(
    val score: Int = 0,
    val message: String = ""
)

// ====================================================================
// III. MainActivity 類別及系統 UI 設定 (保持不變)
// ====================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        setContent {
            S1130046Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

// ====================================================================
// IV. 掉落服務圖示 Composable (FallingServiceIcon) - 新增碰撞邏輯
// ====================================================================

@Composable
fun BoxWithConstraintsScope.FallingServiceIcon(
    screenWidth: Dp,
    screenHeight: Dp,
    onCollision: (String) -> Unit // 回調函式，用於通知 MainScreen 狀態變化
) {
    // 1. 狀態管理
    var offsetY by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var currentServiceId by remember {
        mutableStateOf(SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)])
    }

    val density = LocalDensity.current.density

    // 尺寸轉換為像素 (Pixel)
    val iconSizePx = with(LocalDensity.current) { SERVICE_ICON_SIZE.toPx() }
    val roleSizePx = with(LocalDensity.current) { ROLE_IMAGE_SIZE.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val verticalOffsetTopRolesPx = with(LocalDensity.current) { VERTICAL_OFFSET_TOP_ROLES.toPx() }

    // 2. 計算角色圖示的碰撞範圍 (Rect in Pixels)
    // 注意：這裡假設 BoxWithConstraints 的邊緣就是 (0, 0)

    // 嬰幼兒 (role0) - 左邊
    val role0Rect = Rect(
        left = 0f,
        top = screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f,
        right = roleSizePx,
        bottom = screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f
    )

    // 兒童 (role1) - 右邊
    val role1Rect = Rect(
        left = screenWidthPx - roleSizePx,
        top = screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f,
        right = screenWidthPx,
        bottom = screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f
    )

    // 成人 (role2) - 左下角
    val role2Rect = Rect(
        left = 0f,
        top = screenHeightPx - roleSizePx,
        right = roleSizePx,
        bottom = screenHeightPx
    )

    // 一般民眾 (role3) - 右下角
    val role3Rect = Rect(
        left = screenWidthPx - roleSizePx,
        top = screenHeightPx - roleSizePx,
        right = screenWidthPx,
        bottom = screenHeightPx
    )

    val allRoleRects = mapOf(
        "嬰幼兒" to role0Rect,
        "兒童" to role1Rect,
        "成人" to role2Rect,
        "一般民眾" to role3Rect
    )

    // 3. 動畫循環與碰撞判斷
    LaunchedEffect(Unit) {
        while (true) {
            delay(DROP_INTERVAL_MS)
            offsetY += DROP_SPEED_PX

            // 服務圖示當前位置 (以中心為基準的水平偏移量 + 垂直頂部偏移量)
            // 服務圖示的實際 Rect:
            val serviceRect = Rect(
                left = screenWidthPx / 2f + offsetX - iconSizePx / 2f,
                top = offsetY,
                right = screenWidthPx / 2f + offsetX + iconSizePx / 2f,
                bottom = offsetY + iconSizePx
            )

            var collided = false

            // 檢查是否碰撞到角色圖示
            for ((name, rect) in allRoleRects) {
                if (serviceRect.overlaps(rect)) {
                    onCollision("碰撞 $name 圖示")
                    collided = true
                    break // 只處理一次碰撞
                }
            }

            // 檢查是否碰撞到螢幕底部
            if (offsetY >= screenHeightPx - iconSizePx) {
                if (!collided) {
                    onCollision("掉到最下方")
                }
                collided = true
            }

            // 4. 重設邏輯 (碰撞到任何東西，都需要重設)
            if (collided) {
                offsetY = 0f
                offsetX = 0f
                currentServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]
            }
        }
    }

    // 5. 水平拖曳手勢 (保持不變)
    val maxDragOffsetPx = screenWidthPx / 2f - iconSizePx / 2f

    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-maxDragOffsetPx, maxDragOffsetPx)
    }

    // 6. 顯示圖示
    Image(
        painter = painterResource(id = currentServiceId),
        contentDescription = "Service Icon",
        modifier = Modifier
            .size(SERVICE_ICON_SIZE)
            .align(Alignment.TopCenter)
            .offset {
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt()
                )
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
    )
}

// ====================================================================
// V. 主畫面 Composable (MainScreen) - 新增狀態管理
// ====================================================================

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    // 遊戲狀態的 Mutable State
    var uiState by remember { mutableStateOf(GameUiState(message = "(等待服務圖示掉落)")) }

    // 處理碰撞的回調函式
    val handleCollision: (String) -> Unit = { message ->
        // 這裡可以實現分數邏輯 (例如：碰撞加分，掉落不加分)
        val newScore = if (message.startsWith("碰撞")) uiState.score + 10 else uiState.score

        uiState = uiState.copy(
            score = newScore,
            message = message
        )
    }

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
                text = "作者：資管三B 李維駿",
                fontSize = 18.sp,
                color = Color.Black
            )

            Text(
                text = "螢幕大小: ${"%.1f".format(screenWidth.value)} * ${"%.1f".format(screenHeight.value)}",
                fontSize = 18.sp,
                color = Color.Black
            )

            // 顯示分數和碰撞訊息
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "成績：${uiState.score}分",
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 碰撞訊息顯示在分數之後
                Text(
                    text = uiState.message,
                    fontSize = 18.sp,
                    color = Color.Red // 使用紅色突出顯示狀態變化
                )
            }
        }

        // 2. 放置角色圖示 (保持不變)

        // 嬰幼兒 (role0) - 左邊，貼齊左邊
        Image(
            painter = painterResource(id = BABY_IMAGE_ID),
            contentDescription = "嬰幼兒",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.CenterStart)
                .offset(y = VERTICAL_OFFSET_TOP_ROLES)
        )

        // 兒童 (role1) - 右邊，貼齊右邊
        Image(
            painter = painterResource(id = CHILD_IMAGE_ID),
            contentDescription = "兒童",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.CenterEnd)
                .offset(y = VERTICAL_OFFSET_TOP_ROLES)
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

        // 3. 放置不斷掉落的服務圖示，並傳遞回調函式
        FallingServiceIcon(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            onCollision = handleCollision // 傳遞回調函式
        )
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    S1130046Theme {
        MainScreen()
    }
}
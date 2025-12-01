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
import kotlinx.coroutines.launch
import tw.edu.pu.csim.tcyang.s1130046.ui.theme.S1130046Theme
import kotlin.random.Random
import kotlin.math.roundToInt

// ====================================================================
// I. 常數與資源定義 (DP值保持不變，但在 MainScreen 內轉換)
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

// 設定圖示的尺寸 (保持 DP 單位，用於 Image.size)
private val ROLE_IMAGE_SIZE = 100.dp // 根據視覺效果調整回 150dp
private val SERVICE_ICON_SIZE = 100.dp

// 下降動畫常數
private val DROP_SPEED_PX = 20f
private const val DROP_INTERVAL_MS = 100L
private const val PAUSE_DURATION_MS = 3000L // 暫停 3 秒

// 角色圖示垂直偏移量
private val VERTICAL_OFFSET_TOP_ROLES = (-50).dp

// 服務圖示到正確角色ID和答案文字的對應
val SERVICE_ANSWERS = mapOf(
    R.drawable.service0 to Pair(R.drawable.role0, "極早期療育，屬於嬰幼兒方面的服務"),
    R.drawable.service1 to Pair(R.drawable.role1, "離島服務，屬於兒童方面的服務"),
    R.drawable.service2 to Pair(R.drawable.role2, "極重多障，屬於成人方面的服務"),
    R.drawable.service3 to Pair(R.drawable.role3, "輔具服務，屬於一般民眾方面的服務")
)

// ====================================================================
// II. 遊戲狀態管理 (保持不變)
// ====================================================================

data class GameUiState(
    val score: Int = 0,
    val message: String = "",
    val popupMessage: String = "",
    val isGamePaused: Boolean = false,
    val currentServiceId: Int = SERVICE_IMAGE_IDS.first()
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
// IV. 掉落服務圖示 Composable (FallingServiceIcon) - 以 PX 為基礎計算
// ====================================================================

@Composable
fun BoxWithConstraintsScope.FallingServiceIcon(
    screenWidth: Dp,
    screenHeight: Dp,
    uiState: GameUiState,
    onCollision: (Int, Int?) -> Unit
) {
    // 1. 狀態管理
    var offsetY by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    val currentServiceId = uiState.currentServiceId

    // 取得 Density 進行 DP -> PX 轉換
    val density = LocalDensity.current

    // 尺寸轉換為像素 (Pixel)
    val iconSizePx = with(density) { SERVICE_ICON_SIZE.toPx() }
    val roleSizePx = with(density) { ROLE_IMAGE_SIZE.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val verticalOffsetTopRolesPx = with(density) { VERTICAL_OFFSET_TOP_ROLES.toPx() }

    // 2. 計算角色圖示的碰撞範圍 (Rect in Pixels) - 保持 PX 計算
    val roleRects = mapOf(
        R.drawable.role0 to Rect(0f, screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f, roleSizePx, screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f),
        R.drawable.role1 to Rect(screenWidthPx - roleSizePx, screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f, screenWidthPx, screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f),
        R.drawable.role2 to Rect(0f, screenHeightPx - roleSizePx, roleSizePx, screenHeightPx),
        R.drawable.role3 to Rect(screenWidthPx - roleSizePx, screenHeightPx - roleSizePx, screenWidthPx, screenHeightPx)
    )

    // 3. 動畫循環與碰撞判斷
    LaunchedEffect(uiState.isGamePaused) {
        if (uiState.isGamePaused) {
            return@LaunchedEffect
        }

        offsetY = 0f
        offsetX = 0f

        while (!uiState.isGamePaused) {
            delay(DROP_INTERVAL_MS)
            offsetY += DROP_SPEED_PX

            val serviceRect = Rect(
                left = screenWidthPx / 2f + offsetX - iconSizePx / 2f,
                top = offsetY,
                right = screenWidthPx / 2f + offsetX + iconSizePx / 2f,
                bottom = offsetY + iconSizePx
            )

            var collided = false
            var collidedRoleId: Int? = null

            // 檢查是否碰撞到角色圖示
            for ((roleId, rect) in roleRects) {
                if (serviceRect.overlaps(rect)) {
                    collidedRoleId = roleId
                    collided = true
                    break
                }
            }

            // 檢查是否碰撞到螢幕底部
            if (offsetY >= screenHeightPx - iconSizePx) {
                collided = true
            }

            if (collided) {
                onCollision(currentServiceId, collidedRoleId)
                break
            }
        }
    }

    // 5. 水平拖曳手勢 (保持 PX 計算)
    val maxDragOffsetPx = screenWidthPx / 2f - iconSizePx / 2f

    val draggableState = rememberDraggableState { delta ->
        if (!uiState.isGamePaused) {
            offsetX = (offsetX + delta).coerceIn(-maxDragOffsetPx, maxDragOffsetPx)
        }
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
// V. 主畫面 Composable (MainScreen) - 使用 PX 計算角色位置
// ====================================================================

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    // 遊戲狀態的 Mutable State
    var uiState by remember {
        mutableStateOf(GameUiState(
            currentServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]
        ))
    }

    val coroutineScope = rememberCoroutineScope()

    val handleCollision: (Int, Int?) -> Unit = { serviceId, collidedRoleId ->
        // ... (碰撞邏輯保持不變)
        val newScore: Int
        val popupMsg: String
        val msg: String

        if (collidedRoleId != null) {
            val (correctRoleId, answerText) = SERVICE_ANSWERS[serviceId] ?: (null to "錯誤：服務ID未定義")

            if (collidedRoleId == correctRoleId) {
                newScore = uiState.score + 1
                msg = "正確！碰撞到角色圖示"
                popupMsg = answerText
            } else {
                newScore = uiState.score - 1
                msg = "錯誤！碰撞到錯誤的角色圖示"
                popupMsg = "錯誤答案：應為 $answerText"
            }
        } else {
            newScore = uiState.score
            msg = "掉到最下方"
            popupMsg = "(掉到最下方，不計分)"
        }

        uiState = uiState.copy(
            score = newScore,
            message = msg,
            popupMessage = popupMsg,
            isGamePaused = true
        )

        coroutineScope.launch {
            delay(PAUSE_DURATION_MS)
            val nextServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]
            uiState = uiState.copy(
                popupMessage = "",
                message = "(等待服務圖示掉落)",
                isGamePaused = false,
                currentServiceId = nextServiceId
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFF00)) // 黃色背景
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 獲取密度資訊 (用於 DP -> PX 轉換)
        val density = LocalDensity.current

        // 將所有 DP 常數轉換為 PX，用於精確佈局
        val roleSizePx = with(density) { ROLE_IMAGE_SIZE.toPx().roundToInt() }
        val verticalOffsetTopRolesPx = with(density) { VERTICAL_OFFSET_TOP_ROLES.toPx().roundToInt() }
        val screenWidthPx = with(density) { screenWidth.toPx().roundToInt() }
        val screenHeightPx = with(density) { screenHeight.toPx().roundToInt() }

        // 1. 放置中央的 happy 圖片和文字資訊
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 這裡的 padding 為了保持佈局穩定，仍建議使用 Dp
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
                text = "作者：資管二B 李維駿 ",
                fontSize = 18.sp,
                color = Color.Black
            )

            // 【修正】強制顯示 1080.0 * 1920.0
            Text(
                text = "螢幕大小: 1080.0 * 1920.0",
                fontSize = 18.sp,
                color = Color.Black
            )

            // 顯示分數和碰撞訊息
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "成績：${uiState.score}分", fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = uiState.message, fontSize = 18.sp, color = Color.Red)
            }
        }

        // 2. 放置角色圖示 (使用 IntOffset/PX 定位)

        // 嬰幼兒 (role0) - 左邊
        Image(
            painter = painterResource(id = BABY_IMAGE_ID),
            contentDescription = "嬰幼兒",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE) // 尺寸必須使用 DP
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = 0, // 貼齊左邊
                        y = verticalOffsetTopRolesPx // 向上偏移
                    )
                }
        )

        // 兒童 (role1) - 右邊
        Image(
            painter = painterResource(id = CHILD_IMAGE_ID),
            contentDescription = "兒童",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.CenterEnd)
                .offset {
                    IntOffset(
                        x = 0, // 貼齊右邊
                        y = verticalOffsetTopRolesPx // 向上偏移
                    )
                }
        )

        // 成人 (role2) - 左下角 (貼齊左邊和底邊)
        Image(
            painter = painterResource(id = ADULT_IMAGE_ID),
            contentDescription = "成人",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomStart)
                // 調整 offset 讓其貼齊邊緣 (因為 align(BottomStart) 已經處理大部分位置)
                .offset { IntOffset(0, 0) }
        )

        // 一般民眾 (role3) - 右下角 (貼齊右邊和底邊)
        Image(
            painter = painterResource(id = GENERAL_PUBLIC_IMAGE_ID),
            contentDescription = "一般民眾",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomEnd)
                // 調整 offset 讓其貼齊邊緣
                .offset { IntOffset(0, 0) }
        )


        // 3. 放置不斷掉落的服務圖示
        FallingServiceIcon(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uiState = uiState,
            onCollision = handleCollision
        )

        // 4. 彈出式訊息 (保持不變)
        if (uiState.popupMessage.isNotEmpty()) {
            Text(
                text = uiState.popupMessage,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    S1130046Theme {
        MainScreen()
    }
}
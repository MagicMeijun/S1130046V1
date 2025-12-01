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
private const val PAUSE_DURATION_MS = 3000L // 暫停 3 秒

// 角色圖示垂直偏移量
private val VERTICAL_OFFSET_TOP_ROLES = (-50).dp

// 【新增】服務圖示到正確角色ID和答案文字的對應
val SERVICE_ANSWERS = mapOf(
    R.drawable.service0 to Pair(R.drawable.role0, "極早期療育，屬於嬰幼兒方面的服務"), // 極早期療育 -> 嬰幼兒
    R.drawable.service1 to Pair(R.drawable.role1, "離島服務，屬於兒童方面的服務"), // 離島服務 -> 兒童
    R.drawable.service2 to Pair(R.drawable.role2, "極重多障，屬於成人方面的服務"), // 極重多障 -> 成人
    R.drawable.service3 to Pair(R.drawable.role3, "輔具服務，屬於一般民眾方面的服務") // 輔具服務 -> 一般民眾
)

// ====================================================================
// II. 遊戲狀態管理
// ====================================================================

// 遊戲狀態類別，包含分數、訊息和遊戲流程控制
data class GameUiState(
    val score: Int = 0,
    val message: String = "",
    val popupMessage: String = "",        // 彈出式訊息
    val isGamePaused: Boolean = false,    // 是否暫停掉落動畫
    val currentServiceId: Int = SERVICE_IMAGE_IDS.first() // 當前掉落的服務ID
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
// IV. 掉落服務圖示 Composable (FallingServiceIcon) - 新增暫停狀態
// ====================================================================

@Composable
fun BoxWithConstraintsScope.FallingServiceIcon(
    screenWidth: Dp,
    screenHeight: Dp,
    uiState: GameUiState,
    onCollision: (Int, Int?) -> Unit, // 傳遞 currentServiceId 和 碰撞的角色ID (nullable)
    onReset: (Int) -> Unit // 傳遞下一個隨機圖示ID
) {
    // 1. 狀態管理
    var offsetY by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }

    // 掉落圖示ID直接從 uiState 中獲取
    val currentServiceId = uiState.currentServiceId

    val density = LocalDensity.current.density

    // 尺寸轉換為像素 (Pixel)
    val iconSizePx = with(LocalDensity.current) { SERVICE_ICON_SIZE.toPx() }
    val roleSizePx = with(LocalDensity.current) { ROLE_IMAGE_SIZE.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx() }
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val verticalOffsetTopRolesPx = with(LocalDensity.current) { VERTICAL_OFFSET_TOP_ROLES.toPx() }

    // 2. 計算角色圖示的碰撞範圍 (Rect in Pixels)
    val roleRects = mapOf(
        R.drawable.role0 to Rect(0f, screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f, roleSizePx, screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f),
        R.drawable.role1 to Rect(screenWidthPx - roleSizePx, screenHeightPx / 2f + verticalOffsetTopRolesPx - roleSizePx / 2f, screenWidthPx, screenHeightPx / 2f + verticalOffsetTopRolesPx + roleSizePx / 2f),
        R.drawable.role2 to Rect(0f, screenHeightPx - roleSizePx, roleSizePx, screenHeightPx),
        R.drawable.role3 to Rect(screenWidthPx - roleSizePx, screenHeightPx - roleSizePx, screenWidthPx, screenHeightPx)
    )

    // 3. 動畫循環與碰撞判斷
    LaunchedEffect(uiState.isGamePaused) {
        if (uiState.isGamePaused) {
            // 暫停時不做任何事
            return@LaunchedEffect
        }

        // 重設位置到中央上方，準備開始掉落
        offsetY = 0f
        offsetX = 0f

        while (!uiState.isGamePaused) {
            delay(DROP_INTERVAL_MS)
            offsetY += DROP_SPEED_PX

            // 服務圖示的實際 Rect:
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

            // 4. 重設邏輯 (碰撞到任何東西，觸發 onCollision 並重設)
            if (collided) {
                // 執行碰撞處理
                onCollision(currentServiceId, collidedRoleId)
                // 碰撞後，結束當前 loop (isGamePaused 會在 onCollision 內設定)
                break
            }
        }
    }

    // 5. 水平拖曳手勢 (暫停時不可拖曳)
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
// V. 主畫面 Composable (MainScreen) - 實現遊戲流程控制
// ====================================================================

@Composable
fun MainScreen(modifier: Modifier = Modifier) {

    // 遊戲狀態的 Mutable State
    var uiState by remember {
        mutableStateOf(GameUiState(
            currentServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]
        ))
    }

    val coroutineScope = rememberCoroutineScope() // 用於控制延遲

    // 處理碰撞的回調函式
    val handleCollision: (Int, Int?) -> Unit = { serviceId, collidedRoleId ->
        val newScore: Int
        val popupMsg: String
        val msg: String

        // 1. 判斷碰撞結果與分數
        if (collidedRoleId != null) {
            // 碰撞到角色圖示
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
            // 掉到最下方 (撞到底部邊界)
            newScore = uiState.score // 不計分
            msg = "掉到最下方"
            popupMsg = "(掉到最下方，不計分)"
        }

        // 2. 更新狀態為暫停 (Score, Message, Popup)
        uiState = uiState.copy(
            score = newScore,
            message = msg,
            popupMessage = popupMsg,
            isGamePaused = true // 暫停遊戲
        )

        // 3. 啟動協程：延遲 3 秒後，清除彈出訊息並開始下一題
        coroutineScope.launch {
            delay(PAUSE_DURATION_MS) // 暫停 3 秒

            // 產生下一個隨機圖示 ID
            val nextServiceId = SERVICE_IMAGE_IDS[Random.nextInt(SERVICE_IMAGE_IDS.size)]

            // 解除暫停，並設定下一題的服務圖示
            uiState = uiState.copy(
                popupMessage = "",
                message = "(等待服務圖示掉落)",
                isGamePaused = false, // 解除暫停
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
                text = "作者：資管二B 李維駿 ",
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
                    color = Color.Red
                )
            }
        }

        // 2. 放置角色圖示 (保持不變)
        // 嬰幼兒 (role0) - 左邊
        Image(painter = painterResource(id = BABY_IMAGE_ID), contentDescription = "嬰幼兒", modifier = Modifier.size(ROLE_IMAGE_SIZE).align(Alignment.CenterStart).offset(y = VERTICAL_OFFSET_TOP_ROLES))
        // 兒童 (role1) - 右邊
        Image(painter = painterResource(id = CHILD_IMAGE_ID), contentDescription = "兒童", modifier = Modifier.size(ROLE_IMAGE_SIZE).align(Alignment.CenterEnd).offset(y = VERTICAL_OFFSET_TOP_ROLES))
        // 成人 (role2) - 左下角
        Image(painter = painterResource(id = ADULT_IMAGE_ID), contentDescription = "成人", modifier = Modifier.size(ROLE_IMAGE_SIZE).align(Alignment.BottomStart))
        // 一般民眾 (role3) - 右下角
        Image(painter = painterResource(id = GENERAL_PUBLIC_IMAGE_ID), contentDescription = "一般民眾", modifier = Modifier.size(ROLE_IMAGE_SIZE).align(Alignment.BottomEnd))

        // 3. 放置不斷掉落的服務圖示，並傳遞遊戲狀態和回調函式
        FallingServiceIcon(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            uiState = uiState, // 傳遞狀態
            onCollision = handleCollision, // 碰撞處理
            onReset = { nextId -> uiState = uiState.copy(currentServiceId = nextId) } // 處理下一題ID
        )

        // 4. 【新增】彈出式訊息 (貼齊底部中央)
        if (uiState.popupMessage.isNotEmpty()) {
            Text(
                text = uiState.popupMessage,
                fontSize = 18.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.8f)) // 半透明白色背景
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
package tw.edu.pu.csim.tcyang.s1130046

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tw.edu.pu.csim.tcyang.s1130046.ui.theme.S1130046Theme

// 假設您在 drawable 資料夾中有名為 happy 的圖片
// 如果您的圖片名稱不同，請更改 R.drawable.happy
val HAPPY_IMAGE_ID = R.drawable.happy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 設定全螢幕沉浸模式以隱藏系統列
        hideSystemUI()

        setContent {
            S1130046Theme {
                // 將 Greeting 替換為您的新主畫面
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // 2. 隱藏系統列的函式
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

/**
 * 應用程式的主畫面 Composable
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    // 設置 Box 作為容器，用於設置黃色背景
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFF00)) // 黃色背景 (使用 ARGB 格式)
    ) {
        // 取得螢幕的寬度和高度，並轉換為 dp 單位
        val widthDp = maxWidth
        val heightDp = maxHeight

        // 將 dp 轉換為浮點數，以便顯示
        val widthFloat = widthDp.value
        val heightFloat = heightDp.value

        // Column 用於垂直排列所有內容 (圖片和文字)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // 讓 Column 內的所有內容水平居中
        ) {
            // 1. 圖片 (放在正中央) - 使用 Spacer 將其推到中央附近
            Spacer(modifier = Modifier.weight(1f)) // 佔據上方空間

            Image(
                painter = painterResource(id = HAPPY_IMAGE_ID),
                contentDescription = "Happy Image",
                modifier = Modifier.size(200.dp) // 可以調整圖片大小
            )

            Spacer(modifier = Modifier.weight(0.5f)) // 圖片下方的間隔，權重小一點

            // 2. 文字資訊

            // 第一行字：瑪麗亞基金會服務大考驗
            Text(
                text = "瑪麗亞基金會服務大考驗",
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 第二行：資管二B 李維駿
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "資管二B 李維駿",
                fontSize = 18.sp
            )

            // 第三行：螢幕大小 (讀取螢幕寬高)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "螢幕大小 寬 ${"%.1f".format(widthFloat)} * 高 ${"%.1f".format(heightFloat)}",
                fontSize = 16.sp
            )

            // 第四行：成績
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "成績："+"分",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f)) // 佔據下方空間，將內容保持在垂直中央
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
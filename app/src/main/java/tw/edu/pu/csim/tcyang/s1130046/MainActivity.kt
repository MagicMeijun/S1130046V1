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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tw.edu.pu.csim.tcyang.s1130046.ui.theme.S1130046Theme

// 【修正圖片資源名稱為 role0 到 role3】
val HAPPY_IMAGE_ID = R.drawable.happy // 中央圖片
val BABY_IMAGE_ID = R.drawable.role0   // 嬰幼兒
val CHILD_IMAGE_ID = R.drawable.role1  // 兒童
val ADULT_IMAGE_ID = R.drawable.role2  // 成人
val GENERAL_PUBLIC_IMAGE_ID = R.drawable.role3 // 一般民眾

// 設定圖片的新尺寸
private val ROLE_IMAGE_SIZE = 150.dp

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFFF00)) // 黃色背景
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // ----------------------------------------------------
        // 1. 放置中央的 happy 圖片和文字資訊 (調整為更集中)
        // ----------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = screenHeight / 6), // 稍微向下偏移，讓頂部有空間
            horizontalAlignment = Alignment.CenterHorizontally,
            // 讓內容在 Column 內集中，而不是分散填滿
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {

            // 中央圖片 (Happy Image)
            Image(
                painter = painterResource(id = HAPPY_IMAGE_ID),
                contentDescription = "Happy Image",
                modifier = Modifier.size(200.dp)
            )

            // 文字資訊

            Text(
                text = "瑪麗亞基金會服務大考驗",
                fontSize = 20.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            // 【修正作者名稱】
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
                text = "成績：0分", // 根據截圖，預設顯示 0 分
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        // ----------------------------------------------------
        // 2. 放置角色圖示 (使用新的 ROLE_IMAGE_SIZE = 150.dp)
        // ----------------------------------------------------

        // 嬰幼兒 (role0) - 左邊，下方切齊螢幕高 1/2
        Image(
            painter = painterResource(id = BABY_IMAGE_ID),
            contentDescription = "嬰幼兒",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomStart)
                .offset(y = -screenHeight / 2) // 向上偏移螢幕高度的一半
        )

        // 兒童 (role1) - 右邊，下方切齊螢幕高 1/2
        Image(
            painter = painterResource(id = CHILD_IMAGE_ID),
            contentDescription = "兒童",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomEnd)
                .offset(y = -screenHeight / 2) // 向上偏移螢幕高度的一半
        )

        // 成人 (role2) - 左下角
        Image(
            painter = painterResource(id = ADULT_IMAGE_ID),
            contentDescription = "成人",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomStart)
        )

        // 一般民眾 (role3) - 右下角
        Image(
            painter = painterResource(id = GENERAL_PUBLIC_IMAGE_ID),
            contentDescription = "一般民眾",
            modifier = Modifier
                .size(ROLE_IMAGE_SIZE)
                .align(Alignment.BottomEnd)
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
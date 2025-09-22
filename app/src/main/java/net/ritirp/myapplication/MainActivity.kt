package net.ritirp.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kakao.vectormap.LatLng
import net.ritirp.myapplication.ui.screens.MainScreen
import net.ritirp.myapplication.ui.screens.NavigationScreen
import net.ritirp.myapplication.ui.theme.RiTripTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiTripTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RiTripApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RiTripApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var startLocation by remember { mutableStateOf<LatLng?>(null) }
    var endLocation by remember { mutableStateOf<LatLng?>(null) }

    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            MainScreen(
                onNavigationClick = { start, end ->
                    startLocation = start
                    endLocation = end
                    navController.navigate("navigation")
                }
            )
        }

        composable("navigation") {
            startLocation?.let { start ->
                endLocation?.let { end ->
                    NavigationScreen(
                        startLocation = start,
                        endLocation = end,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

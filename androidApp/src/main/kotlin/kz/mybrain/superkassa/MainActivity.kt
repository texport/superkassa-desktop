package kz.mybrain.superkassa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.mybrain.superkassa.presentation.starting.StartingScreen
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.strings.ProvideStrings
import kz.mybrain.superkassa.presentation.theme.SuperkassaTheme

/**
 * Точка входа кассы на Android.
 *
 * Касса поднимается в [SuperkassaApp]; пока она поднимается — и пока экраны
 * областей не перенесены в общий код — показывается общий экран запуска
 * в общей теме и на языке системы. Модели экранов активность хранит сама:
 * она и есть их хранилище, и они переживают поворот экрана.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperkassaTheme {
                ProvideStrings(Language.byCode(null)) { StartingScreen() }
            }
        }
    }
}

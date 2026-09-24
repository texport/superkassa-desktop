package kz.mybrain.superkassa.data.local

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Активность кассы, которая сейчас на экране.
 *
 * Системной печати и выбору файла нужна активность, а не контекст
 * приложения: диалог печати и окно «Сохранить» открываются поверх неё.
 * Касса же живёт в приложении и переживает поворот экрана, который
 * пересоздаёт активность, — поэтому ссылка на активность берётся здесь,
 * в момент обращения, а не хранится адаптерами.
 */
class ForegroundActivity(application: Application) {
    private var current: ComponentActivity? = null
    private var asked = 0

    init {
        application.registerActivityLifecycleCallbacks(Tracker())
    }

    /** Активность на экране; `null` — касса работает без экрана, например в фоне. */
    val activity: ComponentActivity? get() = current

    /**
     * Спрашивает системным окном, куда сохранить файл [name] вида [mime].
     *
     * @return адрес выбранного файла; `null` — окно закрыли или экрана нет.
     */
    suspend fun createDocument(name: String, mime: String): Uri? = withContext(Dispatchers.Main) {
        val screen = current ?: return@withContext null
        suspendCancellableCoroutine { answer ->
            val registry = screen.activityResultRegistry
            var launcher: androidx.activity.result.ActivityResultLauncher<String>? = null
            launcher = registry.register("$KEY${asked++}", ActivityResultContracts.CreateDocument(mime)) { uri ->
                launcher?.unregister()
                if (answer.isActive) answer.resume(uri)
            }
            answer.invokeOnCancellation { launcher.unregister() }
            launcher.launch(name)
        }
    }

    /** Следит, какая активность кассы на экране. */
    private inner class Tracker : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity is ComponentActivity) current = activity
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity === current) current = null
        }

        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    }

    private companion object {
        /** Ключ окна выбора файла: у каждого обращения свой. */
        const val KEY = "superkassa.save."
    }
}

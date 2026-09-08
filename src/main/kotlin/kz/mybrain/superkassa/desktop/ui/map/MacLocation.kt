package kz.mybrain.superkassa.desktop.ui.map

import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Своё место от самого макбука — службой геопозиции macOS.
 *
 * Это то же, чем пользуются «Карты» на этой машине: положение считается
 * по видимым точкам Wi-Fi, и точность выходит десятки метров, а не город.
 * Определение по адресу подключения рядом с ней — запасной путь для
 * машин, где службы нет или владелец её не разрешил.
 *
 * Разрешение спрашивает сама система — тем окном, которое владелец видел
 * от других приложений. Приложение к разрешению не прикасается: оно
 * только просит и ждёт ответа.
 *
 * Обращение идёт через обычную для macOS среду выполнения Objective-C:
 * своего кода на ней нет, вызываются готовые `CLLocationManager`
 * и `CLLocation`. Любая осечка — нет службы, отказ владельца, чужая
 * система — возвращает `null`, и место ищется запасным путём.
 */
object MacLocation {

    /** Работает ли этот путь на этой машине. */
    val available: Boolean by lazy { runCatching { onMac && objc != null }.getOrDefault(false) }

    /**
     * Спрашивает место у системы.
     *
     * Ответ приходит не сразу: система показывает окно разрешения,
     * а потом опрашивает окружение. Поэтому положение запрашивается
     * не однажды, а в течение [WAIT_MS] — молчание первой секунды
     * означает «ещё думает», а не «не знает».
     */
    suspend fun locate(): MapPlace? {
        if (!available) return null
        val manager = runCatching { newManager() }.getOrNull() ?: return null
        onMainThread(manager, "requestWhenInUseAuthorization")
        onMainThread(manager, "startUpdatingLocation")
        try {
            repeat(WAIT_MS / STEP_MS) {
                placeOf(manager)?.let { return it }
                delay(STEP_MS.toLong())
            }
        } finally {
            onMainThread(manager, "stopUpdatingLocation")
        }
        return null
    }

    /** Положение, если система его уже знает. */
    private fun placeOf(manager: Pointer): MapPlace? {
        val location = message(manager, "location") ?: return null
        if (Pointer.nativeValue(location) == 0L) return null
        val coordinate = msgSend.invoke(Coordinate::class.java, arrayOf(location, selector("coordinate")))
        val place = coordinate as? Coordinate ?: return null
        if (place.latitude == 0.0 && place.longitude == 0.0) return null
        return MapPlace(place.latitude, place.longitude, "")
    }

    private fun newManager(): Pointer {
        val cls = objcGetClass.invokePointer(arrayOf("CLLocationManager"))
        val allocated = msgSend.invokePointer(arrayOf(cls, selector("alloc")))
        return msgSend.invokePointer(arrayOf(allocated, selector("init")))
    }

    /**
     * Просит систему сделать это в своей главной очереди.
     *
     * Служба геопозиции привязывается к циклу событий того потока,
     * где её попросили, и окно разрешения показывает оттуда же.
     * У потоков приложения такого цикла нет — просьба из них уходила
     * в пустоту: ни окна, ни координат.
     */
    private fun onMainThread(target: Pointer, name: String) {
        msgSend.invoke(
            Void::class.java,
            arrayOf(target, selector(MAIN_THREAD), selector(name), Pointer.NULL, 1.toByte())
        )
    }

    private fun message(target: Pointer, name: String): Pointer? =
        runCatching { msgSend.invokePointer(arrayOf(target, selector(name))) }.getOrNull()

    private fun selector(name: String): Pointer = selectorRegister.invokePointer(arrayOf(name))

    private val onMac: Boolean
        get() = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT).contains("mac")

    private val objc: NativeLibrary? by lazy {
        runCatching {
            NativeLibrary.getInstance("/System/Library/Frameworks/CoreLocation.framework/CoreLocation")
            NativeLibrary.getInstance("objc")
        }.getOrNull()
    }

    private val objcGetClass: Function get() = objc!!.getFunction("objc_getClass")
    private val selectorRegister: Function get() = objc!!.getFunction("sel_registerName")
    private val msgSend: Function get() = objc!!.getFunction("objc_msgSend")

    /** Как попросить систему выполнить действие в главном потоке. */
    private const val MAIN_THREAD = "performSelectorOnMainThread:withObject:waitUntilDone:"

    /** Сколько ждать ответа системы и как часто спрашивать. */
    private const val WAIT_MS = 8_000
    private const val STEP_MS = 250
}

/** `CLLocationCoordinate2D`: две широты подряд, как их отдаёт система. */
@Structure.FieldOrder("latitude", "longitude")
class Coordinate : Structure(), Structure.ByValue {

    @JvmField
    var latitude: Double = 0.0

    @JvmField
    var longitude: Double = 0.0
}

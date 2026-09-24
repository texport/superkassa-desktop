package kz.mybrain.superkassa.domain.cabinet.port

/**
 * Куда владелец сохраняет выданный ему файл: регистрационную карту в PDF.
 *
 * Порт, а не окно выбора файла: на настольной кассе место выбирают окном
 * системы, а экран знает только, что файл предложен и сохранён или нет.
 */
interface SavedFiles {

    /**
     * Предлагает сохранить [bytes] под именем [name].
     *
     * @return имя сохранённого файла; `null` — владелец передумал.
     */
    suspend fun save(bytes: ByteArray, name: String): String?
}

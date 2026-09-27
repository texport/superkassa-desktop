# Правила R8 для выпускной сборки кассы.
#
# Своё правило одно — Kalkan (ниже). Всё, что ядро берёт отражением,
# покрыто правилами, которые библиотеки приносят сами:
#
# - kotlinx-serialization — companion и serializer() классов @Serializable
#   (настройки ядра, оформление чека, ответы фасада);
# - Ktor — члены io.ktor.** и контейнеры движков клиента;
# - Room — сгенерированные *_Impl базы ядра;
# - WorkManager — конструкторы наследников ListenableWorker (QueueWork, ShiftWork).
#
# Кодек протокола (Wire, сгенерированный Kotlin) отражения не использует:
# ProtoAdapter каждого сообщения берётся прямой ссылкой на ADAPTER, а не
# через ProtoAdapter.get(Class). Появится такой вызов — правило для
# сообщений кодека ляжет сюда.

# Kalkan НУЦ РК — провайдер JCA: алгоритмы он объявляет именами классов,
# и KeyStore, Signature и CMS находят их отражением. Своих правил у jar нет.
# LDAP-хранилище сертификатов и смарт-карты (javax.naming, javax.smartcardio)
# на Android не нужны: подпись файлом ключа их не касается.
-keep class kz.gov.pki.kalkan.** { *; }
-dontwarn javax.naming.**
-dontwarn javax.smartcardio.**
# Разметка Lombok (@NonNull) нужна только компилятору Kalkan: в самом jar
# её классов нет, и в работе она не участвует.
-dontwarn lombok.**

# Журнал ядра: SLF4J находит привязку через ServiceLoader по имени класса
# из META-INF/services — R8 не видит прямой ссылки на неё.
-keep class kz.mybrain.superkassa.data.log.CoreLogProvider { <init>(); }

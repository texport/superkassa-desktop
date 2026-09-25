package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

internal actual fun platformEngine(pin: HostPin?): HttpClientEngine = Java.create()

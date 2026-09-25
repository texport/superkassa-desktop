package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformEngine(pin: HostPin?): HttpClientEngine = Darwin.create()

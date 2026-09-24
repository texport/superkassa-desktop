package kz.mybrain.superkassa.integrations.maps

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

internal actual fun platformEngine(): HttpClientEngine = Java.create()

package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

internal actual fun platformEngine(): HttpClientEngine = Java.create()

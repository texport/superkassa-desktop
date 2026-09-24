package kz.mybrain.superkassa.integrations.maps

import io.ktor.client.engine.HttpClientEngine

/** Движок Ktor этой платформы: OkHttp, Darwin или клиент JDK. */
internal expect fun platformEngine(): HttpClientEngine

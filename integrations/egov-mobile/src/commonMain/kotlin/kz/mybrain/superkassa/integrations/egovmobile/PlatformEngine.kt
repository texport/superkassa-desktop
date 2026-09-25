package kz.mybrain.superkassa.integrations.egovmobile

import io.ktor.client.engine.HttpClientEngine

/** Движок Ktor этой платформы: OkHttp, Darwin или клиент JDK. */
internal expect fun platformEngine(): HttpClientEngine

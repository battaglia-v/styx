/*
  Copyright (C) 2013-2021 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.support.configuration

import com.hotels.styx.server.HttpServer
import com.hotels.styx.servers.MockOriginServer
import com.hotels.styx.support.configuration.Origins.Companion.toOrigin
import com.hotels.styx.support.server.FakeHttpServer
import java.time.Duration


object ImplicitOriginConversions {
  fun fakeserver2Origin(fakeServer: FakeHttpServer): Origin = Origin(
    id = fakeServer.originId(),
    appId = fakeServer.appId(),
    host = "localhost",
    port = fakeServer.port())

  fun mockOrigin2Origin(server: MockOriginServer): Origin = Origin(
    id = server.originId(),
    appId = server.appId(),
    host = "localhost",
    port = server.port())

  fun httpServer2Origin(httpServer: HttpServer): Origin = Origin(
    host = "localhost", port = httpServer.inetAddress().port
  )

  fun java2ScalaOrigin(origin: com.hotels.styx.api.extension.Origin): Origin = Origin.fromJava(origin)
}


class Origins(val origins: Set<Origin>) {
    companion object {

        fun toOrigin(appId: String, origin: Origin): Origin =
            if (origin.id.contains("anonymous-origin")) {
                origin.copy(
                    id = origin.hostAsString(),
                    appId = appId
                )
            } else {
                origin.copy(appId = appId)
            }

    }
}

interface StyxBackend {
  val appId: String
  val origins: Origins
  val responseTimeout: Duration
  val connectionPoolConfig: ConnectionPoolSettings
  val healthCheckConfig: HealthCheckConfig?
  val stickySessionConfig: StickySessionConfig

  fun toBackend(path: String): BackendService
}



class HttpBackend(override val appId: String,
                  override val origins: Origins,
                  override val responseTimeout: Duration,
                  override val connectionPoolConfig: ConnectionPoolSettings,
                  override val healthCheckConfig: HealthCheckConfig?,
                  override val stickySessionConfig: StickySessionConfig) : StyxBackend {

    override fun toBackend(path: String) =
        BackendService(appId, path, origins, connectionPoolConfig, healthCheckConfig, stickySessionConfig,
            responseTimeout, tlsSettings = null)

    companion object {
        private val dontCare = Origin("localhost", 0)
        private val defaults = BackendService()
        private val defaultResponseTimeout = defaults.responseTimeout

        fun apply(appId: String,
            origins: Origins,
            responseTimeout: Duration = defaultResponseTimeout,
            connectionPoolConfig: ConnectionPoolSettings = ConnectionPoolSettings(),
            healthCheckConfig: HealthCheckConfig? = null,
            stickySessionConfig: StickySessionConfig = StickySessionConfig()): HttpBackend {

        val originsWithId = origins.origins.map { toOrigin(appId, it) }.toSet()

        return HttpBackend(appId, Origins(originsWithId), responseTimeout, connectionPoolConfig, healthCheckConfig, stickySessionConfig)
        }
    }
}

class HttpsBackend(override val appId: String,
                   override val origins: Origins,
                   override val responseTimeout: Duration,
                   override val connectionPoolConfig: ConnectionPoolSettings,
                   override val healthCheckConfig: HealthCheckConfig? = null,
                   override val stickySessionConfig: StickySessionConfig = StickySessionConfig(),
                   val tlsSettings: TlsSettings) : StyxBackend {
  override fun toBackend(path: String) = BackendService(appId, path, origins, connectionPoolConfig, healthCheckConfig, stickySessionConfig, responseTimeout, tlsSettings = tlsSettings)

  companion object {
    private val dontCare = Origin("localhost", 0)
    private val defaults = BackendService()
    private val defaultResponseTimeout = defaults.responseTimeout

    fun apply(appId: String,
              origins: Origins,
              tlsSettings: TlsSettings,
              responseTimeout: Duration = defaultResponseTimeout,
              connectionPoolConfig: ConnectionPoolSettings = ConnectionPoolSettings(),
              healthCheckConfig: HealthCheckConfig? = null,
              stickySessionConfig: StickySessionConfig = StickySessionConfig()): HttpsBackend {

        val originsWithId = origins.origins.map { toOrigin(appId, it) }.toSet()

        return HttpsBackend(appId, Origins(originsWithId), responseTimeout, connectionPoolConfig, healthCheckConfig, stickySessionConfig, tlsSettings)
    }
  }
}



class BackendService(val appId: String = "generic-app",
                     val path: String = "/",
                     val origins: Origins = Origins(setOf()),
                     val connectionPoolConfig: ConnectionPoolSettings = ConnectionPoolSettings(),
                     val healthCheckConfig: HealthCheckConfig? = null,
                     val stickySessionConfig: StickySessionConfig = StickySessionConfig(),
                     val responseTimeout: Duration = Duration.ofSeconds(35),
                     val maxHeaderSize: Int = 8192,
                     val tlsSettings: TlsSettings? = null) {

  fun asJava(): com.hotels.styx.api.extension.service.BackendService =
      com.hotels.styx.api.extension.service.BackendService.Builder()
          .id(appId)
          .path(path)
          .origins(origins.origins.map { it.asJava() } .toSet())
          .connectionPoolConfig(connectionPoolConfig.asJava())
          .healthCheckConfig(healthCheckConfig?.asJava())
          .stickySessionConfig(stickySessionConfig.asJava())
          .responseTimeoutMillis(responseTimeout.toMillis().toInt())
          .https(tlsSettings?.asJava())
          .maxHeaderSize(maxHeaderSize)
          .build()

    companion object {
        fun fromJava(from: com.hotels.styx.api.extension.service.BackendService): BackendService {
            val config: com.hotels.styx.api.extension.service.ConnectionPoolSettings = from.connectionPoolConfig()

            return BackendService(
                appId = from.id().toString(),
                path = from.path(),
                origins = Origins(from.origins().map { Origin.fromJava(it) }.toSet()),
                connectionPoolConfig = ConnectionPoolSettings.fromJava(config),
                healthCheckConfig = from.healthCheckConfig()?.let { HealthCheckConfig.fromJava(it) },
                stickySessionConfig = StickySessionConfig.fromJava(from.stickySessionConfig()),
                responseTimeout = Duration.ofMillis(from.responseTimeoutMillis().toLong()),
                tlsSettings = from.tlsSettings().map { TlsSettings.fromJava(it) }.orElse(null)
            )
        }
    }
}



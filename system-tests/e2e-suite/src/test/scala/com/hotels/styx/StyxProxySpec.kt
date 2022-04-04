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
package com.hotels.styx

import com.fasterxml.jackson.databind.util.ClassUtil.classOf
import com.hotels.styx.api.extension.service.BackendService
import com.hotels.styx.infrastructure.MemoryBackedRegistry
import com.hotels.styx.infrastructure.RegistryServiceAdapter
import com.hotels.styx.plugins.PluginPipelineSpec
import io.micrometer.core.instrument.MeterRegistry

import com.hotels.styx.support.configuration.StyxBaseConfig
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry


import org.slf4j.LoggerFactory

import java.nio.file.Paths
import java.util.Collections.singletonList

// These classes were previously inherited using the 'with' keyword in Scala:
//BeforeAndAfterAll
//BeforeAndAfterEach
//Matchers
//ImplicitOriginConversions
//ImplicitStyxConversions
//StyxConfiguration
//SSLSetup

// StyxClientSupplier was previously inherited/extended from the class.
class StyxProxySpec  {
//    this: Suite =>
    private val styxClientSuppler: StyxClientSupplier = StyxClientSupplier()
    private val LOGGER = LoggerFactory.getLogger(javaClass)
    var backendsRegistry = MemoryBackedRegistry<BackendService>()
    var pluginPipelineSpec: PluginPipelineSpec = PluginPipelineSpec()
    lateinit var styxServer: StyxServer
    lateinit var meterRegistry: MeterRegistry

    class StyxServerOperations(val styxServer: StyxServer): StyxServerSupplements
    with BackendServicesRegistrySupplier {
      fun setBackends (backends: (String, StyxBackend)*): Unit = setBackends(backendsRegistry, backends:_*)
    }

    fun resourcesPluginsPath(): String {
      val url = classOf(pluginPipelineSpec).classLoader.getResource("plugins")
      return Paths.get(url.toURI()).toString().replace("\\", "/")
    }

    protected fun beforeAll () {
      meterRegistry = CompositeMeterRegistry (Clock.SYSTEM, singletonList(SimpleMeterRegistry()))
      styxServer = styxConfig().startServer(RegistryServiceAdapter (backendsRegistry), meterRegistry)
      LOGGER.info("Styx http port is: [%d]".format(styxServer.httpPort()))
      LOGGER.info("Styx https port is: [%d]".format(styxServer.secureHttpPort()))
//      super.beforeAll()
    }

    protected fun afterAll () {
      LOGGER.info("Styx http port was: [%d]".format(styxServer.httpPort()))
      LOGGER.info("Styx https port was: [%d]".format(styxServer.secureHttpPort()))
      styxServer.stopAsync().awaitTerminated()
//      super.afterAll()
    }
  }

  interface StyxConfiguration {
    fun styxConfig(): StyxBaseConfig
  }

  interface DefaultStyxConfiguration: StyxConfiguration {
      //    lazy
    override fun styxConfig(): StyxBaseConfig = configuration.StyxConfig()
  }

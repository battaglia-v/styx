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
package com.hotels.styx.client

import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.hotels.styx.support.configuration.StyxYamlConfig

import com.hotels.styx.api.HttpRequest.get
import com.hotels.styx.support.ResourcePaths.fixturesHome
import com.hotels.styx.support.backends.FakeHttpServer
import com.hotels.styx.utils.StubOriginHeader.STUB_ORIGIN_INFO
import com.hotels.styx.StyxClientSupplier
import com.hotels.styx.StyxProxySpec
import com.hotels.styx.api.HttpResponseStatus.BAD_GATEWAY
import com.hotels.styx.api.HttpResponseStatus.OK
import com.hotels.styx.support.configuration.Origins
import com.hotels.styx.support.configuration.TlsSettings
import io.kotest.core.spec.style.StringSpec
import org.scalatest.SequentialNestedSuiteExecution

import java.nio.charset.StandardCharsets.UTF_8
import kotlin.time.ExperimentalTime

// These classes were previously inherited using the 'with' keyword in Scala:
//  StyxProxySpec,
//  StyxClientSupplier,
//  SequentialNestedSuiteExecution,

@ExperimentalTime
class TlsVersionSpec: StringSpec() {
    private val styxProxySpec: StyxProxySpec = StyxProxySpec()
    private val styxClientSupplier: StyxClientSupplier = StyxClientSupplier()
//    private val sequentialNestedSuiteExecution: SequentialNestedSuiteExecution = SequentialNestedSuiteExecution()


    fun originResponse(appId: String) = aResponse()
      .withStatus(OK.code())
      .withHeader(STUB_ORIGIN_INFO.toString(), appId)
      .withBody("Hello, World!")

    val logback = fixturesHome(this.javaClass, "/conf/logback/logback-debug-stdout.xml")

    val appOriginTlsv11 = FakeHttpServer.HttpsStartupConfig(
      appId = "appTls11",
      originId = "appTls11-01",
      protocols = listOf("TLSv1.1")
    )
      .start()
      .stub(WireMock.get(urlMatching("/.*")), originResponse("App TLS v1.1"))

    val appOriginTlsv12B = FakeHttpServer.HttpsStartupConfig(
      appId = "appTls11B",
      originId = "appTls11B-01",
      protocols = listOf("TLSv1.2")
    )
      .start()
      .stub(WireMock.get(urlMatching("/.*")), originResponse("App TLS v1.2 B"))

    val appOriginTlsDefault = FakeHttpServer.HttpsStartupConfig(
      appId = "appTlsDefault",
      originId = "appTlsDefault-01",
      protocols = listOf("TLSv1.1", "TLSv1.2")
    )
      .start()
      .stub(WireMock.get(urlMatching("/.*")), originResponse("App TLS v1.1"))

    val appOriginTlsv12 = FakeHttpServer.HttpsStartupConfig(
      appId = "appTls12",
      originId = "appTls12-02",
      protocols = listOf("TLSv1.2")
    )
      .start()
      .stub(WireMock.get(urlMatching("/.*")), originResponse("App TLS v1.2"))

    val styxConfig = StyxYamlConfig(
      """
      |proxy:
      |  connectors:
      |    http:
      |      port: 0
      |admin:
      |  connectors:
      |    http:
      |      port: 0
      |services:
      |  factories: {}
    """.trim(),
      logbackXmlLocation = logback
    )

    override fun beforeTest(): Unit {
      super.beforeAll()

      styxServer().setBackends(
        "/tls11/" -> HttpsBackend(
      "appTls11",
      Origins(appOriginTlsv11),
      TlsSettings(authenticate = false, sslProvider = "JDK", protocols = listOf("TLSv1.1"))),

      "/tlsDefault/" -> HttpsBackend(
      "appTlsDefault",
      Origins(appOriginTlsDefault),
      TlsSettings(authenticate = false, sslProvider = "JDK", protocols = listOf("TLSv1.1", "TLSv1.2"))),

      "/tls12" -> HttpsBackend(
      "appTls12",
      Origins(appOriginTlsv12),
      TlsSettings(authenticate = false, sslProvider = "JDK", protocols = listOf("TLSv1.2"))),

      "/tls11-to-tls12" -> HttpsBackend(
      "appTls11B",
      Origins(appOriginTlsv12B),
      TlsSettings(authenticate = false, sslProvider = "JDK", protocols = listOf("TLSv1.1")))
      )
    }

    override fun afterAll(): Unit {
      appOriginTlsv11.stop()
      appOriginTlsv12.stop()
      afterAll()
    }

    fun httpRequest(path: String) = get(styxServer().routerURL(path)).build()

    //  ERROR : <ERROR FUNCTION RETURN TYPE>
    fun valueMatchingStrategy(matches: String): ValueMatchingStrategy {
      val matchingStrategy = ValueMatchingStrategy()
      matchingStrategy.setMatches(matches)
      return matchingStrategy
    }

    init {
      "Proxies to TLSv1.1 origin when TLSv1.1 support enabled." {
        val response1 = styxClientSupplier.decodedRequest(httpRequest("/tls11/a"))
        assert(response1.status() == OK)
        assert(response1.bodyAs(UTF_8) == "Hello, World!")

        appOriginTlsv11.verify(
          getRequestedFor(
            urlEqualTo("/tls11/a")
          )
            .withHeader("X-Forwarded-Proto", valueMatchingStrategy("http"))
        )

        val response2 = styxClientSupplier.decodedRequest(httpRequest("/tlsDefault/a2"))
        assert(response2.status() == OK)
        assert(response2.bodyAs(UTF_8) == "Hello, World!")

        appOriginTlsDefault.verify(
          getRequestedFor(
            urlEqualTo("/tlsDefault/a2")
          )
            .withHeader("X-Forwarded-Proto", valueMatchingStrategy("http"))
        )
      }

      "Proxies to TLSv1.2 origin when TLSv1.2 support is enabled." {
        val response1 = styxClientSupplier.decodedRequest(httpRequest("/tlsDefault/b1"))
        assert(response1.status() == OK)
        assert(response1.bodyAs(UTF_8) == "Hello, World!")

        appOriginTlsDefault.verify(
          getRequestedFor(urlEqualTo("/tlsDefault/b1"))
            .withHeader("X-Forwarded-Proto", valueMatchingStrategy("http"))
        )

        val response2 = styxClientSupplier.decodedRequest(httpRequest("/tls12/b2"))
        assert(response2.status() == OK)
        assert(response2.bodyAs(UTF_8) == "Hello, World!")

        appOriginTlsv12.verify(
          getRequestedFor(urlEqualTo("/tls12/b2"))
            .withHeader("X-Forwarded-Proto", valueMatchingStrategy("http"))
        )
      }

      "Refuses to connect to TLSv1.1 origin when TLSv1.1 is disabled" {
        val response = styxClientSupplier.decodedRequest(httpRequest("/tls11-to-tls12/c"))

        assert(response.status() == BAD_GATEWAY)
        assert(response.bodyAs(UTF_8) == "Site temporarily unavailable.")

        appOriginTlsv12B.verify(0, getRequestedFor(urlEqualTo("/tls11-to-tls12/c")))
      }
    }

  }




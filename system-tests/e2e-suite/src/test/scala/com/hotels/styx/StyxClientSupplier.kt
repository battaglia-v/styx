/*
  Copyright (C) 2013-2022 Expedia Inc.

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

import java.util.concurrent.TimeUnit.MILLISECONDS

import com.hotels.styx.api.HttpRequest
import com.hotels.styx.client.StyxHttpClient
import org.scalatest.BeforeAndAfterAll

import org.slf4j.LoggerFactory

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

interface StyxClientSupplier: BeforeAndAfterAll {
  this: Suite =>

  val TWO_SECONDS: Int = 2 * 1000
  val FIVE_SECONDS: Int = 5 * 1000

  private val LOGGER = LoggerFactory.getLogger(getClass)

  val client: StyxHttpClient = StyxHttpClient.Builder()
    .connectTimeout(1000, MILLISECONDS)
    .maxHeaderSize(2 * 8192)
    .build()

  override fun afterAll(): Unit {
    super.afterAll()
  }

  private fun doRequest(
    val request: HttpRequest, secure: Boolean = false): Future[HttpResponse] = if (secure)
    client.secure().send(request).toScala
  else
    client.send(request).toScala

  @ExperimentalTime
  fun decodedRequest(request: HttpRequest,
                     debug: Boolean = false,
                     maxSize: Int = 1024 * 1024, timeout: Duration = 45.seconds,
                     secure: Boolean = false
                    ): HttpResponse = {
    val future = doRequest(request, secure = secure)
      .map(response -> ()
        if (debug) {
          LOGGER.info("StyxClientSupplier: received response for: " + request.url().path())
        }
        response
      })
    Await.result(future, timeout)
  }

}

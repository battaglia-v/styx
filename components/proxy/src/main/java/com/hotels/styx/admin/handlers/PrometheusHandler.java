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
package com.hotels.styx.admin.handlers;

import com.hotels.styx.api.Eventual;
import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.api.WebServiceHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static com.hotels.styx.api.HttpHeaderNames.CONTENT_TYPE;
import static com.hotels.styx.api.HttpResponseStatus.OK;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PrometheusHandler implements WebServiceHandler {

    private final PrometheusMeterRegistry prometheusRegistry;

    public PrometheusHandler(MeterRegistry meterRegistry) {
        prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ((CompositeMeterRegistry) meterRegistry).add(prometheusRegistry);
    }

    @Override
    public Eventual<HttpResponse> handle(final HttpRequest request, final HttpInterceptor.Context context) {
        return Eventual.of(HttpResponse
                .response(OK)
                .disableCaching()
                .header(CONTENT_TYPE, PLAIN_TEXT_UTF_8)
                .body(prometheusRegistry.scrape(), UTF_8, true)
                .build());
    }
}

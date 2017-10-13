/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.admin.handlers;

import com.hotels.styx.api.HttpHandler;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.api.http.handlers.BaseHttpHandler;
import com.hotels.styx.server.HttpInterceptorContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.testng.annotations.Test;

import static com.hotels.styx.support.api.BlockingObservables.getFirst;
import static com.hotels.styx.api.HttpRequest.Builder.get;
import static com.hotels.styx.api.HttpResponse.Builder.response;
import static com.hotels.styx.support.api.matchers.HttpResponseBodyMatcher.hasBody;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.hamcrest.MatcherAssert.assertThat;

public class StatusHandlerTest {

    @Test
    public void returnsOKForHealthyHealthcheck() {
        StatusHandler statusHandler = new StatusHandler(underlyingHealthCheckIs(OK));
        HttpResponse response = handle(statusHandler, get("/status").build());
        assertThat(response, hasBody("OK"));
    }

    @Test
    public void returnsNOT_OKForFaultyHealthcheck() {
        StatusHandler statusHandler = new StatusHandler(underlyingHealthCheckIs(INTERNAL_SERVER_ERROR));
        HttpResponse response = handle(statusHandler, get("/status").build());
        assertThat(response, hasBody("NOT_OK"));
    }

    private static HttpHandler underlyingHealthCheckIs(HttpResponseStatus status) {
        return new BaseHttpHandler() {
            @Override
            protected HttpResponse doHandle(HttpRequest request) {
                return response(status)
                        .body("some stuff")
                        .build();
            }
        };
    }

    private static HttpResponse handle(HttpHandler handler, HttpRequest request) {
        return getFirst(handler.handle(request));
    }
}

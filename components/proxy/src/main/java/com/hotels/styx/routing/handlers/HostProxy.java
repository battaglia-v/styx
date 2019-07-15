/*
  Copyright (C) 2013-2019 Expedia Inc.

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
package com.hotels.styx.routing.handlers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.hotels.styx.api.Eventual;
import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.LiveHttpRequest;
import com.hotels.styx.api.LiveHttpResponse;
import com.hotels.styx.api.ResponseEventListener;
import com.hotels.styx.api.extension.Origin;
import com.hotels.styx.api.extension.service.ConnectionPoolSettings;
import com.hotels.styx.api.extension.service.TlsSettings;
import com.hotels.styx.client.Connection;
import com.hotels.styx.client.OriginStatsFactory;
import com.hotels.styx.client.StyxHostHttpClient;
import com.hotels.styx.client.applications.metrics.OriginMetrics;
import com.hotels.styx.client.connectionpool.ConnectionPool;
import com.hotels.styx.client.connectionpool.ExpiringConnectionFactory;
import com.hotels.styx.client.connectionpool.SimpleConnectionPoolFactory;
import com.hotels.styx.client.netty.connectionpool.NettyConnectionFactory;
import com.hotels.styx.config.schema.Schema;
import com.hotels.styx.infrastructure.configuration.yaml.JsonNodeConfig;
import com.hotels.styx.routing.RoutingObject;
import com.hotels.styx.routing.config.RoutingObjectFactory;
import com.hotels.styx.routing.config.StyxObjectDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.hotels.styx.api.extension.Origin.newOriginBuilder;
import static com.hotels.styx.api.extension.service.ConnectionPoolSettings.defaultConnectionPoolSettings;
import static com.hotels.styx.client.HttpRequestOperationFactory.Builder.httpRequestOperationFactoryBuilder;
import static com.hotels.styx.config.schema.SchemaDsl.atLeastOne;
import static com.hotels.styx.config.schema.SchemaDsl.bool;
import static com.hotels.styx.config.schema.SchemaDsl.field;
import static com.hotels.styx.config.schema.SchemaDsl.integer;
import static com.hotels.styx.config.schema.SchemaDsl.list;
import static com.hotels.styx.config.schema.SchemaDsl.object;
import static com.hotels.styx.config.schema.SchemaDsl.optional;
import static com.hotels.styx.config.schema.SchemaDsl.string;
import static com.hotels.styx.routing.config.RoutingSupport.missingAttributeError;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * A routing object that proxies all incoming traffic to a remote host.
 */
public class HostProxy implements RoutingObject {
    public static final Schema.FieldType SCHEMA = object(
            field("host", string()),
            optional("tlsSettings", object(
                    optional("trustAllCerts", bool()),
                    optional("sslProvider", string()),
                    optional("trustStorePath", string()),
                    optional("trustStorePassword", string()),
                    optional("protocols", list(string())),
                    optional("cipherSuites", list(string())),
                    optional("additionalCerts", list(object(
                            field("alias", string()),
                            field("certificatePath", string())
                    ))),
                    atLeastOne("trustAllCerts",
                            "trustStorePath",
                            "trustStorePassword",
                            "protocols",
                            "cipherSuites",
                            "additionalCerts")
            )),
            optional("connectionPool", object(
                    optional("maxConnections", integer()),
                    optional("maxPendingConnections", integer()),
                    optional("connectTimeoutMillis", integer()),
                    optional("socketTimeoutMillis", integer()),
                    optional("pendingConnectionTimeoutMillis", integer()),
                    optional("connectionExpirationSeconds", integer()),
                    atLeastOne("maxConnections",
                            "maxPendingConnections",
                            "connectTimeoutMillis",
                            "socketTimeoutMillis",
                            "pendingConnectionTimeoutMillis",
                            "connectionExpirationSeconds")
            )),
            optional("responseTimeoutMillis", integer()),
            optional("metricPrefix", string())
    );

    private final String errorMessage;
    private final StyxHostHttpClient client;
    private OriginMetrics originMetrics;
    private volatile boolean active = true;

    @VisibleForTesting
    final HostAndPort hostAndPort;

    public HostProxy(HostAndPort hostAndPort, StyxHostHttpClient client, OriginMetrics originMetrics) {
        this.hostAndPort = requireNonNull(hostAndPort);
        this.errorMessage = format("HostProxy %s:%d is stopped but received traffic.",
                hostAndPort.getHostText(),
                hostAndPort.getPort());
        this.client = requireNonNull(client);
        this.originMetrics = requireNonNull(originMetrics);
    }

    @Override
    public Eventual<LiveHttpResponse> handle(LiveHttpRequest request, HttpInterceptor.Context context) {
        if (active) {
            return new Eventual<>(
                    ResponseEventListener.from(client.sendRequest(request))
                            .whenCancelled(() -> originMetrics.requestCancelled())
                            .apply());
        } else {
            return Eventual.error(new IllegalStateException(errorMessage));
        }
    }

    @Override
    public CompletableFuture<Void> stop() {
        active = false;
        client.close();
        return completedFuture(null);
    }

    /**
     * A factory for creating HostProxy routingObject objects.
     */
    public static class Factory implements RoutingObjectFactory {
        private static final int DEFAULT_REQUEST_TIMEOUT = 60000;
        private static final int DEFAULT_TLS_PORT = 443;
        private static final int DEFAULT_HTTP_PORT = 80;

        @Override
        public RoutingObject build(List<String> fullName, Context context, StyxObjectDefinition configBlock) {
            JsonNodeConfig config = new JsonNodeConfig(configBlock.config());

            ConnectionPoolSettings poolSettings = config.get("connectionPool", ConnectionPoolSettings.class)
                    .orElse(defaultConnectionPoolSettings());

            TlsSettings tlsSettings = config.get("tlsSettings", TlsSettings.class)
                    .orElse(null);

            int responseTimeoutMillis = config.get("responseTimeoutMillis", Integer.class)
                    .orElse(DEFAULT_REQUEST_TIMEOUT);

            String metricPrefix = config.get("metricPrefix", String.class)
                    .orElse("routing.objects.hostProxy");

            HostAndPort hostAndPort = config.get("host")
                    .map(HostAndPort::fromString)
                    .map(it -> addDefaultPort(it, tlsSettings))
                    .orElseThrow(() -> missingAttributeError(configBlock, join(".", fullName), "host"));

            return createHostProxyHandler(context, hostAndPort, poolSettings, tlsSettings, responseTimeoutMillis, metricPrefix);
        }

        private static HostAndPort addDefaultPort(HostAndPort hostAndPort, TlsSettings tlsSettings) {
            if (hostAndPort.hasPort()) {
                return hostAndPort;
            }

            int defaultPort = Optional.ofNullable(tlsSettings)
                    .map(it -> DEFAULT_TLS_PORT)
                    .orElse(DEFAULT_HTTP_PORT);

            return HostAndPort.fromParts(hostAndPort.getHostText(), defaultPort);
        }

        @NotNull
        private static HostProxy createHostProxyHandler(
                Context context,
                HostAndPort hostAndPort,
                ConnectionPoolSettings poolSettings,
                TlsSettings tlsSettings,
                int responseTimeoutMillis,
                String metricPrefix) {

            Origin origin = newOriginBuilder(hostAndPort.getHostText(), hostAndPort.getPort())
                    .applicationId(metricPrefix)
                    .id(format("%s:%s", hostAndPort.getHostText(), hostAndPort.getPort()))
                    .build();

            OriginMetrics originMetrics = OriginMetrics.create(
                    origin,
                    context.environment().metricRegistry());

            ConnectionPool.Factory connectionPoolFactory = new SimpleConnectionPoolFactory.Builder()
                    .connectionFactory(
                            connectionFactory(
                                    tlsSettings,
                                    responseTimeoutMillis,
                                    theOrigin -> originMetrics,
                                    poolSettings.connectionExpirationSeconds()))
                    .connectionPoolSettings(poolSettings)
                    .metricRegistry(context.environment().metricRegistry())
                    .build();

            return new HostProxy(hostAndPort, StyxHostHttpClient.create(connectionPoolFactory.create(origin)), originMetrics);
        }

        private static Connection.Factory connectionFactory(
                TlsSettings tlsSettings,
                int responseTimeoutMillis,
                OriginStatsFactory originStatsFactory,
                long connectionExpiration) {

            NettyConnectionFactory factory = new NettyConnectionFactory.Builder()
                    .httpRequestOperationFactory(
                            httpRequestOperationFactoryBuilder()
                                    .flowControlEnabled(true)
                                    .originStatsFactory(originStatsFactory)
                                    .responseTimeoutMillis(responseTimeoutMillis)
                                    .build()
                    )
                    .tlsSettings(tlsSettings)
                    .build();

            if (connectionExpiration > 0) {
                return new ExpiringConnectionFactory(connectionExpiration, factory);
            } else {
                return factory;
            }
        }

    }

}

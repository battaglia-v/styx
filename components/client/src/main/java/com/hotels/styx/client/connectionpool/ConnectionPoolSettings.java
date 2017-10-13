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
package com.hotels.styx.client.connectionpool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hotels.styx.api.client.ConnectionPool;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Objects.toStringHelper;

/**
 * Programmatically configurable connection pool settings.
 */
public class ConnectionPoolSettings implements ConnectionPool.Settings {
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 50;
    public static final int DEFAULT_MAX_PENDING_CONNECTIONS_PER_HOST = 25;
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 2000;
    public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 11000;

    private final int maxConnectionsPerHost;
    private final int maxPendingConnectionsPerHost;
    private final int connectTimeoutMillis;
    private final int socketTimeoutMillis;
    private final int pendingConnectionTimeoutMillis;

    @JsonCreator
    ConnectionPoolSettings(@JsonProperty("maxConnectionsPerHost") Integer maxConnectionsPerHost,
                           @JsonProperty("maxPendingConnectionsPerHost") Integer maxPendingConnectionsPerHost,
                           @JsonProperty("connectTimeoutMillis") Integer connectTimeoutMillis,
                           @JsonProperty("socketTimeoutMillis") Integer socketTimeoutMillis,
                           @JsonProperty("pendingConnectionTimeoutMillis") Integer pendingConnectionTimeoutMillis
                           ) {
        this.maxConnectionsPerHost = firstNonNull(maxConnectionsPerHost, DEFAULT_MAX_CONNECTIONS_PER_HOST);
        this.maxPendingConnectionsPerHost = firstNonNull(maxPendingConnectionsPerHost, DEFAULT_MAX_PENDING_CONNECTIONS_PER_HOST);
        this.connectTimeoutMillis = firstNonNull(connectTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS);
        this.socketTimeoutMillis = firstNonNull(socketTimeoutMillis, DEFAULT_SOCKET_TIMEOUT_MILLIS);
        this.pendingConnectionTimeoutMillis = firstNonNull(pendingConnectionTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS);
    }

    ConnectionPoolSettings(int maxConnectionsPerHost,
                           int maxPendingConnectionsPerHost,
                           int connectTimeoutMillis,
                           int socketTimeoutMillis,
                           int pendingConnectionTimeoutMillis) {
        this.maxConnectionsPerHost = firstNonNull(maxConnectionsPerHost, DEFAULT_MAX_CONNECTIONS_PER_HOST);
        this.maxPendingConnectionsPerHost = firstNonNull(maxPendingConnectionsPerHost, DEFAULT_MAX_PENDING_CONNECTIONS_PER_HOST);
        this.connectTimeoutMillis = firstNonNull(connectTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS);
        this.socketTimeoutMillis = firstNonNull(socketTimeoutMillis, DEFAULT_SOCKET_TIMEOUT_MILLIS);
        this.pendingConnectionTimeoutMillis = firstNonNull(pendingConnectionTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS);
    }

    private ConnectionPoolSettings(Builder builder) {
        this(
                builder.maxConnectionsPerHost,
                builder.maxPendingConnectionsPerHost,
                builder.connectTimeoutMillis,
                builder.socketTimeoutMillis,
                builder.pendingConnectionTimeoutMillis
                );
    }

    /**
     * Creates a new instance with default settings.
     *
     * @return a new instance
     */
    public static ConnectionPoolSettings defaultSettableConnectionPoolSettings() {
        return new ConnectionPoolSettings(new Builder());
    }

    @Override
    @JsonProperty("socketTimeoutMillis")
    public int socketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    @Override
    @JsonProperty("connectTimeoutMillis")
    public int connectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    @Override
    @JsonProperty("maxConnectionsPerHost")
    public int maxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    @Override
    @JsonProperty("maxPendingConnectionsPerHost")
    public int maxPendingConnectionsPerHost() {
        return maxPendingConnectionsPerHost;
    }

    @Override
    @JsonProperty("pendingConnectionTimeoutMillis")
    public int pendingConnectionTimeoutMillis() {
        return pendingConnectionTimeoutMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxConnectionsPerHost, maxPendingConnectionsPerHost, connectTimeoutMillis,
                socketTimeoutMillis, pendingConnectionTimeoutMillis);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConnectionPoolSettings other = (ConnectionPoolSettings) obj;
        return Objects.equals(this.maxConnectionsPerHost, other.maxConnectionsPerHost)
                && Objects.equals(this.maxPendingConnectionsPerHost, other.maxPendingConnectionsPerHost)
                && Objects.equals(this.connectTimeoutMillis, other.connectTimeoutMillis)
                && Objects.equals(this.socketTimeoutMillis, other.socketTimeoutMillis)
                && Objects.equals(this.pendingConnectionTimeoutMillis, other.pendingConnectionTimeoutMillis);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("maxConnectionsPerHost", maxConnectionsPerHost)
                .add("maxPendingConnectionsPerHost", maxPendingConnectionsPerHost)
                .add("connectTimeoutMillis", connectTimeoutMillis)
                .add("socketTimeoutMillis", socketTimeoutMillis)
                .add("pendingConnectionTimeoutMillis", pendingConnectionTimeoutMillis)
                .toString();
    }

    /**
     * A builder that builds {@link ConnectionPoolSettings}s. Will use default values for any settings not set.
     */
    public static final class Builder {
        private int maxConnectionsPerHost = DEFAULT_MAX_CONNECTIONS_PER_HOST;
        private int maxPendingConnectionsPerHost = DEFAULT_MAX_PENDING_CONNECTIONS_PER_HOST;
        private int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
        private int socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS;
        private int pendingConnectionTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;

        /**
         * Constructs an instance with default settings.
         */
        public Builder() {
        }

        /**
         * Constructs an instance with inherited settings.
         *
         * @param settings settings to inherit from
         */
        public Builder(ConnectionPool.Settings settings) {
            this.maxConnectionsPerHost = settings.maxConnectionsPerHost();
            this.maxPendingConnectionsPerHost = settings.maxPendingConnectionsPerHost();
            this.connectTimeoutMillis = settings.connectTimeoutMillis();
            this.socketTimeoutMillis = settings.socketTimeoutMillis();
            this.pendingConnectionTimeoutMillis = settings.pendingConnectionTimeoutMillis();
        }

        /**
         * Sets the maximum number of active connections for a single hosts's connection pool.
         *
         * @param maxConnectionsPerHost maximum number of active connections
         * @return this builder
         */
        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }

        /**
         * Sets the maximum allowed number of consumers, per host, waiting for a connection.
         *
         * @param maxPendingConnectionsPerHost maximum number of consumers
         * @return this builder
         */
        public Builder maxPendingConnectionsPerHost(int maxPendingConnectionsPerHost) {
            this.maxPendingConnectionsPerHost = maxPendingConnectionsPerHost;
            return this;
        }

        /**
         * Sets socket read timeout.
         *
         * @param socketTimeout read timeout
         * @param timeUnit unit of timeout
         * @return this builder
         */
        public Builder socketTimeout(int socketTimeout, TimeUnit timeUnit) {
            this.socketTimeoutMillis = (int) timeUnit.toMillis(socketTimeout);
            return this;
        }

        /**
         * Sets socket connect timeout.
         *
         * @param connectTimeout connect timeout
         * @param timeUnit unit of timeout
         * @return this builder
         */
        public Builder connectTimeout(int connectTimeout, TimeUnit timeUnit) {
            this.connectTimeoutMillis = (int) timeUnit.toMillis(connectTimeout);
            return this;
        }

        /**
         * Sets the maximum wait time for pending consumers.
         *
         * @param waitTimeout timeout
         * @param timeUnit unit that timeout is measured in
         * @return this builder
         */
        public Builder pendingConnectionTimeout(int waitTimeout, TimeUnit timeUnit) {
            this.pendingConnectionTimeoutMillis = (int) timeUnit.toMillis(waitTimeout);
            return this;
        }

        /**
         * Constructs a new instance with the configured settings.
         *
         * @return a new instance
         */
        public ConnectionPoolSettings build() {
            return new ConnectionPoolSettings(this);
        }
    }
}

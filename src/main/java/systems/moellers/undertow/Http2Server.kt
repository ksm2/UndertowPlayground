/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package systems.moellers.undertow

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Handlers.*
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.attribute.ExchangeAttributes
import io.undertow.predicate.Predicates.secure
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.LearningPushHandler
import io.undertow.server.session.InMemorySessionManager
import io.undertow.server.session.SessionAttachmentHandler
import io.undertow.server.session.SessionCookieConfig
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import systems.moellers.undertow.error.*
import systems.moellers.undertow.handler.Router
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.*

/**
 * @author Stuart Douglas
 */
object Http2Server {

    private val STORE_PASSWORD = "password".toCharArray()

    @Throws(Exception::class)
    @JvmStatic fun main(args: Array<String>) {
        val version = System.getProperty("java.version")
        println("Java version " + version)
        if (version[0] == '1' && Integer.parseInt(version[2] + "") < 8) {
            println("This example requires Java 1.8 or later")
            println("The HTTP2 spec requires certain cyphers that are not present in older JVM's")
            println("See section 9.2.2 of the HTTP2 specification for details")
            System.exit(1)
        }

        val bindAddress = System.getProperty("bind.address", "0.0.0.0")
        val sslContext = createSSLContext(loadKeyStore("server.keystore"), loadKeyStore("server.truststore"))

        var handler: HttpHandler = Router()
        handler = handleException(handler)
        handler = header(handler, Headers.CONTENT_TYPE_STRING, "application/json")
        handler = redirectIfNotSecure(handler)
        handler = addUndertowTransport(handler)
        handler = learnPush(handler)
        handler = attachSession(handler)
        handler = log(handler)

        val server = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8080, bindAddress)
                .addHttpsListener(8443, bindAddress, sslContext)
                .setHandler(handler)
                .build()

        server.start()
        println("Server ist listening at https://localhost:8443")
    }

    private fun handleException(next: HttpHandler): HttpHandler {
        return HttpHandler { exchange ->
            try {
                next.handleRequest(exchange)
                if (exchange.statusCode == StatusCodes.METHOD_NOT_ALLOWED)
                    throw MethodNotAllowed("Method ${exchange.requestMethod} not allowed for ${exchange.requestPath}")
                if (exchange.statusCode == StatusCodes.NOT_FOUND)
                    throw NotFound("Route not found ${exchange.requestPath}")
            } catch (e: HttpError) {
                exchange.statusCode = e.statusCode
                exchange.responseSender.send(jacksonObjectMapper().writeValueAsString(HttpErrorMessage(e)))
            }
        }
    }

    private fun attachSession(handler: HttpHandler): HttpHandler {
        return SessionAttachmentHandler(handler, InMemorySessionManager("test"), SessionCookieConfig())
    }

    private fun addUndertowTransport(handler: HttpHandler): HttpHandler {
        return header(handler, "x-undertow-transport", ExchangeAttributes.transportProtocol())
    }

    private fun learnPush(handler: HttpHandler): HttpHandler {
        return LearningPushHandler(100, -1, handler)
    }

    private fun redirectIfNotSecure(handler: HttpHandler): HttpHandler {
        val redirect = HttpHandler { exchange ->
            exchange.responseHeaders.add(Headers.LOCATION, "https://${exchange.hostName}:${exchange.hostPort + 363}${exchange.relativePath}")
            exchange.statusCode = StatusCodes.TEMPORARY_REDIRECT
        }
        return predicate(secure(), handler, redirect)
    }

    private fun log(next: HttpHandler): HttpHandler {
        val dateFormat = SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]")
        return HttpHandler { exchange ->
            next.handleRequest(exchange)
            val statusColor = when (exchange.statusCode) {
                in 100..299 -> "1;32"
                in 300..399 -> "1;33"
                else -> "1;31"
            }
            val method = exchange.requestMethod.toString().padEnd(7)
            println("${dateFormat.format(Date())} \u001b[1;38;2;255;127;0m$method\u001b[0m \u001b[${statusColor}m${exchange.statusCode}\u001b[0m ${exchange.requestPath}")
        }
    }

    @Throws(Exception::class)
    private fun loadKeyStore(name: String): KeyStore {
        val storeLoc = System.getProperty(name)
        val stream: InputStream?
        if (storeLoc == null) {
            stream = Http2Server::class.java.classLoader.getResourceAsStream(name)
        } else {
            stream = Files.newInputStream(Paths.get(storeLoc))
        }

        if (stream == null) {
            throw RuntimeException("Could not load keystore $name")
        }
        stream.use { `is` ->
            val loadedKeystore = KeyStore.getInstance("JKS")
            loadedKeystore.load(`is`, password(name))
            return loadedKeystore
        }
    }

    internal fun password(name: String): CharArray {
        val pw = System.getProperty(name + ".password")
        return pw?.toCharArray() ?: STORE_PASSWORD
    }

    @Throws(Exception::class)
    private fun createSSLContext(keyStore: KeyStore, trustStore: KeyStore): SSLContext {
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, password("key"))
        val keyManagers = keyManagerFactory.keyManagers

        val trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        val trustManagers = trustManagerFactory.trustManagers

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustManagers, null)

        return sslContext
    }
}

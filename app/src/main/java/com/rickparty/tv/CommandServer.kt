package com.rickparty.tv

import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.lang.ref.WeakReference

class CommandServer(private val port: Int = 8080) : NanoHTTPD(port) {

    interface CommandListener {
        fun onNext()
        fun onGlitch()
        fun onChaosToggle()
        fun onBoost()
    }

    private var listenerRef: WeakReference<CommandListener?> = WeakReference(null)

    fun setListener(listener: CommandListener?) {
        listenerRef = WeakReference(listener)
    }

    override fun start() {
        try {
            super.start(SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        try {
            super.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun serve(session: NanoHTTPD.IHTTPSession?): NanoHTTPD.Response {
        val uri = session?.uri ?: "/"
        val listener = listenerRef.get()
        when (uri) {
            "/next" -> {
                listener?.onNext()
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            "/glitch" -> {
                listener?.onGlitch()
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            "/chaos" -> {
                listener?.onChaosToggle()
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            "/boost" -> {
                listener?.onBoost()
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            }
            "/" -> {
                val html = """
                    <!doctype html>
                    <html>
                      <head>
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <title>RickPartyTV Controls</title>
                        <style>
                          body { font-family: sans-serif; background:#111; color:#fff; display:flex; align-items:center; justify-content:center; height:100vh; margin:0; }
                          .card { display:grid; gap:12px; width:320px; }
                          button { padding:14px; font-size:16px; border-radius:8px; border:none; cursor:pointer; }
                          .btn1 { background:#ff6b6b; color:#000; }
                          .btn2 { background:#ffd166; color:#000; }
                          .btn3 { background:#6bcBff; color:#000; }
                          .btn4 { background:#8aff6b; color:#000; }
                        </style>
                      </head>
                      <body>
                        <div class="card">
                          <button class="btn1" onclick="fetch('/next')">Next Image</button>
                          <button class="btn2" onclick="fetch('/glitch')">Glitch</button>
                          <button class="btn3" onclick="fetch('/chaos')">Chaos Mode</button>
                          <button class="btn4" onclick="fetch('/boost')">Audio Boost</button>
                        </div>
                      </body>
                    </html>
                """.trimIndent()
                val resp = newFixedLengthResponse(Response.Status.OK, "text/html", html)
                resp.addHeader("Access-Control-Allow-Origin", "*")
                return resp
            }
            else -> {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }
        }
    }
}

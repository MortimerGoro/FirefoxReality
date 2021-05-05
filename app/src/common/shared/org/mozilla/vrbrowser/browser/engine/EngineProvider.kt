/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.browser.engine

import android.content.Context
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.mozilla.vrbrowser.browser.api.RuntimeAPI

object EngineProvider {

    private var client: HttpURLConnectionClient? = null
    private var runtime: RuntimeAPI? = null

    @Synchronized
    fun getOrCreateRuntime(context: Context): RuntimeAPI {
        if (runtime == null) {
            runtime = RuntimeAPI(context)
        }

        return runtime!!
    }

    @Synchronized
    fun isRuntimeCreated(): Boolean {
        return true
    }

    fun createClient(context: Context): HttpURLConnectionClient {
        return HttpURLConnectionClient()
    }

    fun getDefaultClient(context: Context): HttpURLConnectionClient {
        if (client == null) {
            client = createClient(context)
        }

        return client!!
    }

}

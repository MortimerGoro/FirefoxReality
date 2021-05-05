/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.browser.engine

import mozilla.components.concept.fetch.*
import java.io.IOException

class WPEFetchClient(
) : Client() {

    @Throws(IOException::class)
    override fun fetch(request: Request): Response {
        throw IOException("Not implemented")
    }

    companion object {
        const val MAX_READ_TIMEOUT_MINUTES = 5L
    }
}


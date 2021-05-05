/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.browser.components

import mozilla.components.concept.engine.CancellableOperation
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.*
import mozilla.components.concept.engine.webextension.WebExtension

class WPEWebExtensionRuntime: WebExtensionRuntime {
    /**
     * See [Engine.installWebExtension].
     */
    override fun installWebExtension(
            id: String,
            url: String,
            onSuccess: ((WebExtension) -> Unit),
            onError: ((String, Throwable) -> Unit)
    ): CancellableOperation {
        //onError("Not implemented", Throwable())
        return CancellableOperation.Noop()
    }

    /**
     * See [Engine.uninstallWebExtension].
     */
    override fun uninstallWebExtension(
            ext: WebExtension,
            onSuccess: () -> Unit,
            onError: (String, Throwable) -> Unit
    ) {

        //onError("Not implemented", Throwable())
    }

    /**
     * See [Engine.updateWebExtension].
     */
    override fun updateWebExtension(
            extension: WebExtension,
            onSuccess: (WebExtension?) -> Unit,
            onError: (String, Throwable) -> Unit
    ) {
        onError("Not implemented", Throwable())
    }

    /**
     * See [Engine.registerWebExtensionDelegate].
     */
    @Suppress("Deprecation")
    override fun registerWebExtensionDelegate(
            webExtensionDelegate: WebExtensionDelegate
    ) {
    }

    /**
     * See [Engine.listInstalledWebExtensions].
     */
    override fun listInstalledWebExtensions(onSuccess: (List<WebExtension>) -> Unit, onError: (Throwable) -> Unit) {
        onSuccess(emptyList())
    }

    /**
     * See [Engine.enableWebExtension].
     */
    override fun enableWebExtension(
            extension: WebExtension,
            source: EnableSource,
            onSuccess: (WebExtension) -> Unit,
            onError: (Throwable) -> Unit
    ) {
        onError(Throwable())
    }

    /**
     * See [Engine.disableWebExtension].
     */
    override fun disableWebExtension(
            extension: WebExtension,
            source: EnableSource,
            onSuccess: (WebExtension) -> Unit,
            onError: (Throwable) -> Unit
    ) {
        onError(Throwable())
    }

    /**
     * See [Engine.setAllowedInPrivateBrowsing].
     */
    override fun setAllowedInPrivateBrowsing(
            extension: WebExtension,
            allowed: Boolean,
            onSuccess: (WebExtension) -> Unit,
            onError: (Throwable) -> Unit
    ) {
        onError(Throwable())
    }

}
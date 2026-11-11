/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.flushbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process

/**
 * Restarts Corn's process after cookies were deleted from WebView's cookie file, so WebView
 * reloads the cleaned file instead of its in-memory copy (DuckDuckGo does the same after its
 * Fire Button). Runs in its own short-lived process (":restart" in the manifest): the main
 * process exits, this activity relaunches Corn and exits too.
 */
class RestartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Process.killProcess(intent.getIntExtra(EXTRA_PID, -1))
        val relaunch = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.getStringExtra(EXTRA_QUIP)?.let { relaunch.putExtra(EXTRA_QUIP, it) }
        intent.getStringExtra(EXTRA_TOAST)?.let { relaunch.putExtra(EXTRA_TOAST, it) }
        startActivity(relaunch)
        finish()
        Runtime.getRuntime().exit(0)
    }

    companion object {
        private const val EXTRA_PID = "flush.restart.pid"
        const val EXTRA_QUIP = "flush.quip"
        const val EXTRA_TOAST = "flush.toast"

        /** Relaunch Corn in a fresh process, optionally showing a Flush one-liner or a toast. */
        fun restart(context: Context, quip: String? = null, toast: String? = null) {
            val i = Intent(context, RestartActivity::class.java)
                .putExtra(EXTRA_PID, Process.myPid())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            quip?.let { i.putExtra(EXTRA_QUIP, it) }
            toast?.let { i.putExtra(EXTRA_TOAST, it) }
            context.startActivity(i)
            Runtime.getRuntime().exit(0)
        }

        /**
         * Background Auto-Flush: Android doesn't let an app relaunch itself from the background,
         * so just end the process; the next launch starts clean.
         */
        fun exitProcess() = Runtime.getRuntime().exit(0)
    }
}

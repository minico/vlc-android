/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package org.videolan.vlc.smbclient

import android.os.Build
import androidx.annotation.RequiresApi
import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMBApiException
import java.nio.file.AtomicMoveNotSupportedException

class ClientException : Exception {
    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)

    private val status: NtStatus? = (cause as? SMBApiException)?.status
    private val statusCode: Long? = (cause as? SMBApiException)?.statusCode

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(AtomicMoveNotSupportedException::class)
    fun maybeThrowAtomicMoveNotSupportedException(file: String?, other: String?) {
        if (status == NtStatus.STATUS_NOT_SAME_DEVICE) {
            throw AtomicMoveNotSupportedException(file, other, message)
                .apply { initCause(this@ClientException) }
        }
    }
}

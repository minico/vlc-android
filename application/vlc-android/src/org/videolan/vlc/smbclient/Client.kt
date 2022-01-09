/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */
package org.videolan.vlc.smbclient

import android.os.Build
import androidx.annotation.RequiresApi
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.common.SMBRuntimeException
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.NamedPipe
import com.hierynomus.smbj.share.PrinterShare
import com.hierynomus.smbj.share.Share
import java.io.IOException
import java.util.Collections
import java.util.WeakHashMap

object SambaClient {
    @Volatile
    lateinit var authenticator: Authenticator

    private val client = SMBClient()

    private val sessions = mutableMapOf<Authority, Session>()

    private val directoryFileInformationCache =
        Collections.synchronizedMap(WeakHashMap<SmbPath, FileInformation>())

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getFileLastModifiedDate(path: String): Long {
        val info = try {
            val smbPath: SmbPath = SmbPath("", path)
            getPathInformation(smbPath, false) as? FileInformation
        } catch (e: ClientException) {
            e.printStackTrace()
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Throws(ClientException::class)
    fun getPathInformation(path: SmbPath, openReparsePoint: Boolean): PathInformation {
        val sharePath = path
        val session = getSession(Authority("10.0.0.4", 445))
        if (sharePath.path.isEmpty()) {
            val share = getShare(session, sharePath.name)
            return when (share) {
                is DiskShare -> {
                    val shareInfo = try {
                        share.shareInformation
                    } catch (e: SMBRuntimeException) {
                        e.printStackTrace()
                        null
                    }
                    ShareInformation(ShareType.DISK, shareInfo)
                    // Don't close the disk share, because it might still be in use, or might become
                    // in use shortly. All shares are automatically closed when the session is
                    // closed anyway.
                }
                is NamedPipe -> ShareInformation(ShareType.PIPE, null).also { share.closeSafe() }
                is PrinterShare -> ShareInformation(ShareType.PRINTER, null)
                    .also { share.closeSafe() }
                else -> throw AssertionError(share)
            }
        } else {
            synchronized(directoryFileInformationCache) {
                directoryFileInformationCache[path]?.let {
                    if (openReparsePoint || !it.fileAttributes.hasBits(
                            FileAttributes.FILE_ATTRIBUTE_REPARSE_POINT.value
                        )) {
                        return it.also { directoryFileInformationCache -= path }
                    }
                }
            }
            val share = getDiskShare(session, sharePath.name)
            val diskEntry = try {
                share.open(
                    sharePath.path,
                    enumSetOf(AccessMask.FILE_READ_ATTRIBUTES, AccessMask.FILE_READ_EA),
                    null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN,
                    if (openReparsePoint) {
                        enumSetOf(SMB2CreateOptions.FILE_OPEN_REPARSE_POINT)
                    } else {
                        null
                    }
                )
            } catch (e: SMBRuntimeException) {
                throw ClientException(e)
            }
            val fileAllInformation = try {
                diskEntry.use { it.fileInformation }
            } catch (e: SMBRuntimeException) {
                throw ClientException(e)
            }
            return fileAllInformation.toFileInformation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @Throws(ClientException::class)
    private fun getSession(authority: Authority): Session {
        synchronized(sessions) {
            var session = sessions[authority]
            if (session != null) {
                val connection = session.connection
                if (connection.isConnected) {
                    return session
                } else {
                    session.closeSafe()
                    connection.closeSafe()
                    sessions -= authority
                }
            }
            //val authentication = authenticator.getAuthentication(authority)
            //    ?: throw ClientException("No authentication found for $authority")
            val authentication = Authentication("admin", "WorkGroup", "3753154")
            val hostAddress = authority.host
            val connection = try {
                client.connect(hostAddress, authority.port)
            } catch (e: IOException) {
                throw ClientException(e)
            }
            session = try {
                connection.authenticate(authentication.toContext())
            } catch (e: SMBRuntimeException) {
                // We need to close the connection here, otherwise future authentications reusing it
                // will receive an exception about no available credits.
                connection.closeSafe()
                throw ClientException(e)
            // TODO: kotlinc: Type mismatch: inferred type is Session? but TypeVariable(V) was
            //  expected
            //}
            }!!
            sessions[authority] = session
            return session
        }
    }

    @Throws(ClientException::class)
    private fun getShare(session: Session, shareName: String): Share {
        return try {
            session.connectShare(shareName)
        } catch (e: SMBRuntimeException) {
            throw ClientException(e)
        }
    }

    @Throws(ClientException::class)
    private fun getDiskShare(session: Session, shareName: String): DiskShare =
        getShare(session, shareName) as? DiskShare
            ?: throw ClientException("$shareName is not a DiskShare")

    data class SmbPath(
        val name: String,
        val path: String
    )
}

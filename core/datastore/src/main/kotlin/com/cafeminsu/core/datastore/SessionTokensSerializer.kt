package com.cafeminsu.core.datastore

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class SessionTokensSerializer @Inject constructor() : Serializer<SessionTokensProto> {
    override val defaultValue: SessionTokensProto = SessionTokensProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SessionTokensProto =
        SessionTokensProto.parseFrom(input)

    override suspend fun writeTo(t: SessionTokensProto, output: OutputStream) {
        t.writeTo(output)
    }
}

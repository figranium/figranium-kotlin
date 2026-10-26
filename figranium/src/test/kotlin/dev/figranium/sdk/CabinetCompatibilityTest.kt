package dev.figranium.sdk

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class CabinetCompatibilityTest {
    @Test fun listAndUnzipDecodeCoreV021Responses() = runBlocking {
        val paths = mutableListOf<String>()
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            paths.add(path)
            val body = when (path) {
                "/api/cabinets" ->
                    """{"defaultCabinetId":"cab_basic","cabinets":[{"id":"cab_basic","name":"Basic Cabinet","isDefault":true,"itemCount":2,"unuploadedCount":1,"createdAt":123}]}"""
                "/api/cabinets/cab_basic/items/item-1/unzip" ->
                    """{"id":"item-2","name":"extracted","kind":"folder","status":"unuploaded","size":42,"createdAt":456}"""
                else -> error("Unexpected path: $path")
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val client = Figranium(authentication = FigraniumAuthentication.Session, httpClient = transport)
        val cabinets = client.cabinets.list()
        assertEquals("cab_basic", cabinets.single().id)
        assertEquals(1, cabinets.single().unuploadedCount)

        val extracted = client.cabinets.unzipItem("cab_basic", "item-1")
        assertEquals("item-2", extracted.id)
        assertEquals("folder", extracted.kind)
        assertEquals(42, extracted.size)
        assertEquals(
            listOf("/api/cabinets", "/api/cabinets/cab_basic/items/item-1/unzip"),
            paths,
        )
    }
}

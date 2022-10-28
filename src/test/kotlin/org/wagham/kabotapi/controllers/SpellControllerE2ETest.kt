package org.wagham.kabotapi.controllers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wagham.db.models.Spell
import org.wagham.kabotapi.exceptions.ErrorResponsePayload
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

suspend fun StringSpec.testSpellE2ETest(
    url: String,
    client: HttpClient,
    guild: String,
    objectMapper: ObjectMapper
) {

    "Can get all the Spells" {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Guild-ID", guild)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val spells = objectMapper.readValue(response.body(), object : TypeReference<List<Spell>>(){})
        response.statusCode() shouldBe 200
        spells.size shouldBeGreaterThan 0
    }

    "Requesting the Spell from a non-existing guild results in 404" {
        val errorGuildId = "I_DO_NOT_EXIST"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Guild-ID", errorGuildId)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        response.statusCode() shouldBe 404
        val errorMessage = objectMapper.readValue(response.body(), object : TypeReference<ErrorResponsePayload>(){})
        errorMessage.status shouldNotBe null
        errorMessage.message shouldBe "Invalid Guild ID: $errorGuildId"
    }

    "Requesting the Spell with no Guild ID should result in 400" {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        response.statusCode() shouldBe 400
    }

}
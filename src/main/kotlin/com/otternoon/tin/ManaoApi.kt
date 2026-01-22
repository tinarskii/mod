package com.otternoon.tin

import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object ManaoApi {
  private const val API_BASE = "http://localhost:3000/api/link"
  private val client = HttpClient.newHttpClient()
  private val ALLOWED_ROLES = listOf("1229458138705432696")

  data class Result(val success: Boolean, val discordId: String? = null, val message: String)

  fun verify(code: String): Result {
    return try {
      val response = client.send(
        HttpRequest.newBuilder().uri(URI.create("$API_BASE/$code")).GET().build(),
        HttpResponse.BodyHandlers.ofString()
      )

      if (response.statusCode() != 200) return Result(false, message = "Invalid code")

      val data = JsonParser.parseString(response.body()).asJsonObject

      val discordId = data.getAsJsonObject("user").get("id").asString
      val roles = data.getAsJsonObject("roles").getAsJsonObject("member").getAsJsonArray("roles")

      if (roles.none { ALLOWED_ROLES.contains(it.asString) }) return Result(false, message = "You are not subbed!")

      client.send(HttpRequest.newBuilder().uri(URI.create("$API_BASE/$code/delete")).GET().build(), HttpResponse.BodyHandlers.ofString())

      Result(true, discordId, "Success")
    } catch (e: Exception) {
      Result(false, message = "Cannot connect to Manao, CONTACT ADMIN!!!\nError: ${e.message}")
    }
  }
}
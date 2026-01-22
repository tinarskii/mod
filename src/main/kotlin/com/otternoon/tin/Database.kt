package com.otternoon.tin

import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

object Database {
  private const val DB_URL = "jdbc:sqlite:scentcraft.db"

  fun init() {
    getConnection().use { conn ->
      val sql = """
                CREATE TABLE IF NOT EXISTS player_links (
                    uuid TEXT PRIMARY KEY,
                    discord_id TEXT NOT NULL,
                    expiry_time LONG NOT NULL
                )
            """.trimIndent()
      conn.createStatement().execute(sql)
    }
  }

  private fun getConnection(): Connection = DriverManager.getConnection(DB_URL)

  fun saveLink(uuid: UUID, discordId: String) {
    val expiry = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
    getConnection().use { conn ->
      val sql = "INSERT OR REPLACE INTO player_links(uuid, discord_id, expiry_time) VALUES(?, ?, ?)"
      val pstmt = conn.prepareStatement(sql)
      pstmt.setString(1, uuid.toString())
      pstmt.setString(2, discordId)
      pstmt.setLong(3, expiry)
      pstmt.executeUpdate()
    }
  }

  fun checkStatus(uuid: UUID): Int {
    getConnection().use { conn ->
      val sql = "SELECT expiry_time FROM player_links WHERE uuid = ?"
      val pstmt = conn.prepareStatement(sql)
      pstmt.setString(1, uuid.toString())
      val rs = pstmt.executeQuery()
      return if (rs.next()) {
        if (System.currentTimeMillis() > rs.getLong("expiry_time")) 2 else 0
      } else 1
    }
  }
}
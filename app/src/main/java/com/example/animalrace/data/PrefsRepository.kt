package com.example.animalrace.data

import android.content.Context
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.MiniGame
import com.example.animalrace.model.MiniGameResult
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
import com.example.animalrace.model.RankMode
import com.example.animalrace.model.RankSetting
import com.example.animalrace.model.SavedSetup
import com.example.animalrace.model.Venue
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences에 JSON 문자열로 저장. 기록이 10판뿐이라 DB 없이 충분하다. */
class PrefsRepository(context: Context) : AppRepository {
    private val prefs = context.getSharedPreferences("animal_race", Context.MODE_PRIVATE)

    override fun loadSetup(): SavedSetup? = runCatching {
        val json = JSONObject(prefs.getString(KEY_SETUP, null) ?: return null)
        val animals = json.getJSONArray("animals").let { arr ->
            (0 until arr.length()).mapNotNull { animalOf(arr.getString(it)) }
        }
        val rank = json.optJSONObject("rank")?.let { r ->
            RankSetting(
                mode = RankMode.entries.firstOrNull { it.name == r.optString("mode") } ?: RankMode.ROULETTE,
                count = if (r.has("count")) r.getInt("count") else null,
                fixed = r.optJSONArray("fixed")?.let { a -> (0 until a.length()).map { a.getInt(it) } }.orEmpty(),
            )
        } ?: RankSetting()
        fun strings(a: JSONArray?) = a?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }.orEmpty()
        val groups = json.optJSONArray("groups")?.let { arr -> (0 until arr.length()).map { strings(arr.optJSONArray(it)) } }.orEmpty()
        SavedSetup(animals, json.optString("bet"), json.optBoolean("randomRank"), rank, strings(json.optJSONArray("members")), groups)
    }.getOrNull()

    override fun saveSetup(setup: SavedSetup) {
        val json = JSONObject()
            .put("animals", JSONArray(setup.animals.map { it.name }))
            .put("bet", setup.bet)
            .put("randomRank", setup.randomRank)
            .put("rank", JSONObject().put("mode", setup.rank.mode.name).put("count", setup.rank.count).put("fixed", JSONArray(setup.rank.fixed)))
            .put("members", JSONArray(setup.members))
            .put("groups", JSONArray(setup.groups.map { JSONArray(it) }))
        prefs.edit().putString(KEY_SETUP, json.toString()).apply()
    }

    override fun loadSoundOn(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    override fun saveSoundOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, on).apply()
    }

    override fun loadMusicOn(): Boolean = prefs.getBoolean(KEY_MUSIC, true)

    override fun saveMusicOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC, on).apply()
    }

    override fun loadHistory(): List<RaceRecord> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, null) ?: return emptyList())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val res = o.getJSONArray("results")
            val results = (0 until res.length()).mapNotNull { j ->
                val r = res.getJSONObject(j)
                animalOf(r.getString("a"))?.let { RaceResult(it, r.getDouble("t"), r.optInt("u"), r.optInt("l", j), r.optString("w").ifEmpty { null }) }
            }
            val picks = o.optJSONArray("picks")?.let { p -> (0 until p.length()).map { p.getInt(it) } }
            // 3.11 기록은 "mollbang" 키로 저장돼 있다
            val miniGame = (o.optJSONObject("miniGame") ?: o.optJSONObject("mollbang"))?.let { m ->
                val game = MiniGame.entries.firstOrNull { it.name == m.optString("g") }
                val loser = animalOf(m.optString("a"))
                if (game != null && loser != null) MiniGameResult(game, loser, m.optString("w").ifEmpty { null }) else null
            }
            if (results.isEmpty()) null
            else RaceRecord(o.getLong("at"), o.optString("bet"), o.optInt("lead"), results,
                Venue.entries.firstOrNull { it.name == o.optString("venue") }, picks, miniGame, o.optBoolean("fixed"))
        }
    }.getOrDefault(emptyList())

    override fun saveHistory(history: List<RaceRecord>) {
        val arr = JSONArray()
        history.forEach { rec ->
            val results = JSONArray()
            rec.results.forEach { r ->
                results.put(JSONObject().put("a", r.animal.name).put("t", r.time).put("u", r.skillUses).put("l", r.lane).put("w", r.who))
            }
            arr.put(
                JSONObject().put("at", rec.at).put("bet", rec.bet).put("lead", rec.leadChanges).put("results", results)
                    .put("venue", rec.venue?.name)
                    .put("picks", rec.picks?.let { JSONArray(it) })
                    .put("fixed", rec.picksFixed)
                    .put("miniGame", rec.miniGame?.let { JSONObject().put("g", it.game.name).put("a", it.loser.name).put("w", it.who) }),
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun animalOf(name: String) = Animal.entries.firstOrNull { it.name == name }

    private companion object {
        const val KEY_SETUP = "setup"
        const val KEY_SOUND = "sound"
        const val KEY_MUSIC = "music"
        const val KEY_HISTORY = "history"
    }
}

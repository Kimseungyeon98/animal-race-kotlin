package com.example.animalrace.data

import android.content.Context
import com.example.animalrace.model.Animal
import com.example.animalrace.model.AppRepository
import com.example.animalrace.model.RaceRecord
import com.example.animalrace.model.RaceResult
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
        SavedSetup(animals, json.optString("bet"))
    }.getOrNull()

    override fun saveSetup(setup: SavedSetup) {
        val json = JSONObject()
            .put("animals", JSONArray(setup.animals.map { it.name }))
            .put("bet", setup.bet)
        prefs.edit().putString(KEY_SETUP, json.toString()).apply()
    }

    override fun loadHistory(): List<RaceRecord> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, null) ?: return emptyList())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val res = o.getJSONArray("results")
            val results = (0 until res.length()).mapNotNull { j ->
                val r = res.getJSONObject(j)
                animalOf(r.getString("a"))?.let { RaceResult(it, r.getDouble("t"), r.optInt("u"), r.optInt("l", j)) }
            }
            if (results.isEmpty()) null
            else RaceRecord(o.getLong("at"), o.optString("bet"), o.optInt("lead"), results,
                Venue.entries.firstOrNull { it.name == o.optString("venue") })
        }
    }.getOrDefault(emptyList())

    override fun saveHistory(history: List<RaceRecord>) {
        val arr = JSONArray()
        history.forEach { rec ->
            val results = JSONArray()
            rec.results.forEach { r ->
                results.put(JSONObject().put("a", r.animal.name).put("t", r.time).put("u", r.skillUses).put("l", r.lane))
            }
            arr.put(
                JSONObject().put("at", rec.at).put("bet", rec.bet).put("lead", rec.leadChanges).put("results", results)
                    .put("venue", rec.venue?.name),
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun animalOf(name: String) = Animal.entries.firstOrNull { it.name == name }

    private companion object {
        const val KEY_SETUP = "setup"
        const val KEY_HISTORY = "history"
    }
}

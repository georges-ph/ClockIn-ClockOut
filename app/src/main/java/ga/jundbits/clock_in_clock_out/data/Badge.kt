package ga.jundbits.clock_in_clock_out.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONException
import org.json.JSONObject

/** What a badge QR code carries. Name and department may be empty for an ID-only code. */
data class BadgeData(val id: String, val name: String, val department: String) {
    val isComplete: Boolean get() = id.isNotBlank() && name.isNotBlank() && department.isNotBlank()
}

object Badge {

    private val plainId = Regex("^[A-Za-z0-9._-]{1,64}$")

    /** One QR code per person: the gate reads the ID, an employee phone keeps all three fields. */
    fun encode(id: String, name: String, department: String): String =
        JSONObject().put("id", id).put("name", name).put("department", department).toString()

    fun encode(profile: Profile): String = encode(profile.id, profile.name, profile.department)

    /**
     * Reads a badge. Accepts the JSON written by every version of the app (app 2.0
     * wrote the ID as a number and added a "clocking" field, which is ignored) and a
     * code that holds only an ID, such as a printed card.
     */
    fun decode(text: String): BadgeData? {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) {
            return try {
                val json = JSONObject(trimmed)
                val id = json.optString("id").trim()
                if (id.isEmpty()) null
                else BadgeData(id, json.optString("name").trim(), json.optString("department").trim())
            } catch (e: JSONException) {
                null
            }
        }
        return if (plainId.matches(trimmed)) BadgeData(trimmed, "", "") else null
    }
}

/** The badge saved on an employee's phone. The preference names match app 2.0. */
class EmployeeStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)

    fun read(): BadgeData? =
        preferences.getString(KEY, null)?.let(Badge::decode)?.takeIf { it.isComplete }

    fun save(badge: BadgeData) {
        preferences.edit { putString(KEY, Badge.encode(badge.id, badge.name, badge.department)) }
    }

    private companion object {
        const val KEY = "profile"
    }
}

package space.crewpulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CrewMember(val badgeId: String, val name: String, val role: String)

@Serializable
private data class CrewRoster(val members: List<CrewMember>)

/**
 * NFC badge sign-in. A badge carries an id (NDEF text record, or the tag UID as "UID:..."),
 * and only ids in the base roster can sign in, so a random person with the app cannot.
 * Sign-in lives in memory only: the app asks for the badge on every launch.
 */
class BadgeRepository(private val context: android.content.Context) {

    private val _scans = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Badge ids read by the NFC reader (MainActivity) or by the demo buttons. */
    val scans: SharedFlow<String> = _scans

    private val _current = MutableStateFlow<CrewMember?>(null)
    val current: StateFlow<CrewMember?> = _current

    private var roster: List<CrewMember>? = null

    fun onScan(badgeId: String) {
        _scans.tryEmit(badgeId.trim())
    }

    /** Returns the crew member for this badge and signs them in, or null if the badge is unknown. */
    suspend fun signIn(badgeId: String): CrewMember? {
        val member = findInRoster(badgeId, loadRoster())
        _current.value = member
        return member
    }

    fun signOut() {
        _current.value = null
    }

    private suspend fun loadRoster(): List<CrewMember> = roster ?: withContext(Dispatchers.IO) {
        context.assets.open("crew_roster.json").bufferedReader().use { it.readText() }
            .let { Json { ignoreUnknownKeys = true }.decodeFromString<CrewRoster>(it).members }
    }.also { roster = it }
}

/** Pure lookup, for unit tests. Badge ids are compared case-insensitively. */
fun findInRoster(badgeId: String, roster: List<CrewMember>): CrewMember? =
    roster.firstOrNull { it.badgeId.equals(badgeId.trim(), ignoreCase = true) }

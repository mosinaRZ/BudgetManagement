package ir.hamedan.budgetmanagement.data.update

/**
 * Numeric major.minor.patch of the app ("1.2.0"). Missing parts count as zero ("1.2" == "1.2.0").
 * Versions are compared numerically, never as text, so "1.10.0" is newer than "1.9.0".
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = when {
        major != other.major -> major.compareTo(other.major)
        minor != other.minor -> minor.compareTo(other.minor)
        else -> patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        // Up to three numeric parts, optionally prefixed with "v" and followed by a suffix that
        // does not start with a digit or a dot ("1.2.0-debug"). "1.2.3.4" is rejected.
        private val PATTERN = Regex("""^[vV]?(\d{1,6})(?:\.(\d{1,6}))?(?:\.(\d{1,6}))?(?![\d.])""")

        /** Returns null when [raw] has no usable version, so callers can fail open. */
        fun parse(raw: String?): AppVersion? {
            val match = PATTERN.find(raw?.trim().orEmpty()) ?: return null
            val (major, minor, patch) = match.destructured
            return AppVersion(
                major = major.toInt(),
                minor = minor.ifEmpty { "0" }.toInt(),
                patch = patch.ifEmpty { "0" }.toInt()
            )
        }
    }
}

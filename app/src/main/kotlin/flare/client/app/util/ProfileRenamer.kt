package flare.client.app.util

import flare.client.app.data.model.ProfileEntity

object ProfileRenamer {

    fun renameProfiles(profiles: List<ProfileEntity>): List<ProfileEntity> {
        val counters = mutableMapOf<String, Int>()

        return profiles.map { profile ->
            val originalName = profile.name.trim()
            val (prefix, remaining) = extractEmojiPrefix(originalName)
            val country = determineCountry(remaining)

            val count = counters.getOrDefault(country, 0) + 1
            counters[country] = count

            val finalPrefix = if (prefix.isEmpty() && country == "AUTO") "🌐" else prefix
            val newName = if (finalPrefix.isNotEmpty()) "$finalPrefix $country #$count" else "$country #$count"

            profile.copy(name = newName)
        }
    }

    private fun extractEmojiPrefix(name: String): Pair<String, String> {
        var index = 0
        val sb = java.lang.StringBuilder()
        val length = name.length

        while (index < length) {
            val codePoint = name.codePointAt(index)
            if (isEmojiOrSymbol(codePoint)) {
                sb.appendCodePoint(codePoint)
                index += Character.charCount(codePoint)
            } else {
                break
            }
        }

        val prefix = sb.toString().trim()
        var remaining = name.substring(index).trimStart()

        
        if (remaining.startsWith("+") || remaining.startsWith("-") || remaining.startsWith("#") || 
            remaining.startsWith("|") || remaining.startsWith("/") || remaining.startsWith("\\")) {
            remaining = remaining.substring(1).trimStart()
        }

        return Pair(prefix, remaining)
    }

    private fun isEmojiOrSymbol(codePoint: Int): Boolean {
        if (codePoint in 0x1F1E6..0x1F1FF) return true 
        if (codePoint in 0x1F300..0x1F9FF) return true 
        if (codePoint in 0x1F600..0x1F6FF) return true 
        if (codePoint in 0x2700..0x27BF) return true 
        if (codePoint in 0x2600..0x26FF) return true 
        if (codePoint in 0x1F100..0x1F1FF) return true 
        if (codePoint == 0x200D || codePoint in 0x1F3FB..0x1F3FF) return true 
        if (codePoint in 0xFE00..0xFE0F) return true 
        return false
    }

    private fun determineCountry(remaining: String): String {
        val originalLower = remaining.lowercase()

        
        if (originalLower.contains("anycast") || originalLower.contains("auto")) {
            return "AUTO"
        }

        
        val cleaned = cleanRemaining(remaining)
        val lower = cleaned.lowercase()

        
        if (lower.contains("netherland") || lower.contains("holland") || lower.equals("nl")) return "Netherlands"
        if (lower.contains("austria") || lower.equals("at")) return "Austria"
        if (lower.contains("finland") || lower.equals("fi")) return "Finland"
        if (lower.contains("france") || lower.equals("fr")) return "France"
        if (lower.contains("germany") || lower.contains("deutschland") || lower.equals("de")) return "Germany"
        if (lower.contains("sweden") || lower.equals("se")) return "Sweden"
        if (lower.contains("australia") || lower.equals("au")) return "Australia"
        if (lower.contains("bulgaria") || lower.equals("bg")) return "Bulgaria"
        if (lower.contains("lithuania") || lower.equals("lt")) return "Lithuania"
        if (lower.contains("united states") || lower.contains("usa") || lower.equals("us")) return "USA"
        if (lower.contains("united kingdom") || lower.contains("uk") || lower.equals("gb")) return "UK"
        if (lower.contains("singapore") || lower.equals("sg")) return "Singapore"
        if (lower.contains("japan") || lower.equals("jp")) return "Japan"
        if (lower.contains("poland") || lower.equals("pl")) return "Poland"
        if (lower.contains("turkey") || lower.equals("tr")) return "Turkey"
        if (lower.contains("russia") || lower.equals("ru")) return "Russia"
        if (lower.contains("kazakhstan") || lower.equals("kz")) return "Kazakhstan"
        if (lower.contains("ukraine") || lower.equals("ua")) return "Ukraine"
        if (lower.contains("canada") || lower.equals("ca")) return "Canada"
        if (lower.contains("switzerland") || lower.equals("ch")) return "Switzerland"
        if (lower.contains("italy") || lower.equals("it")) return "Italy"
        if (lower.contains("spain") || lower.equals("es")) return "Spain"
        if (lower.contains("norway") || lower.equals("no")) return "Norway"

        
        if (cleaned.isNotBlank()) {
            return cleaned.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
        }

        return "Server"
    }

    private fun cleanRemaining(remaining: String): String {
        
        val separators = listOf("|", "-", "_", ",", "#", "/", "\\", ":", "[", "(")
        var part = remaining
        for (sep in separators) {
            val idx = part.indexOf(sep)
            if (idx != -1) {
                part = part.substring(0, idx)
            }
        }

        
        val sb = java.lang.StringBuilder()
        var lastWasSpace = false
        val length = part.length
        var index = 0
        while (index < length) {
            val codePoint = part.codePointAt(index)
            if (Character.isLetter(codePoint)) {
                sb.appendCodePoint(codePoint)
                lastWasSpace = false
            } else if (Character.isWhitespace(codePoint) || codePoint == ' '.code) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            }
            index += Character.charCount(codePoint)
        }

        var cleaned = sb.toString().trim()

        
        val keywords = listOf("server", "free", "node", "vpn", "plus", "premium", "fast")
        for (kw in keywords) {
            cleaned = cleaned.replace(Regex("(?i)\\b$kw\\b"), "").trim()
        }

        
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        return cleaned
    }
}

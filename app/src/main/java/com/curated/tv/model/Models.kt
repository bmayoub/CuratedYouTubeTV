package com.curated.tv.model

data class Channel(
    val name: String,
    val handle: String,
    val channelId: String,
    val url: String,
    val category: String = "عام",
    val enabled: Boolean = true
)

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedDate: String,
    val thumbnailUrl: String,
    val description: String = ""
)

data class ChannelFeed(
    val channel: Channel,
    val videos: List<Video>
)

data class Category(
    val id: String,
    val title: String,
    val icon: String,
    val keywords: List<String> = emptyList()
) {
    companion object {
        val ALL = Category("all", "الكل", "🏠")
        val RANDOM = Category("random", "عشوائي", "🎲")
        val SPACE = Category(
            id = "space",
            title = "فضاء وفلك",
            icon = "🌌",
            keywords = listOf(
                "فضاء", "فلك", "كوكب", "كواكب", "مجرة", "مجرات", "ناسا", "نجوم", "شمس", "قمر",
                "ثقب أسود", "كون", "كوني", "مشتري", "مريخ", "جاذبية", "تلسكوب", "صاروخ",
                "space", "astronomy", "nasa", "galaxy", "planet", "universe", "cosmos"
            )
        )
        val NATURE = Category(
            id = "nature",
            title = "طبيعة وحياة",
            icon = "🌿",
            keywords = listOf(
                "طبيعة", "حيوان", "حيوانات", "بحار", "محيط", "محيطات", "غابة", "غابات", "بيئة",
                "افتراس", "كائنات", "طيور", "أسد", "أعماق", "برية", "حشرات", "ديناصور",
                "nature", "wildlife", "ocean", "animals", "forest", "earth", "creatures"
            )
        )
        val PHILOSOPHY = Category(
            id = "philosophy",
            title = "فلسفة وفكر",
            icon = "🧠",
            keywords = listOf(
                "فلسفة", "فلسفي", "فكر", "وعي", "تأمل", "نيتشه", "سقراط", "أفلاطون", "وجودية",
                "عقل", "منطق", "أخلاق", "رواقية", "معنى", "حكمة", "نفسي", "نفس",
                "philosophy", "consciousness", "mind", "stoic", "existentialism", "thinking"
            )
        )
        val HISTORY = Category(
            id = "history",
            title = "تاريخ وحضارة",
            icon = "📜",
            keywords = listOf(
                "تاريخ", "تاريخي", "حضارة", "حضارات", "حرب", "حروب", "إمبراطورية", "عصر", "أندلس",
                "عثماني", "رومان", "فرعون", "فراعنة", "وثائقي", "مملكة", "ثورة", "ملك",
                "history", "ancient", "war", "civilization", "empire", "historical"
            )
        )
        val SCIENCE_TECH = Category(
            id = "science_tech",
            title = "علوم وتكنولوجيا",
            icon = "🔬",
            keywords = listOf(
                "تكنولوجيا", "تقنية", "ذكاء اصطناعي", "برمجة", "حاسوب", "كمبيوتر", "علم", "علوم",
                "فيزياء", "كيمياء", "اختراع", "روبوت", "هندسة", "علماء", "نظرية",
                "tech", "technology", "science", "ai", "physics", "robot"
            )
        )

        val DEFAULT_CATEGORIES = listOf(ALL, RANDOM, SPACE, NATURE, PHILOSOPHY, HISTORY, SCIENCE_TECH)
    }

    fun matches(video: Video, channelCategory: String = ""): Boolean {
        if (id == "all" || id == "random") return true

        // 1. مطابقة تصنيف القناة
        if (channelCategory.isNotBlank()) {
            val normCategory = channelCategory.lowercase()
            if (normCategory.contains(title.lowercase()) || title.lowercase().contains(normCategory)) {
                return true
            }
            if (keywords.any { normCategory.contains(it) }) {
                return true
            }
        }

        // 2. مطابقة عنوان ووصف الفيديو
        val content = "${video.title} ${video.description}".lowercase()
        return keywords.any { content.contains(it) }
    }
}

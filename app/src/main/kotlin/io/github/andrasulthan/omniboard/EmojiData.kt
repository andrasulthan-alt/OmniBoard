package io.github.andrasulthan.omniboard

import android.graphics.Paint

object EmojiData {

    class Category(val name: String, val icon: String, raw: String) {
        val all: List<String> = raw.trim().split(Regex("\\s+"))
    }

    val categories = listOf(
        Category(
            "smileys", "😀",
            """
            😀 😃 😄 😁 😆 😅 🤣 😂 🙂 🙃 😉 😊 😇 🥰 😍 🤩 😘 😗 😚 😙 😋 😛 😜 🤪 😝 🤑
            🤗 🤭 🤫 🤔 🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 😌 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🤧 🥵 🥶
            🥴 😵 🤯 🤠 🥳 😎 🤓 🧐 😕 😟 🙁 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣
            😞 😓 😩 😫 🥱 😤 😡 😠 🤬 😈 👿 💀 💩 🤡 👻 👽 🤖 😺 😸 😹 😻 😼 😽 🙀 😿 😾
            🙈 🙉 🙊 👋 🤚 ✋ 🖖 👌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏
            🙌 👐 🤲 🤝 🙏 ✍️ 💅 💪 👀 👅 👄 💋 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵
            """
        ),
        Category(
            "nature", "🐶",
            """
            🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴
            🦄 🐝 🐛 🦋 🐌 🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍
            🐘 🦏 🐪 🐫 🦒 🐄 🐎 🐖 🐑 🐐 🦌 🐕 🐈 🐓 🦃 🦚 🦜 🕊️ 🐇 🐿️ 🦔 🐾 🌵 🎄 🌲 🌳
            🌴 🌱 🌿 ☘️ 🍀 🍁 🍂 🍃 🌷 🌹 🥀 🌺 🌸 🌼 🌻 🌞 🌝 🌚 🌙 ⭐ 🌟 ✨ ⚡ 🔥 🌈 ☀️
            ⛅ ☁️ 🌧️ ⛈️ ❄️ ☃️ 🌊 💧
            """
        ),
        Category(
            "food", "🍎",
            """
            🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥬 🥒 🌶️ 🌽 🥕 🧄
            🧅 🥔 🍠 🥐 🍞 🥖 🥨 🧀 🥚 🍳 🥞 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲
            🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍘 🍥 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🥜
            🍯 🥛 ☕ 🍵 🧃 🥤 🧋 🍶 🍺 🍻 🥂 🍷 🍹 🧊 🥢 🍽️ 🍴 🥄
            """
        ),
        Category(
            "activity", "⚽",
            """
            ⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🏒 🏏 ⛳ 🏹 🎣 🥊 🥋 🛹 ⛸️ 🎿 🏂 🏋️ 🤸 ⛹️
            🤺 🏌️ 🏇 🧘 🏄 🏊 🚴 🚵 🏆 🥇 🥈 🥉 🏅 🎖️ 🎫 🎪 🎭 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺
            🎸 🎻 🎲 🎯 🎳 🎮 🧩
            """
        ),
        Category(
            "travel", "✈️",
            """
            🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🚚 🚛 🚜 🛴 🚲 🛵 🏍️ 🚨 🚃 🚄 🚅 🚂 🚆 🚇 🚉 ✈️
            🛫 🛬 🚀 🛸 🚁 🛶 ⛵ 🚤 🛳️ 🚢 ⚓ ⛽ 🚧 🚦 🗺️ 🗿 🗽 🗼 🏰 🏯 🏟️ 🎡 🎢 🎠 ⛲ ⛱️
            🏖️ 🏝️ 🏜️ 🌋 ⛰️ 🏔️ 🗻 🏕️ ⛺ 🏠 🏡 🏢 🏬 🏥 🏦 🏨 🏪 🏫 💒 🏛️ ⛪ 🕌 🕍 🕋 🌅 🌄
            🌠 🎇 🎆 🌇 🌆 🏙️ 🌃 🌌 🌉
            """
        ),
        Category(
            "objects", "💡",
            """
            ⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💾 💿 📷 📸 📹 🎥 📞 ☎️ 📺 📻 🎙️ ⏰ ⏳ 📡 🔋 🔌 💡 🔦 🕯️
            💸 💵 💰 💳 💎 ⚖️ 🔧 🔨 🛠️ 🔩 ⚙️ 🧱 🔮 🔭 🔬 💊 💉 🧬 🧹 🧺 🧻 🚿 🛁 🧼 🔑 🗝️
            🚪 🛋️ 🛏️ 🧸 🖼️ 🛍️ 🛒 🎁 🎈 🎀 🎊 🎉 🏮 ✉️ 📩 📧 💌 📦 📜 📄 📊 📈 📉 📅 📆 📋
            📁 📂 📰 📓 📒 📕 📗 📘 📙 📚 📖 🔖 🔗 📎 📐 📏 📌 📍 ✂️ 🖊️ ✒️ 🖌️ 🖍️ 📝 ✏️ 🔍
            🔒 🔓
            """
        ),
        Category(
            "symbols", "❤️",
            """
            ❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ☮️ ☯️ ♈ ♉ ♊ ♋ ♌
            ♍ ♎ ♏ ♐ ♑ ♒ ♓ ❌ ⭕ 🛑 ⛔ 🚫 💯 💢 ♨️ ❗ ❓ ‼️ ⁉️ ⚠️ ♻️ ✅ ❎ 🌀 💤 ♿ 🅿️ 🆗 🆙
            🆒 🆕 🆓 🆘 0️⃣ 1️⃣ 2️⃣ 3️⃣ 4️⃣ 5️⃣ 6️⃣ 7️⃣ 8️⃣ 9️⃣ 🔟 #️⃣ *️⃣ ▶️ ⏸️ ⏹️ ⏭️ ⏮️ ⏩ ⏪ 🔀 🔁
            🔂 ◀️ 🔼 🔽 ➡️ ⬅️ ⬆️ ⬇️ ↗️ ↘️ ↙️ ↖️ ↕️ ↔️ ↪️ ↩️ 🔄 🎵 🎶 ➕ ➖ ➗ ✖️ 💲 ™️ ©️
            ®️ ✔️ ☑️ 🔘 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🟤 🔺 🔻 🔸 🔹 🔶 🔷 ⬛ ⬜
            """
        ),
        Category(
            "flags", "🏁",
            """
            🏁 🚩 🎌 🏴 🏳️ 🏳️‍🌈 🇮🇩 🇲🇾 🇸🇬 🇧🇳 🇹🇭 🇵🇭 🇻🇳 🇯🇵 🇰🇷 🇨🇳 🇮🇳 🇸🇦 🇦🇪 🇹🇷 🇪🇬 🇬🇧
            🇺🇸 🇨🇦 🇦🇺 🇳🇿 🇩🇪 🇫🇷 🇮🇹 🇪🇸 🇳🇱 🇧🇷 🇦🇷 🇲🇽 🇷🇺 🇿🇦 🇳🇬
            """
        ),
    )

    private val paint = Paint()
    private val cache = HashMap<Int, List<String>>()

    /** Emoji of a category that this phone can actually display (older phones hide newer emoji). */
    fun supported(index: Int): List<String> = cache.getOrPut(index) {
        categories[index].all.filter { paint.hasGlyph(it) }
    }
}

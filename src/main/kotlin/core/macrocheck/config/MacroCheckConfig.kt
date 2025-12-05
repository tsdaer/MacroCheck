package core.macrocheck.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

/**
 * 宏检查配置
 */
@State(
    name = "MacroCheckConfig",
    storages = [Storage("MacroCheck.xml")]
)
class MacroCheckConfig : PersistentStateComponent<MacroCheckConfig.State> {

    data class State(
        // 白名单宏列表（宏名称 + 启用状态）
        @get:XCollection(style = XCollection.Style.v2)
        @get:Tag("whitelistMacros")
        var whitelistMacros: MutableList<MacroEntry> = mutableListOf(
            MacroEntry("DEBUG", true),
            MacroEntry("NDEBUG", true),
            MacroEntry("_DEBUG", true),
            MacroEntry("__cplusplus", true),
            MacroEntry("__FILE__", true),
            MacroEntry("__LINE__", true),
            MacroEntry("__DATE__", true),
            MacroEntry("__TIME__", true),
            MacroEntry("__STDC__", true),
            MacroEntry("__STDC_VERSION__", true)
        ),

        // 是否启用白名单功能
        @get:Tag("enableWhitelist")
        var enableWhitelist: Boolean = true
    )

    data class MacroEntry(
        @get:Tag("name")
        var name: String = "",

        @get:Tag("enabled")
        var enabled: Boolean = true
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MacroEntry

            return name.equals(other.name, ignoreCase = true)
        }

        override fun hashCode(): Int {
            return name.lowercase().hashCode()
        }
    }

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        // 在加载状态时去重
        val uniqueMacros = mutableListOf<MacroEntry>()
        val seenNames = mutableSetOf<String>()

        for (entry in state.whitelistMacros) {
            val normalizedName = entry.name.trim()
            if (normalizedName.isNotEmpty() && seenNames.add(normalizedName.lowercase())) {
                // 保留第一个出现的宏，并更新其名称（去除空格）
                entry.name = normalizedName
                uniqueMacros.add(entry)
            }else {
                // 记录重复的宏（可选）
                println("Duplicate macro names found: '$normalizedName', skipped.")
            }
        }

        state.whitelistMacros = uniqueMacros
        XmlSerializerUtil.copyBean(state, this.state)
    }

    // 获取所有启用的宏名称
    fun getEnabledMacros(): Set<String> {
        return state.whitelistMacros
            .filter { it.enabled }
            .map { it.name }
            .toSet()
    }

    // 获取所有宏（包括启用状态） - 返回 Pair<宏名称, 启用状态>
    fun getAllMacros(): List<Pair<String, Boolean>> {
        return state.whitelistMacros.map { it.name to it.enabled }
    }

    // 设置所有宏 - 接收 Pair<宏名称, 启用状态>，自动去重
    fun setAllMacros(macros: List<Pair<String, Boolean>>) {
        val uniqueMacros = mutableListOf<MacroEntry>()
        val seenNames = mutableSetOf<String>()

        for ((name, enabled) in macros) {
            val normalizedName = name.trim()
            if (normalizedName.isNotEmpty() && seenNames.add(normalizedName.lowercase())) {
                uniqueMacros.add(MacroEntry(normalizedName, enabled))
            }
        }

        state.whitelistMacros = uniqueMacros
    }

    // 是否启用白名单功能
    fun isEnableWhitelist(): Boolean = state.enableWhitelist

    fun setEnableWhitelist(enabled: Boolean) {
        state.enableWhitelist = enabled
    }

    companion object {
        fun getInstance(project: Project): MacroCheckConfig {
            return project.getService(MacroCheckConfig::class.java)
        }
    }
}
package core.macrocheck.i18n

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * 本地化资源包
 */
class MacroCheckBundle : DynamicBundle(BUNDLE_NAME) {
    
    companion object {
        @NonNls
        private const val BUNDLE_NAME = "messages.MacroCheckBundle"
        
        private val INSTANCE = MacroCheckBundle()
        
        /**
         * 获取本地化字符串
         */
        fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
            return INSTANCE.getMessage(key, *params)
        }
        
        /**
         * 获取本地化字符串，带默认值
         */
        fun messageOrDefault(
            @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
            defaultValue: String,
            vararg params: Any
        ): String {
            return INSTANCE.messageOrDefault(key, defaultValue, *params) ?: defaultValue
        }
    }
}
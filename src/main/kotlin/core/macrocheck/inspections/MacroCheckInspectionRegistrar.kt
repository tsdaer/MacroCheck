package core.macrocheck.inspections

import com.intellij.codeInspection.InspectionToolProvider
import com.intellij.codeInspection.LocalInspectionTool
import core.macrocheck.inspections.MacroDefineUndefInspection

/**
 * 注册宏定义检查器
 */
class MacroCheckInspectionRegistrar : InspectionToolProvider {
    override fun getInspectionClasses(): Array<out Class<out LocalInspectionTool?>?> {
        return arrayOf(MacroDefineUndefInspection::class.java)
    }
}
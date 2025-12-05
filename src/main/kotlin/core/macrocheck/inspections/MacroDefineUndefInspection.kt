package core.macrocheck.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import core.macrocheck.config.MacroCheckConfig
import core.macrocheck.i18n.MacroCheckBundle
import org.jetbrains.annotations.NotNull

/**
 * 检查 #define 宏定义后是否有对应的 #undef
 */
class MacroDefineUndefInspection : LocalInspectionTool() {

    @NotNull
    override fun buildVisitor(@NotNull holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val config = MacroCheckConfig.getInstance(project)

        // 获取启用的宏名称
        val enabledMacros = config.getEnabledMacros()

        return MacroDefineUndefVisitor(holder, config.isEnableWhitelist(), enabledMacros)
    }

    @NotNull
    override fun getDisplayName(): String {
        return MacroCheckBundle.message("inspection.name")
    }

    @NotNull
    override fun getShortName(): String {
        return "MacroDefineUndef"
    }

    @NotNull
    override fun getGroupDisplayName(): String {
        return MacroCheckBundle.message("inspection.group")
    }

    override fun isEnabledByDefault(): Boolean {
        return true
    }

    private fun parseWhitelist(whitelistStr: String): Set<String> {
        return whitelistStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private class MacroDefineUndefVisitor(
        private val holder: ProblemsHolder,
        private val enableWhitelist: Boolean,
        private val whitelist: Set<String>
    ) : PsiElementVisitor() {
        private val macroDefines: MutableMap<String?, PsiElement?> = HashMap()
        private val macroUndefs: MutableSet<String?> = HashSet()

        override fun visitFile(file: PsiFile) {
            // 收集文件中的所有宏定义和取消定义
            collectMacros(file)


            // 检查每个宏定义是否有对应的 #undef
            for (entry in macroDefines.entries) {
                val macroName = entry.key
                val defineElement: PsiElement = entry.value!!

                // 检查是否在白名单中
                if (enableWhitelist && macroName != null && whitelist.contains(macroName)) {
                    continue // 跳过白名单中的宏
                }
                if (!macroUndefs.contains(macroName)) {
                    // 找到宏定义的位置
                    val range: TextRange? = defineElement.textRange
                    if (range != null) {
                        macroName?.let {
                            holder.registerProblem(
                                defineElement,
                                MacroCheckBundle.message("inspection.problem.description", macroName),
                                AddUndefQuickFix(it)
                            )
                        }
                    }
                }
            }


            // 清空集合，为下一个文件做准备
            macroDefines.clear()
            macroUndefs.clear()
        }

        fun collectMacros(element: PsiElement) {
            // 遍历所有子元素，查找宏定义和取消定义
            for (child in element.children) {
                val text = child.text

                if (text.startsWith("#define")) {
                    // 提取宏名
                    val parts: Array<String?> =
                        text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (parts.size >= 2) {
                        var macroName = parts[1]!!.trim { it <= ' ' }
                        // 移除可能的括号
                        if (macroName.contains("(")) {
                            macroName = macroName.substringBefore('(')
                        }
                        macroDefines[macroName] = child
                    }
                } else if (text.startsWith("#undef")) {
                    // 提取宏名
                    val parts: Array<String?> =
                        text.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (parts.size >= 2) {
                        val macroName = parts[1]!!.trim { it <= ' ' }
                        macroUndefs.add(macroName)
                    }
                }


                // 递归处理子元素
                collectMacros(child)
            }
        }
    }
}
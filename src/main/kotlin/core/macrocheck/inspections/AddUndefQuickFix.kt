package core.macrocheck.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import core.macrocheck.i18n.MacroCheckBundle
import org.jetbrains.annotations.NotNull

/**
 * 快速修复：为缺少 #undef 的宏添加 #undef 语句
 */
class AddUndefQuickFix(private val macroName: String) : LocalQuickFix {
    @NotNull
    override fun getName(): String {
        return MacroCheckBundle.message("inspection.quickfix.name", macroName)
    }

    @NotNull
    override fun getFamilyName(): String {
        return MacroCheckBundle.message("inspection.quickfix.family")
    }

    override fun applyFix(@NotNull project: Project, @NotNull descriptor: ProblemDescriptor) {
        WriteCommandAction.runWriteCommandAction(project) {
            val element = descriptor.psiElement ?: return@runWriteCommandAction

            val file = element.containingFile ?: return@runWriteCommandAction

            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return@runWriteCommandAction

            // 在文件末尾添加 #undef 语句
            val undefStatement = "#undef $macroName\n"

            // 插入到文件末尾
            document.insertString(document.textLength, undefStatement)

            // 提交文档更改
            PsiDocumentManager.getInstance(project).commitDocument(document)

            // 格式化代码
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    CodeStyleManager.getInstance(project).reformat(file)
                }
            }
        }
    }
}
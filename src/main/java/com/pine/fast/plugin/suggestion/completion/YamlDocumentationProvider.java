package com.pine.fast.plugin.suggestion.completion;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        System.out.println("generateDoc");
        return super.generateDoc(element, originalElement);
    }

    /*
     * This will called if the user tries to lookup documentation for the choices being shown (ctrl+q within suggestion dialog)
     */
    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object,
                                                           @Nullable PsiElement element) {
        System.out.println("getDocumentationElementForLookupItem");
        return super.getDocumentationElementForLookupItem(psiManager, object, element);
    }

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                    @Nullable PsiElement element) {
        System.out.println("getCustomDocumentationElement");
        return super.getCustomDocumentationElement(editor, file, element);
    }
}

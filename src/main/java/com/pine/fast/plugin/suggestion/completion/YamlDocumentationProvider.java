package com.pine.fast.plugin.suggestion.completion;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        System.out.println("generateDoc");
        return super.generateDoc(element, originalElement);
    }
}

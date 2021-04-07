package com.pine.fast.plugin.misc;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForFile;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.openapi.roots.ModuleRootManager.getInstance;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author pine
 */
@UtilityClass
public class PsiCustomUtil {

    private static final Logger log = Logger.getInstance(PsiCustomUtil.class);

    @Nullable
    public static Module findModule(@NotNull PsiElement element) {
        return findModuleForPsiElement(element);
    }

    @Nullable
    public static Module findModule(@NotNull InsertionContext context) {
        return findModuleForFile(context.getFile().getVirtualFile(), context.getProject());
    }


    @Nullable
    public static VirtualFile findFileUnderRootInModule(Module module, String targetFileName) {
        VirtualFile[] contentRoots = getInstance(module).getContentRoots();
        for (VirtualFile contentRoot : contentRoots) {
            VirtualFile childFile = findFileUnderRootInModule(contentRoot, targetFileName);
            if (childFile != null) {
                return childFile;
            }
        }
        return null;
    }

    @Nullable
    public static VirtualFile findFileUnderRootInModule(@NotNull VirtualFile contentRoot,
                                                        String targetFileName) {
        VirtualFile childFile = contentRoot.findChild(targetFileName);
        if (childFile != null) {
            return childFile;
        }
        return null;
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix For eg., to enable
     * debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private static void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }

}

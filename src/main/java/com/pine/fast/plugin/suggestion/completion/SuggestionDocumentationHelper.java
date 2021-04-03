package com.pine.fast.plugin.suggestion.completion;

import com.intellij.openapi.module.Module;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public interface SuggestionDocumentationHelper extends SuggestionNodeTypeProvider {

    @Nullable
    String getOriginalName();

    @NotNull
    Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                     List<SuggestionNode> matchesRootTillMe, int numOfAncestors);

    /**
     * @return false if an intermediate node (neither group, nor property, nor class). true otherwise
     */
    boolean supportsDocumentation();

    @NotNull
    String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited);

}

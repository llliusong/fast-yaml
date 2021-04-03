package com.pine.fast.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.completion.FileType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ClassSuggestionNode extends SuggestionNode {

    @NotNull
    Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                     List<SuggestionNode> matchesRootTillMe, int numOfAncestors);
}

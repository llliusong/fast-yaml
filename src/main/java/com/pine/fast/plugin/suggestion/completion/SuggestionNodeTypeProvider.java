package com.pine.fast.plugin.suggestion.completion;

import com.intellij.openapi.module.Module;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

public interface SuggestionNodeTypeProvider {

    /**
     * @param module module to which this node belongs
     * @return type of node
     */
    @NotNull
    SuggestionNodeType getSuggestionNodeType(Module module);
}

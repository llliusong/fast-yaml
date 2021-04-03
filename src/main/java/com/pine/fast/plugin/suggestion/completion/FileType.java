package com.pine.fast.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.pine.fast.plugin.suggestion.handler.YamlKeyInsertHandler;
import com.pine.fast.plugin.suggestion.handler.YamlValueInsertHandler;

// TODO: Add properties support
public enum FileType {
    yaml, properties;

    public InsertHandler<LookupElement> newKeyInsertHandler() {
        switch (this) {
            case yaml:
                return new YamlKeyInsertHandler();
            default:
                return null;
        }
    }

    public InsertHandler<LookupElement> newValueInsertHandler() {
        switch (this) {
            case yaml:
                return new YamlValueInsertHandler();
            default:
                return null;
        }
    }

}

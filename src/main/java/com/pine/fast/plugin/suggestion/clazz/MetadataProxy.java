package com.pine.fast.plugin.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.completion.FileType;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Since the class documentation is tied to the actual class being available in the classpath of the target project the
 * plugin is operating on, it is possible that the target class is no longer available in the classpath due to change of
 * dependencies. So, lets always access target via the proxy so that we dont have to worry about whether the target
 * class exists in classpath or not
 */
public interface MetadataProxy {

    @Nullable
    List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                   List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                   int pathSegmentStartIndex);

    @Nullable
    SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                           List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
                                                           String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
                                                           @Nullable Set<String> siblingsToExclude);

    @Nullable
    SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                        List<SuggestionNode> matchesRootTillMe, String prefix);

    @Nullable
    SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                        List<SuggestionNode> matchesRootTillMe, String prefix,
                                                        @Nullable Set<String> siblingsToExclude);

    boolean isLeaf(Module module);

    @NotNull
    SuggestionNodeType getSuggestionNodeType(Module module);

}

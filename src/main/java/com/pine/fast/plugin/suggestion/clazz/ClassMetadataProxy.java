package com.pine.fast.plugin.suggestion.clazz;

import static com.intellij.psi.util.CachedValuesManager.getCachedValue;
import static java.util.Objects.requireNonNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import com.pine.fast.plugin.misc.PsiCustomUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.completion.FileType;
import com.pine.fast.plugin.suggestion.completion.SuggestionDocumentationHelper;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

public class ClassMetadataProxy implements MetadataProxy {

    private static final Logger log = Logger.getInstance(ClassMetadataProxy.class);

    private static ConcurrentMap<String, Key<CachedValue<ClassMetadata>>> fqnToKey =
            ContainerUtil.newConcurrentMap();

    @NotNull
    private final PsiClass targetClass;

    @NotNull
    private final PsiClassType type;

    ClassMetadataProxy(@NotNull PsiClassType type) {
        this.type = type;
        targetClass = requireNonNull(PsiCustomUtil.toValidPsiClass(type));
    }

    @Nullable
    @Override
    public SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment) {
        return doWithTargetAndReturn(module, target -> target.findDirectChild(module, pathSegment),
                null);
    }

    @Nullable
    @Override
    public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix) {
        return findDirectChildrenForQueryPrefix(module, querySegmentPrefix, null);
    }

    @Nullable
    @Override
    public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
            Module module, String querySegmentPrefix, @Nullable Set<String> siblingsToExclude) {
        return doWithTargetAndReturn(module, target -> target
                .findDirectChildrenForQueryPrefix(module, querySegmentPrefix, siblingsToExclude), null);
    }

    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                          List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                          int pathSegmentStartIndex) {
        return doWithTargetAndReturn(module, target -> target
                .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                        pathSegmentStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex) {
        return doWithTargetAndReturn(module, target -> target
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
        return doWithTargetAndReturn(module, target -> target
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix) {
        return findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix, null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
                                                               List<SuggestionNode> matchesRootTillMe, String prefix,
                                                               @Nullable Set<String> siblingsToExclude) {
        return doWithTargetAndReturn(module, target -> target
                .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                        siblingsToExclude), null);
    }

    @Nullable
    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                           String originalValue) {
        return doWithTargetAndReturn(module,
                target -> target.getDocumentationForValue(module, nodeNavigationPathDotDelimited,
                        originalValue),
                null);
    }

    @Override
    public boolean isLeaf(Module module) {
        return doWithTargetAndReturn(module, target -> target.isLeaf(module), true);
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        return doWithTargetAndReturn(module, ClassMetadata::getSuggestionNodeType, SuggestionNodeType.UNKNOWN_CLASS);
    }

    @Nullable
    @Override
    public PsiType getPsiType(Module module) {
        return doWithTargetAndReturn(module, target -> target.getPsiType(module), null);
    }

    @Override
    public boolean targetRepresentsArray() {
        return false;
    }

    @Override
    public boolean targetClassRepresentsIterable(Module module) {
        return doWithTargetAndReturn(module, IterableClassMetadata.class::isInstance, false);
    }

    <T> T doWithTargetAndReturn(Module module,
                                TargetInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
        ClassMetadata target = getTarget(module);
        if (target != null) {
            return targetInvokerWithReturnValue.invoke(target);
        }
        return defaultReturnValue;
    }

    private ClassMetadata getTarget(Module module) {
        String fqn = PsiCustomUtil.typeToFqn(module, type);
        if (fqn != null) {
            String userDataKeyRef = "yaml_plugin_class_metadata:" + fqn;
            Key<CachedValue<ClassMetadata>> classMetadataKey =
                    ConcurrencyUtil.cacheOrGet(fqnToKey, userDataKeyRef, Key.create(userDataKeyRef));
            return getCachedValue(targetClass, classMetadataKey, () -> {
                log.debug("Creating metadata instance for " + userDataKeyRef);
                Set<PsiClass> dependencies = PsiCustomUtil.computeDependencies(module, type);
                if (dependencies != null) {
                    return Result.create(ClassSuggestionNodeFactory.newClassMetadata(type), dependencies);
                }
                return null;
            });
        }
        return null;
    }


    protected interface TargetInvokerWithReturnValue<T> {

        T invoke(ClassMetadata classMetadata);
    }

}

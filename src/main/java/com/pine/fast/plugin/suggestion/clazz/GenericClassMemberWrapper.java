package com.pine.fast.plugin.suggestion.clazz;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.pine.fast.plugin.misc.PsiCustomUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.completion.FileType;
import com.pine.fast.plugin.suggestion.completion.SuggestionDocumentationHelper;
import com.pine.fast.plugin.suggestion.handler.YamlValueInsertHandler;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * Documentation is never requested for a class, instead its requested for properties within the class. This acts as a
 * proxy for documentation & basic details about the property/getter method that contains actual documentation
 */
public class GenericClassMemberWrapper implements SuggestionNode, SuggestionDocumentationHelper {

    private final PsiMember member;

    @NotNull
    private final String originalName;
    @Nullable
    private final String documentation;
    @Nullable
    private final String shortType;
    private final boolean deprecated;

    private MetadataProxy proxy;

    public GenericClassMemberWrapper(@NotNull PsiMember member) {
        this.member = member;
        this.originalName = requireNonNull(member.getName());
        this.documentation = PsiCustomUtil.computeDocumentation(member);
        this.shortType = PsiCustomUtil.toClassNonQualifiedName(PsiCustomUtil.getReferredPsiType(member));
        this.deprecated = computeDeprecationStatus();
    }

    public MetadataProxy getMemberReferredClassMetadataProxy(Module module) {
        if (proxy == null) {
            proxy = ClassSuggestionNodeFactory.newMetadataProxy(module, PsiCustomUtil.getReferredPsiType(member));
        }
        return proxy;
    }

    private <T> T doWithMemberReferredClassProxy(Module module, ProxyInvoker<T> invoker,
                                                 T defaultValue) {
        MetadataProxy delegate = getMemberReferredClassMetadataProxy(module);
        if (delegate != null) {
            return invoker.invoke(delegate);
        }
        return defaultValue;
    }

    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
                                                          List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
                                                          int pathSegmentStartIndex) {
        return doWithMemberReferredClassProxy(module, proxy -> proxy
                .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                        pathSegmentStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex) {
        return doWithMemberReferredClassProxy(module, proxy -> proxy
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex), null);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
                                                                  List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
                                                                  int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
        return doWithMemberReferredClassProxy(module, proxy -> proxy
                .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
                        querySegmentPrefixes, querySegmentPrefixStartIndex, siblingsToExclude), null);
    }

    @Override
    public  SortedSet<Suggestion> findKeySuggestionsForContains(Module module, FileType fileType,
                                                                List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String querySegmentPrefixes) {

        return null;
    }

    @NotNull
    @Override
    public String getOriginalName() {
        return originalName;
    }

    @Nullable
    @Override
    public String getNameForDocumentation(Module module) {
        return doWithMemberReferredClassProxy(module, proxy -> {
            if (proxy.targetRepresentsArray() || proxy.targetClassRepresentsIterable(module)) {
                return originalName + "[]";
            } else {
                return originalName;
            }
        }, originalName);
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
        return doWithMemberReferredClassProxy(module, proxy -> proxy
                .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix,
                        siblingsToExclude), null);
    }

    @Override
    public boolean isLeaf(Module module) {
        return doWithMemberReferredClassProxy(module, proxy -> proxy.isLeaf(module), true);
    }

    @Override
    public boolean isMetadataNonProperty() {
        return false;
    }

    private boolean computeDeprecationStatus() {
        if (member instanceof PsiField) {
            return stream(PsiField.class.cast(member).getType().getAnnotations()).anyMatch(annotation -> {
                String fqn = annotation.getQualifiedName();
                return fqn != null && fqn.equals("java.lang.Deprecated");
            });
        } else if (member instanceof PsiMethod) {
            return stream(requireNonNull(PsiMethod.class.cast(member).getReturnType()).getAnnotations())
                    .anyMatch(annotation -> {
                        String fqn = annotation.getQualifiedName();
                        return fqn != null && fqn.equals("java.lang.Deprecated");
                    });
        }
        throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
    }

    @NotNull
    @Override
    public Suggestion buildSuggestionForKey(Module module, FileType fileType,
                                            List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
        Icon icon = doWithMemberReferredClassProxy(module, proxy -> proxy.getSuggestionNodeType(module),
                SuggestionNodeType.UNKNOWN_CLASS).getIcon();
        Suggestion.SuggestionBuilder builder =
                Suggestion.builder().suggestionToDisplay(originalName).description(documentation)
                        .shortType(shortType).numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
                        .icon(icon);
        if (deprecated) {
            builder.deprecationLevel(SpringConfigurationMetadataDeprecationLevel.warning);
        }
        return builder.fileType(fileType).build();
    }

    @Override
    public boolean supportsDocumentation() {
        return true;
    }

    @NotNull
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
        return "<b>" + nodeNavigationPathDotDelimited + "</b>" + new JavaDocumentationProvider()
                .generateDoc(member, member);
    }

    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
                                           String originalValue) {
        return "<b>" + nodeNavigationPathDotDelimited + "</b> =  <b>" + YamlValueInsertHandler.unescapeValue(originalValue) + "</b>"
                + new JavaDocumentationProvider().generateDoc(member, member);
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
        return doWithMemberReferredClassProxy(module, proxy -> proxy.getSuggestionNodeType(module),
                SuggestionNodeType.UNKNOWN_CLASS);
    }

    private interface ProxyInvoker<T> {

        T invoke(MetadataProxy proxy);
    }

}

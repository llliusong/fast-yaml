package com.pine.fast.plugin.suggestion.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.completion.FileType;
import com.pine.fast.plugin.suggestion.metadata.MetadataNonPropertySuggestionNode;
import com.pine.fast.plugin.suggestion.metadata.MetadataPropertySuggestionNode;
import com.pine.fast.plugin.suggestion.metadata.MetadataSuggestionNode;
import com.pine.fast.plugin.suggestion.metadata.json.GsonPostProcessEnablingTypeFactory;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadata;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataHint;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataProperty;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderTypeDeserializer;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

public class SuggestionServiceImpl implements SuggestionService {

    private final static String MODULE_NAME = "yaml";
    private final static String SIMPLE_NAME = "simple";

    private static final Logger log = Logger.getInstance(SuggestionServiceImpl.class);

    /**
     * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hiphens,
     * underscores, e.t.c in the keys themselves
     */
    private final Map<String, Trie<String, MetadataSuggestionNode>> moduleNameToRootSearchIndex;

    SuggestionServiceImpl() {
        moduleNameToRootSearchIndex = new THashMap<>();
    }

    /**
     * 根据 逗号  分隔为数组
     *
     * @param element
     * @return
     */
    private static String[] toSanitizedPathSegments(String element) {
        String[] splits = element.trim().split(Suggestion.PERIOD_DELIMITER, -1);
        for (int i = 0; i < splits.length; i++) {
            splits[i] = SuggestionNode.sanitise(splits[i]);
        }
        return splits;
    }

    private static String[] toRawPathSegments(String element) {
        String[] splits = element.trim().split(Suggestion.PERIOD_DELIMITER, -1);
        for (int i = 0; i < splits.length; i++) {
            splits[i] = splits[i].trim();
        }
        return splits;
    }

    private void initSearchIndex(Module module) {
        Trie<String, MetadataSuggestionNode> rootSearchIndex = moduleNameToRootSearchIndex.get(MODULE_NAME);
        Trie<String, MetadataSuggestionNode> simpleSearchIndex = moduleNameToRootSearchIndex.get(SIMPLE_NAME);

        if (rootSearchIndex == null) {
            rootSearchIndex = new PatriciaTrie<>();
            simpleSearchIndex = new PatriciaTrie<>();
            moduleNameToRootSearchIndex.put(MODULE_NAME, rootSearchIndex);
            moduleNameToRootSearchIndex.put(SIMPLE_NAME, simpleSearchIndex);

            try {
                // TODO: pine 2021/3/31 通过本地配置 + 外部配置实现
                InputStream inputStream = getClass().getResourceAsStream("/suggestion.json");
                GsonBuilder gsonBuilder = new GsonBuilder();
                // register custom mapper adapters
                gsonBuilder.registerTypeAdapter(SpringConfigurationMetadataValueProviderType.class,
                        new SpringConfigurationMetadataValueProviderTypeDeserializer());
                gsonBuilder.registerTypeAdapterFactory(new GsonPostProcessEnablingTypeFactory());
                SpringConfigurationMetadata springConfigurationMetadata = gsonBuilder.create()
                        .fromJson(new BufferedReader(new InputStreamReader(inputStream, UTF_8)),
                                SpringConfigurationMetadata.class);

                addPropertiesToIndex(module, rootSearchIndex, springConfigurationMetadata, "test");
                addHintsToIndex(module, rootSearchIndex, springConfigurationMetadata, "hintTest");
                addSimplesToIndex(simpleSearchIndex, springConfigurationMetadata, "simpleTest");
                System.out.println(rootSearchIndex);
            } catch (Exception e) {
                log.error("初始化搜索索引失败");
            }

        }
    }

    @Override
    public boolean canProvideSuggestions(Project project, Module module) {
        // TODO: pine 2021/4/7  持久化数据待完善
//        ServerPersistent serverPersistent = ServerPersistent.getInstance();
//        ServiceConfig state = serverPersistent.getState();
//        return state == null || state.getHint() == null || state.getHint();
        return true;
    }

    @Override
    public List<LookupElementBuilder> findSuggestionsForQueryPrefix(Project project, Module module,
                                                                    FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys,
                                                                    String queryWithDotDelimitedPrefixes, String pre, @Nullable Set<String> siblingsToExclude) {
        initSearchIndex(module);

        List<LookupElementBuilder> lookupElementBuilders = doFindSuggestions(module,
                moduleNameToRootSearchIndex.get(SIMPLE_NAME), fileType, pre);

        List<LookupElementBuilder> lookupElementBuilder = doFindSuggestionsForQueryPrefix(module,
                moduleNameToRootSearchIndex.get(MODULE_NAME), fileType, element, ancestralKeys,
                queryWithDotDelimitedPrefixes, siblingsToExclude);

        if (CollectionUtils.isEmpty(lookupElementBuilders)) {
            return lookupElementBuilder;
        }
        if (CollectionUtils.isEmpty(lookupElementBuilder)) {
            return lookupElementBuilders;
        }
        lookupElementBuilders.addAll(lookupElementBuilder);
        return lookupElementBuilders;

    }

    private List<LookupElementBuilder> doFindSuggestions(Module module,
                                                         Trie<String, MetadataSuggestionNode> rootSearchIndex, FileType fileType, String queryWithDotDelimitedPrefixes) {
        debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
        StopWatch timer = new StopWatch();
        timer.start();
        try {
            // 简单匹配只需要对顶层进行查询
            Set<Suggestion> suggestions = doFindSuggestionsForQueryPrefix2(module, fileType, rootSearchIndex.values(), queryWithDotDelimitedPrefixes);
            if (suggestions != null) {
                return toLookupElementBuilders(suggestions);
            }
            return null;
        } finally {
            timer.stop();
            debug(() -> log.debug("Search took " + timer.toString()));
        }


    }

    private List<LookupElementBuilder> doFindSuggestionsForQueryPrefix(Module module,
                                                                       Trie<String, MetadataSuggestionNode> rootSearchIndex, FileType fileType, PsiElement element,
                                                                       @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes,
                                                                       @Nullable Set<String> siblingsToExclude) {
        debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
        StopWatch timer = new StopWatch();
        timer.start();
        try {
            String[] querySegmentPrefixes = toSanitizedPathSegments(queryWithDotDelimitedPrefixes);
            Set<Suggestion> suggestions = null;
            if (ancestralKeys != null) {
                String[] ancestralKeySegments =
                        ancestralKeys.stream().flatMap(key -> stream(toRawPathSegments(key)))
                                .toArray(String[]::new);
                MetadataSuggestionNode rootNode = rootSearchIndex.get(SuggestionNode.sanitise(ancestralKeySegments[0]));
                if (rootNode != null) {
                    List<SuggestionNode> matchesRootToDeepest;
                    SuggestionNode startSearchFrom = null;
                    if (ancestralKeySegments.length > 1) {
                        String[] sanitisedAncestralPathSegments =
                                stream(ancestralKeySegments).map(SuggestionNode::sanitise).toArray(String[]::new);
                        matchesRootToDeepest = rootNode
                                .findDeepestSuggestionNode(module, GenericUtil.modifiableList(rootNode),
                                        sanitisedAncestralPathSegments, 1);
                        if (matchesRootToDeepest != null && matchesRootToDeepest.size() != 0) {
                            startSearchFrom = matchesRootToDeepest.get(matchesRootToDeepest.size() - 1);
                        }
                    } else {
                        startSearchFrom = rootNode;
                        matchesRootToDeepest = singletonList(rootNode);
                    }

                    if (startSearchFrom != null) {
                        // if search start node is a leaf, this means, the user is looking for values for the given key, lets find the suggestions for values
                        if (startSearchFrom.isLeaf(module)) {
                            suggestions = startSearchFrom.findValueSuggestionsForPrefix(module, fileType,
                                    unmodifiableList(matchesRootToDeepest),
                                    SuggestionNode.sanitise(GenericUtil.truncateIdeaDummyIdentifier(element.getText())), siblingsToExclude);
                        } else {
                            suggestions = startSearchFrom.findKeySuggestionsForQueryPrefix(module, fileType,
                                    unmodifiableList(matchesRootToDeepest), matchesRootToDeepest.size(),
                                    querySegmentPrefixes, 0, siblingsToExclude);
                        }
                    }
                }
            } else {
                String rootQuerySegmentPrefix = querySegmentPrefixes[0];
                SortedMap<String, MetadataSuggestionNode> topLevelQueryResults =
                        rootSearchIndex.prefixMap(rootQuerySegmentPrefix);

                Collection<MetadataSuggestionNode> childNodes;
                int querySegmentPrefixStartIndex;

                // 如果在顶层没有找到匹配的key，再对儿子级进行匹配查询
                if (topLevelQueryResults == null || topLevelQueryResults.size() == 0) {
                    childNodes = rootSearchIndex.values();
                    querySegmentPrefixStartIndex = 0;
                } else {
                    childNodes = topLevelQueryResults.values();
                    querySegmentPrefixStartIndex = 1;
                }

                Collection<MetadataSuggestionNode> nodesToSearchAgainst;
                if (siblingsToExclude != null) {
                    Set<MetadataSuggestionNode> nodesToExclude = siblingsToExclude.stream()
                            .flatMap(exclude -> rootSearchIndex.prefixMap(exclude).values().stream())
                            .collect(toSet());
                    nodesToSearchAgainst =
                            childNodes.stream().filter(node -> !nodesToExclude.contains(node)).collect(toList());
                } else {
                    nodesToSearchAgainst = childNodes;
                }

                suggestions = doFindSuggestionsForQueryPrefix(module, fileType, nodesToSearchAgainst,
                        querySegmentPrefixes, querySegmentPrefixStartIndex);
            }

            if (suggestions != null) {
                return toLookupElementBuilders(suggestions);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            timer.stop();
            debug(() -> log.debug("Search took " + timer.toString()));
        }
    }

    private Set<Suggestion> doFindSuggestionsForQueryPrefix2(Module module, FileType fileType,
                                                             Collection<MetadataSuggestionNode> nodesToSearchWithin, String queryWithDotDelimitedPrefixes) {
        Set<Suggestion> suggestions = null;
        for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
            Set<Suggestion> matchedSuggestions = suggestionNode
                    .findKeySuggestionsForQueryPrefix(module, fileType, GenericUtil.modifiableList(suggestionNode), 0,
                            queryWithDotDelimitedPrefixes);

            if (matchedSuggestions != null) {
                if (suggestions == null) {
                    suggestions = new THashSet<>();
                }
                suggestions.addAll(matchedSuggestions);
            }
        }
        return suggestions;
    }

    @Nullable
    private Set<Suggestion> doFindSuggestionsForQueryPrefix(Module module, FileType fileType,
                                                            Collection<MetadataSuggestionNode> nodesToSearchWithin, String[] querySegmentPrefixes,
                                                            int querySegmentPrefixStartIndex) {
        Set<Suggestion> suggestions = null;
        for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
            Set<Suggestion> matchedSuggestions = suggestionNode
                    .findKeySuggestionsForQueryPrefix(module, fileType, GenericUtil.modifiableList(suggestionNode), 0,
                            querySegmentPrefixes, querySegmentPrefixStartIndex);
            if (matchedSuggestions != null) {
                if (suggestions == null) {
                    suggestions = new THashSet<>();
                }
                suggestions.addAll(matchedSuggestions);
            }
        }
        return suggestions;
    }

    @Nullable
    private List<LookupElementBuilder> toLookupElementBuilders(
            @Nullable Set<Suggestion> suggestions) {
        if (suggestions != null) {
            return suggestions.stream().map(Suggestion::newLookupElement).collect(toList());
        }
        return null;
    }

    private void addHintsToIndex(Module module, Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                 SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
        List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
        if (hints != null) {
            hints.sort(comparing(SpringConfigurationMetadataHint::getName));
            for (SpringConfigurationMetadataHint hint : hints) {
                String[] pathSegments = toSanitizedPathSegments(hint.getExpectedPropertyName());
                MetadataSuggestionNode closestMetadata =
                        findDeepestMetadataMatch(rootSearchIndex, pathSegments, true);
                if (closestMetadata != null) {
                    if (!closestMetadata.isProperty()) {
                        log.warn(
                                "Unexpected hint " + hint.getName() + " is assigned to  group " + closestMetadata
                                        .getPathFromRoot(module)
                                        + " found. Hints can be only assigned to property. Ignoring the hint completely.Existing group belongs to ("
                                        + closestMetadata.getBelongsTo().stream().collect(joining(","))
                                        + "), New hint belongs " + containerPath);
                    } else {
                        MetadataPropertySuggestionNode propertySuggestionNode =
                                MetadataPropertySuggestionNode.class.cast(closestMetadata);
                        if (hint.representsValueOfMap()) {
                            propertySuggestionNode.getProperty().setValueHint(hint);
                        } else {
                            propertySuggestionNode.getProperty().setGenericOrKeyHint(hint);
                        }
                    }
                }
            }
        }
    }

    private void addPropertiesToIndex(Module module,
                                      Trie<String, MetadataSuggestionNode> rootSearchIndex,
                                      SpringConfigurationMetadata springConfigurationMetadata, String containerArchiveOrFileRef) {
        List<SpringConfigurationMetadataProperty> properties =
                springConfigurationMetadata.getProperties();
        properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
        for (SpringConfigurationMetadataProperty property : properties) {
            String[] pathSegments = toSanitizedPathSegments(property.getName());
            String[] rawPathSegments = toRawPathSegments(property.getName());
            MetadataSuggestionNode closestMetadata =
                    findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

            int startIndex;
            if (closestMetadata == null) {
                // 是否没有子节点，只有根节点存在
                boolean onlyRootSegmentExists = pathSegments.length == 1;
                if (onlyRootSegmentExists) {
                    closestMetadata = MetadataPropertySuggestionNode
                            .newInstance(rawPathSegments[0], property, null, containerArchiveOrFileRef);
                } else {
                    closestMetadata = MetadataNonPropertySuggestionNode
                            .newInstance(rawPathSegments[0], null, containerArchiveOrFileRef);
                }
                rootSearchIndex.put(pathSegments[0], closestMetadata);

                // 因为我们已经处理了根级项目，所以让addChildren从pathSegments的索引1开始
                startIndex = 1;
            } else {
                startIndex = closestMetadata.numOfHopesToRoot() + 1;
            }

            boolean haveMoreSegmentsLeft = startIndex < rawPathSegments.length;

            if (haveMoreSegmentsLeft) {
                if (!closestMetadata.isProperty()) {
                    MetadataNonPropertySuggestionNode.class.cast(closestMetadata)
                            .addChildren(property, rawPathSegments, startIndex, containerArchiveOrFileRef);
                } else {
                    log.warn("Detected conflict between a new group & existing property for suggestion path "
                            + closestMetadata.getPathFromRoot(module)
                            + ". Ignoring property. Existing non property node belongs to (" + closestMetadata
                            .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to "
                            + containerArchiveOrFileRef);
                }
            } else {
                if (!closestMetadata.isProperty()) {
                    log.warn(
                            "Detected conflict between a new metadata property & existing non property node for suggestion path "
                                    + closestMetadata.getPathFromRoot(module)
                                    + ". Ignoring property. Existing non property node belongs to (" + closestMetadata
                                    .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to "
                                    + containerArchiveOrFileRef);
                } else {
                    closestMetadata.addRefCascadeTillRoot(containerArchiveOrFileRef);
                    log.debug("Detected a duplicate metadata property for suggestion path " + closestMetadata
                            .getPathFromRoot(module) + ". Ignoring property. Existing property belongs to ("
                            + closestMetadata.getBelongsTo().stream().collect(joining(","))
                            + "), New property belongs to " + containerArchiveOrFileRef);
                }
            }
        }
    }


    private void addSimplesToIndex(
            Trie<String, MetadataSuggestionNode> rootSearchIndex,
            SpringConfigurationMetadata springConfigurationMetadata, String containerArchiveOrFileRef) {
        List<SpringConfigurationMetadataProperty> simples =
                springConfigurationMetadata.getSimples();
        simples.sort(comparing(SpringConfigurationMetadataProperty::getName));
        for (SpringConfigurationMetadataProperty simple : simples) {
            String originalName = StringUtils.isEmpty(simple.getOriginalName()) ? simple.getName() : simple.getOriginalName();
            MetadataSuggestionNode closestMetadata = MetadataPropertySuggestionNode
                    .newInstance(simple.getName(), originalName, simple, null, containerArchiveOrFileRef);
            rootSearchIndex.put(simple.getName(), closestMetadata);
        }
    }

    private MetadataSuggestionNode findDeepestMetadataMatch(Map<String, MetadataSuggestionNode> roots,
                                                            String[] pathSegments, boolean matchAllSegments) {
        MetadataSuggestionNode closestMatchedRoot = roots.get(pathSegments[0]);
        if (closestMatchedRoot != null) {
            closestMatchedRoot =
                    closestMatchedRoot.findDeepestMetadataNode(pathSegments, 1, matchAllSegments);
        }
        return closestMatchedRoot;
    }


    @SuppressWarnings("unused")
    private String toTree() {
        StringBuilder builder = new StringBuilder();
        moduleNameToRootSearchIndex.forEach((k, v) -> {
            builder.append("Module: ").append(k).append("\n");
            v.values().forEach(root -> builder
                    .append(root.toTree().trim().replaceAll("^", "  ").replaceAll("\n", "\n  "))
                    .append("\n"));
        });
        return builder.toString();
    }

    /**
     * Debug logging can be enabled by adding fully classified class name/package name with # prefix For eg., to enable
     * debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
     *
     * @param doWhenDebug code to execute when debug is enabled
     */
    private void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }
}

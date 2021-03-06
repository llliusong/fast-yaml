package com.pine.fast.plugin.suggestion;

import static com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis;
import static com.intellij.ui.JBColor.RED;
import static com.intellij.ui.JBColor.YELLOW;
import static com.pine.fast.plugin.misc.GenericUtil.getFirstSentenceWithoutDot;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.yaml.YAMLHighlighter.SCALAR_TEXT;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.suggestion.completion.FileType;
import com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.Icon;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

@Getter
@EqualsAndHashCode(of = "suggestionToDisplay")
@ToString
public class Suggestion implements Comparable<Suggestion> {

    public static final String PERIOD_DELIMITER = "\\.";

    /**
     * 根据建议节点拼接代码提示
     */
    private static final LookupElementRenderer<LookupElement> CUSTOM_SUGGESTION_RENDERER =
            new LookupElementRenderer<LookupElement>() {
                @Override
                public void renderElement(LookupElement element, LookupElementPresentation presentation) {
                    Suggestion suggestion = (Suggestion) element.getObject();
                    if (suggestion.icon != null) {
                        presentation.setIcon(suggestion.icon);
                    }

                    presentation.setStrikeout(suggestion.deprecationLevel != null);
                    if (suggestion.deprecationLevel != null) {
                        if (suggestion.deprecationLevel == SpringConfigurationMetadataDeprecationLevel.error) {
                            presentation.setItemTextForeground(RED);
                        } else {
                            presentation.setItemTextForeground(YELLOW);
                        }
                    }

                    String lookupString = element.getLookupString();
                    presentation.setItemText(lookupString);
                    if (!lookupString.equals(suggestion.suggestionToDisplay)) {
                        presentation.setItemTextBold(true);
                    }

                    String shortDescription;
                    if (StringUtils.isNotEmpty(suggestion.defaultValue)) {
                        shortDescription = shortenTextWithEllipsis(suggestion.defaultValue, 60, 0, true);
                        TextAttributes attrs =
                                EditorColorsManager.getInstance().getGlobalScheme().getAttributes(SCALAR_TEXT);
//            presentation.setTailText("=" + shortDescription, attrs.getForegroundColor());
                        presentation.setTailText(" (" + shortDescription + ")", attrs.getForegroundColor());
                    }

                    if (StringUtils.isNotEmpty(suggestion.description)) {
                        presentation
                                .appendTailText(" " + getFirstSentenceWithoutDot(suggestion.description),
                                        true);
                    }

                    if (suggestion.shortType != null) {
                        presentation.setTypeText(suggestion.shortType);
                    }
                }
            };

    @NotNull
    private String suggestionToDisplay;
    @Nullable
    private String description;
    @Nullable
    private String shortType;
    @Nullable
    private String defaultValue;
    @Nullable
    private SpringConfigurationMetadataDeprecationLevel deprecationLevel;
    /**
     * There are two approaches to storing a reference to suggestion
     * <ol>
     * <li>Storing the whole value (support dynamic nodes aswell, as a single PsiClass as leaf might be referred via multiple paths)</li>
     * <li>Storing reference to leaf & navigate up till the root (efficient)</li>
     * </ol>
     * The second solution does not address suggestions that are derived from {@link ClassMetadata} as these nodes are not tied to a single branch of the suggestion tree
     */
    @NotNull
    private List<? extends SuggestionNode> matchesTopFirst;
    /**
     * Defines the number of ancestors from root, below which the current suggestion should be shown
     */
    private int numOfAncestors;
    /**
     * Whether or not the suggestion corresponds to value within key -> value pair
     */
    private boolean forValue;
    /**
     * Whether the current value represents the default value
     */
    @Setter
    private boolean representingDefaultValue;
    /**
     * Type of file that requested this suggestion
     */
    @NotNull
    private FileType fileType;
    @Nullable
    private Icon icon;

    private String pathDotDelimitedRootToLeaf;

    /**
     * 是否追加冒号
     */
    @Getter
    @Setter
    private Boolean isAppendColon;

    @Builder
    public Suggestion(@NotNull String suggestionToDisplay, @Nullable String description,
                      @Nullable String shortType, @Nullable String defaultValue,
                      @Nullable SpringConfigurationMetadataDeprecationLevel deprecationLevel,
                      @NotNull List<? extends SuggestionNode> matchesTopFirst, int numOfAncestors, boolean forValue,
                      boolean representingDefaultValue, @NotNull FileType fileType, @Nullable Icon icon, Boolean isAppendColon) {
        this.suggestionToDisplay = suggestionToDisplay;
        this.description = description;
        this.shortType = shortType;
        this.defaultValue = defaultValue;
        this.deprecationLevel = deprecationLevel;
        this.matchesTopFirst = matchesTopFirst;
        this.numOfAncestors = numOfAncestors;
        this.forValue = forValue;
        this.representingDefaultValue = representingDefaultValue;
        this.fileType = fileType;
        this.icon = icon;
        this.pathDotDelimitedRootToLeaf =
                matchesTopFirst.stream().map(SuggestionNode::getOriginalName).collect(joining("."));
        this.isAppendColon = isAppendColon == null || isAppendColon;
    }

    public LookupElementBuilder newLookupElement() {
        LookupElementBuilder builder = LookupElementBuilder.create(this, suggestionToDisplay).withCaseSensitivity(false);
        if (forValue) {
            if (description != null) {
                builder = builder.withTypeText(description, true);
            }
            if (representingDefaultValue) {
                builder = builder.bold();
            }
            builder = builder.withInsertHandler(fileType.newValueInsertHandler());
        } else {
            builder = builder.withRenderer(CUSTOM_SUGGESTION_RENDERER)
                    .withInsertHandler(fileType.newKeyInsertHandler());
        }
        return builder;
    }

    public String getFullPath() {
        return GenericUtil.dotDelimitedOriginalNames(matchesTopFirst);
    }

    public SuggestionNode getLastSuggestionNode() {
        return matchesTopFirst.get(matchesTopFirst.size() - 1);
    }

    @Override
    public int compareTo(@NotNull Suggestion other) {
        int pathRootToLeafComparisonValue =
                pathDotDelimitedRootToLeaf.compareTo(other.pathDotDelimitedRootToLeaf);
        if (pathRootToLeafComparisonValue == 0) {
            return suggestionToDisplay.compareTo(other.suggestionToDisplay);
        }
        return pathRootToLeafComparisonValue;
    }

    @NotNull
    public List<? extends OriginalNameProvider> getMatchesForReplacement() {
        if (matchesTopFirst.size() > numOfAncestors) {
            return matchesTopFirst.stream().skip(numOfAncestors).collect(toList());
        } else { // can happen when user is trying to select as a child of array, in this case, the suggestion itself becomes the original name
            return singletonList(() -> suggestionToDisplay);
        }
    }

}

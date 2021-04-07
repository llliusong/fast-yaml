package com.pine.fast.plugin.suggestion.metadata.json;

import com.google.gson.annotations.SerializedName;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;
import com.pine.fast.plugin.suggestion.completion.FileType;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0/reference/htmlsingle/#configuration-metadata-group-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataGroup {

    private String name;
    @Nullable
    @SerializedName("type")
    private String className;
    @Nullable
    private String description;
    @Nullable
    private String sourceType;
    @Nullable
    private String sourceMethod;
    @NotNull
    private SuggestionNodeType nodeType = SuggestionNodeType.UNDEFINED;

    /**
     * 是否追加冒号
     */
    private Boolean isAppendColon;

    public Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
                                    int numOfAncestors) {
        return Suggestion.builder().suggestionToDisplay(
                GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                .description(description).shortType(GenericUtil.shortenedType(className)).numOfAncestors(numOfAncestors)
                .matchesTopFirst(matchesRootTillMe).icon(nodeType.getIcon()).fileType(fileType).build();
    }

}

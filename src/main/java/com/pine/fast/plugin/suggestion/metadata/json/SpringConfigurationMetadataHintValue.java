package com.pine.fast.plugin.suggestion.metadata.json;

import com.google.gson.annotations.SerializedName;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.completion.FileType;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpringConfigurationMetadataHintValue {

    /**
     * A valid value for the element to which the hint refers. If the type of the associated PROPERTY is an ARRAY, it
     * can also be an ARRAY of value(s). This attribute is mandatory.
     */
    @SerializedName("value")
    private Object nameAsObjOrArray;
    @Nullable
    private String description;

    @Override
    public String toString() {
        if (nameAsObjOrArray instanceof Array) {
            StringBuilder builder = new StringBuilder("[");
            int length = Array.getLength(nameAsObjOrArray);
            for (int i = 0; i < length; i++) {
                Object arrayElement = Array.get(nameAsObjOrArray, i);
                builder.append(" ").append(arrayElement.toString());
                if (i == length - 1) {
                    builder.append(" ");
                } else {
                    builder.append(",");
                }
            }
            return builder.append("]").toString();
        } else if (nameAsObjOrArray instanceof Collection) {
            Collection nameAsCollection = Collection.class.cast(nameAsObjOrArray);
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < nameAsCollection.size(); i++) {
                Object arrayElement = Array.get(nameAsObjOrArray, i);
                builder.append(" ").append(arrayElement.toString());
                if (i == nameAsCollection.size() - 1) {
                    builder.append(" ");
                } else {
                    builder.append(",");
                }
            }
            return builder.append("]").toString();
        } else {
            return nameAsObjOrArray.toString();
        }
    }

    public boolean representsSingleValue() {
        return !nameAsObjOrArray.getClass().isArray() && !Collection.class.isInstance(nameAsObjOrArray);
    }

    @NotNull
    public Suggestion buildSuggestionForKey(FileType fileType,
                                            List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors, SuggestionNode match) {
        List<SuggestionNode> matchesRootTillMe = GenericUtil.newListWithMembers(matchesRootTillParentNode, match);
        Suggestion.SuggestionBuilder builder = Suggestion.builder().suggestionToDisplay(
                GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                .description(description).numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe);
        return builder.fileType(fileType).build();
    }


    @NotNull
    public Suggestion buildSuggestionForValue(FileType fileType,
                                              List<? extends SuggestionNode> matchesRootTillLeaf, @Nullable String defaultValue) {
        Suggestion.SuggestionBuilder builder =
                Suggestion.builder().suggestionToDisplay(toString()).description(description).forValue(true)
                        .matchesTopFirst(matchesRootTillLeaf).numOfAncestors(matchesRootTillLeaf.size());

        builder.representingDefaultValue(toString().equals(defaultValue));
        return builder.fileType(fileType).build();
    }

}

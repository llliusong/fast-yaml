package com.pine.fast.plugin.suggestion.metadata.json;

import static com.pine.fast.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType.any;
import static java.util.Objects.requireNonNull;

import com.pine.fast.plugin.suggestion.SuggestionNode;
import gnu.trove.THashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataHint implements com.pine.fast.plugin.suggestion.metadata.json.GsonPostProcessable {

    private static final Pattern KEY_REGEX_PATTERN_FOR_MAP = Pattern.compile("\\.keys$");
    private static final Pattern VALUE_REGEX_PATTERN_FOR_MAP = Pattern.compile("\\.values$");

    @Setter
    @Getter
    private String name;
    @Setter
    @Nullable
    private SpringConfigurationMetadataHintValue[] values;
    @Setter
    @Nullable
    private SpringConfigurationMetadataValueProvider[] providers;

    @Nullable
    private Map<String, SpringConfigurationMetadataHintValue> valueLookup;
    @Nullable
    private Trie<String, SpringConfigurationMetadataHintValue> valueTrie;

    /**
     * 是否追加冒号
     */
    private Boolean isAppendColon;

    /**
     * If the property that corresponds with this hint represents a map, Hint's key would be end with `.keys`/`.values`
     *
     * @return property name that corresponds to this hint
     */
    public String getExpectedPropertyName() {
        return VALUE_REGEX_PATTERN_FOR_MAP
                .matcher(KEY_REGEX_PATTERN_FOR_MAP.matcher(name).replaceAll("")).replaceAll("");
    }

    public boolean representsKeyOfMap() {
        return KEY_REGEX_PATTERN_FOR_MAP.matcher(name).find();
    }

    public boolean representsValueOfMap() {
        return VALUE_REGEX_PATTERN_FOR_MAP.matcher(name).find();
    }

    @Override
    public void doOnGsonDeserialization() {
        if (hasPredefinedValues()) {
            valueLookup = new THashMap<>();
            valueTrie = new PatriciaTrie<>();
            for (SpringConfigurationMetadataHintValue value : requireNonNull(values)) {
                // The default value can be array (if property is of type array) as per documentation, we dont support those usecases as of now
                if (value.representsSingleValue()) {
                    String suggestion = value.toString();
                    valueLookup.put(SuggestionNode.sanitise(suggestion), value);
                    valueTrie.put(SuggestionNode.sanitise(suggestion), value);
                }
            }
        }
    }

    public boolean hasPredefinedValues() {
        return values != null && values.length != 0;
    }

    @Nullable
    public SpringConfigurationMetadataHintValue findHintValueWithName(String pathSegment) {
        SpringConfigurationMetadataHintValue value = null;
        if (valueLookup != null) {
            value = valueLookup.get(SuggestionNode.sanitise(pathSegment));
        }

        if (value == null) {
            if (providers != null && Arrays.stream(providers).anyMatch(provider -> provider.getType() == any)) {
                value =
                        SpringConfigurationMetadataHintValue.builder().nameAsObjOrArray(pathSegment).build();
            }
        }
        return value;
    }

    public Collection<SpringConfigurationMetadataHintValue> findHintValuesWithPrefix(
            String querySegmentPrefix) {
        if (valueTrie != null) {
            return valueTrie.prefixMap(SuggestionNode.sanitise(querySegmentPrefix)).values();
        }
        return null;
    }

}

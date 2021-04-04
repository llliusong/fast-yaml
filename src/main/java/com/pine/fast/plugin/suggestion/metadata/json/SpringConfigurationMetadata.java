package com.pine.fast.plugin.suggestion.metadata.json;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;

/**
 * Represents all entries present in `/META-INF/spring-configuration-metadata.json` For more info
 * https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-property-attributes
 */
@Data
public class SpringConfigurationMetadata {

    @Nullable
    private List<SpringConfigurationMetadataGroup> groups;
    private List<SpringConfigurationMetadataProperty> properties;
    @Nullable
    private List<SpringConfigurationMetadataHint> hints;

    /**
     * ¼òµ¥key:value, È«¾ÖËÑË÷
     */
    private List<SpringConfigurationMetadataProperty> simples;

}

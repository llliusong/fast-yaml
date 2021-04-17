package com.pine.fast.plugin.persistent;

import com.intellij.ide.util.PropertiesComponent;
import org.apache.commons.lang3.BooleanUtils;

/**
 * 系统配置
 *
 * @author pine
 * @date 2021/4/17 1:18 下午.
 */
public class SystemConfig {

    private final static String PROJECT_NAME = "com.pine.fast.plugin";

    private static final PropertiesComponent PROPERTIES_COMPONENT = PropertiesComponent.getInstance();

    private final static String IS_HINT_KEY = PROJECT_NAME + "isHint";

    public static Boolean getHint() {
        // 直接存储 boolean 类型会有默认值，默认值一样时不会设置
        String value = PROPERTIES_COMPONENT.getValue(IS_HINT_KEY, "True");
        return BooleanUtils.toBoolean(value);
    }

    public static void setHint() {
        Boolean hint = getHint();
        PROPERTIES_COMPONENT.setValue(IS_HINT_KEY, BooleanUtils.toStringTrueFalse(!hint));
    }
}
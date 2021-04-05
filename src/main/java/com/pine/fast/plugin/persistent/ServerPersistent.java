package com.pine.fast.plugin.persistent;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.generate.element.Element;

/**
 * name：持久化文件组件名称可随意指定，通常用插件名称即可。
 * storages ：定义配置参数的持久化位置。其中$APP_CONFIG$变量为Idea安装后默认的用户路径，例如：C:\Documents and
 * Settings\10139682\.IdeaIC2017.3\config\options\searchJarPath.xml
 *
 * 开发插件配置面板（SearchableConfigurable）
 */
@State(name = "config", storages = {@Storage(value ="$APP_CONFIG$/config.xml")})
public class ServerPersistent implements PersistentStateComponent<ServiceConfig> {
    private ServiceConfig serviceConfig = new ServiceConfig();

    public static ServerPersistent getInstance() {
        return ServiceManager.getService(ServerPersistent.class);
    }


    @Nullable
    @Override
    public ServiceConfig getState() {
        return serviceConfig;
    }

    @Override
    public void loadState(ServiceConfig state) {
        XmlSerializerUtil.copyBean(state, serviceConfig);
    }

    @Override
    public void noStateLoaded() {

    }
}

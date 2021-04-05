package com.pine.fast.plugin.persistent;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * name （必须） — 指定状态的名称（XML中的根标签名）
 * storages 一个或多个@com.intellij.openapi.components.Storage注解指定存储位置。对于项目级的值是可选的 — 这种情况状态会保存在标准项目文件中；
 * reloadable （可选） — 如果设置为false，当XML文件被外部更改或状态更改时，项目或应用需要重新加载。
 * @Storage
 * @Storage("yourName.xml") 如果组件时项目级的 — 标准.ipr项目文件会被自动使用，你不必指定这个值；
 * @Storage(StoragePathMacros.WORKSPACE_FILE) 值保存在工作空间文件。
 * @Storage注解的roamingType参数指定可漫游类型，需要启用Settings Repository插件。
 */
@State(name = "config", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class ServerPersistent implements PersistentStateComponent<ServiceConfig> {
    private ServiceConfig serviceConfig = new ServiceConfig();

    public static ServerPersistent getInstance() {
        return ServiceManager.getService(ServerPersistent.class);
    }


    /**
     * 方法在每次设置保存（例如关闭IDE）时被调用。如果getState()返回的状态与默认状态（通过状态类的默认构造方法）相等，不会在XML中保存状态。否则返回的状态将被序列化后保存。
     *
     * @return
     */
    @Nullable
    @Override
    public ServiceConfig getState() {
        return serviceConfig;
    }

    /**
     * 方法在组件创建后（只有组件有一些非默认状态时）或保存状态的XML被外部更改（例如，项目文件被版本控制系统更改）后被调用。后一种情况，组件会根据改变的状态更新UI和其他相关组件。
     *
     * @param state
     */
    @Override
    public void loadState(ServiceConfig state) {
        XmlSerializerUtil.copyBean(state, serviceConfig);
    }

    @Override
    public void noStateLoaded() {

    }
}

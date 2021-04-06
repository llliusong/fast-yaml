package com.pine.fast.plugin.persistent;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

/**
 * 持久化方案
 * 1.PropertiesComponent
 * 对于一些简单少量的值，我们可以使用 PropertiesComponent，它可以保存 application 级别和 project 级别的值。PropertiesComponent 保存的是键值对，由于所有插件使用的是同一个 namespace，强烈建议使用前缀来命名 name，比如使用 plugin id。
 * 2.PersistentStateComponent
 * 用于持久化比较复杂的 components 或 services，可以指定需要持久化的值、值的格式以及存储位置。
 *
 * name （必须） — 指定状态的名称（XML中的根标签名） storages 一个或多个@com.intellij.openapi.components.Storage注解指定存储位置。对于项目级的值是可选的 —
 * 这种情况状态会保存在标准项目文件中； reloadable （可选） — 如果设置为false，当XML文件被外部更改或状态更改时，项目或应用需要重新加载。
 *
 * 若是 application 级别的组件 运行调试时 xml 文件的位置： ~/IdeaICxxxx/system/plugins-sandbox/config/options 正式环境时 xml 文件的位置： ~/IdeaICxxxx/config/options
 * 若是 project 级别的组件，默认为项目的 .idea/misc.xml，若指定为 StoragePathMacros.WORKSPACE_FILE，则会被保存在 .idea/worksapce.xml
 * @Storage
 * @Storage("yourName.xml") 如果组件时项目级的 — 标准.ipr项目文件会被自动使用，你不必指定这个值；
 * @Storage(StoragePathMacros.WORKSPACE_FILE) 值保存在工作空间文件。
 * @Storage注解的roamingType参数指定可漫游类型，需要启用Settings Repository插件。
 *
 * https://www.cnblogs.com/kancy/p/10654569.html
 */
@State(name = "config", storages = {@Storage(value = "ServerPersistent.xml")})
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

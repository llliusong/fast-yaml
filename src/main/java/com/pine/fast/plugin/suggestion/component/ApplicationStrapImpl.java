package com.pine.fast.plugin.suggestion.component;

/**
 * 项目启动时调用，初始化配置，索引等 应尽可能避免在应用程序启动时执行代码，因为这会减慢启动速度。插件代码仅应在打开项目（请参阅Project Open）或用户调用插件的操作时执行。如果无法避免这种情况，请添加订阅AppLifecycleListener主题的侦听器。
 * https://plugins.jetbrains.com/docs/intellij/plugin-components.html#project-open
 *
 * @author pine
 * @date 2021/3/31 12:08 上午.
 */
public class ApplicationStrapImpl //implements AppLifecycleListener {
{
//
//    @Override
//    public void appStarted() {
//        SuggestionService service = ServiceManager.getService(SuggestionService.class);
//        System.out.println("333333333");
//    }
}

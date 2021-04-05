package com.pine.fast.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.pine.fast.plugin.misc.Icons;

public class ReloadAction extends AnAction {
    public ReloadAction() {
        super("加载自定义配置");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Messages.showMessageDialog("功能开发中", "Fast Yaml", Messages.getWarningIcon());

    }
}

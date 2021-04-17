package com.pine.fast.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.pine.fast.plugin.persistent.SystemConfig;

public class OpenHitAction extends AnAction {

    public OpenHitAction() {
        super(getHintText());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        SystemConfig.setHint();
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setText(getHintText());
    }

    private static String getHintText() {
        return SystemConfig.getHint() ? "Close Hint" : "Open Hint";
    }
}

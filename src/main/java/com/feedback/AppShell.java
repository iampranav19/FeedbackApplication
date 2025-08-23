package com.feedback;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * This class is used to configure the application shell.
 * In Vaadin 24+, theme configuration should be moved here instead of MainLayout.
 */
@Theme(themeClass = Lumo.class)
public class AppShell implements AppShellConfigurator {
    // Configuration happens through annotations
}
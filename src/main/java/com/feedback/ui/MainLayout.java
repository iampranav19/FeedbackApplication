package com.feedback.ui;

import com.feedback.model.Role;
import com.feedback.ui.views.actionitems.ActionItemView;
import com.feedback.ui.views.analytics.AnalyticsView;
import com.feedback.ui.views.dashboard.DashboardView;
import com.feedback.ui.views.feedback.FeedbackFormView;
import com.feedback.ui.views.feedback.FeedbackListView;
import com.feedback.ui.views.templates.TemplateView;
import com.feedback.ui.views.users.UserView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.Lumo;

// Removed @Theme annotation from here as it's now in AppShell class
public class MainLayout extends AppLayout {

    private final Tabs menu;

    public MainLayout() {
        createHeader();
        menu = createMenu();
        addToDrawer(createDrawerContent(menu));
    }

    private void createHeader() {
        H1 logo = new H1("Feedback System");
        logo.addClassNames("text-l", "m-m");

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                logo
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private VerticalLayout createDrawerContent(Tabs menu) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().set("spacing-s", true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        layout.add(menu);
        return layout;
    }

    private Tabs createMenu() {
        final Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.add(createMenuItems());
        return tabs;
    }

    private Tab[] createMenuItems() {
        return new Tab[]{
                createTab(VaadinIcon.DASHBOARD, "Dashboard", DashboardView.class),
                createTab(VaadinIcon.USERS, "Users", UserView.class),
                createTab(VaadinIcon.COMMENT, "Give Feedback", FeedbackFormView.class),
                createTab(VaadinIcon.LIST, "View Feedback", FeedbackListView.class),
                createTab(VaadinIcon.CLIPBOARD_TEXT, "Templates", TemplateView.class),
                createTab(VaadinIcon.TASKS, "Action Items", ActionItemView.class),
                createTab(VaadinIcon.CHART, "Analytics", AnalyticsView.class)
        };
    }

    private static Tab createTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        RouterLink link = new RouterLink();
        link.setRoute(viewClass);

        HorizontalLayout layout = new HorizontalLayout(viewIcon.create(), new com.vaadin.flow.component.html.Span(viewName));
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        link.add(layout);
        
        Tab tab = new Tab();
        tab.add(link);
        return tab;
    }
}
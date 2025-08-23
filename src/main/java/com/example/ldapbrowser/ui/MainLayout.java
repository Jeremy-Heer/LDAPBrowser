package com.example.ldapbrowser.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Root application layout with a side drawer and a top navbar used as view header.
 */
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final H1 viewTitle = new H1();

    public MainLayout() {
        // Navbar content (to the side of the drawer toggle)
        DrawerToggle toggle = new DrawerToggle();
        viewTitle.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        addToNavbar(toggle, viewTitle);

        // Drawer content
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Legacy", LegacyView.class));

        Scroller scroller = new Scroller(nav);
        scroller.addClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller);

        setPrimarySection(Section.DRAWER);
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        if (getContent() == null) {
            return "";
        }
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title != null ? title.value() : "";
    }
}

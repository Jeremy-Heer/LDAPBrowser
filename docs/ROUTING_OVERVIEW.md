# LDAP Browser Routing Overview

This document provides a comprehensive overview of all application routes, the components responsible for serving each route, and details on components that perform redirects to other routes.

---

## Main Routes and Components

| Route Pattern                | Component Class                  | Description / Notes                       |
|------------------------------|----------------------------------|-------------------------------------------|
| `/`                          | `IndexView`                      | Main landing page                         |
| `/servers`                   | `ServersView`                    | LDAP server list and management           |
| `/settings`                  | `SettingsView`                    | Application settings                      |
| `/access`                    | `AccessView`                      | Access control and permissions            |
| `/select/:sid`               | `SelectServerView`                | Selects a server by ID, then redirects    |
| `/group/:group`              | `SelectGroupView`                 | Selects a group, then redirects           |
| `/group/:group/:sid`         | `SelectGroupServerView`           | Selects server by group and ID, then redirects |
| `/group-search/:group`       | `GroupSearchView`                 | Search within a group                     |
| `/servers/:group`            | `ServersGroupView`                | Server list for a group                   |
| `/servers/:group/:sid`       | `ServersGroupServerView`          | Server details for a group and server     |

---

## Components That Redirect (Forward) to Other Routes

Some components are designed to process route parameters, perform selection logic, and then immediately forward the user to another route. These are typically lightweight views with no visible UI.

### `SelectGroupServerView`
- **Route:** `/group/:group/:sid`
- **Redirects to:** `ServersView`
- **Logic:** Selects a server by group and ID, then forwards to the main server view.

### `SelectServerView`
- **Route:** `/select/:sid`
- **Redirects to:** `ServersView`
- **Logic:** Selects a server by ID, then forwards to the main server view.

### `SelectGroupView`
- **Route:** `/group/:group`
- **Redirects to:** `ServersView`
- **Logic:** Selects a group, then forwards to the main server view.

---

## Example: Redirect Logic in `SelectGroupServerView`

```java
@Route(value = "group/:group/:sid", layout = MainLayout.class)
public class SelectGroupServerView extends Div implements BeforeEnterObserver {
  // ...existing code...
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // ...selection logic...
    event.forwardTo(ServersView.class);
  }
}
```

---

## Notes
- All redirecting components use the `event.forwardTo(...)` method to forward to the target view.
- Main UI views (such as `ServersView`, `SettingsView`, etc.) are responsible for rendering the application interface.
- Route parameters (e.g., `:group`, `:sid`) are used for selection and navigation logic.

---

## How to Add New Routes
- Define a new component class and annotate with `@Route`.
- Specify the route pattern and layout.
- Implement any selection or redirect logic as needed.

---

For further details, see the source files in `src/main/java/com/ldapweb/ldapbrowser/ui/`.

| Route Pattern                | Component Class                  | Description / Notes                       |
|------------------------------|----------------------------------|-------------------------------------------|
| `/`                          | `IndexView`                      | * Welcome page. Todo help and usage.        |
| `/servers`                   | `New Does not exist today`       | * Displays all server cards. Cards that are similar to cards in ServersGroupView. Each server card links to `/servers/:sid` |
| `/servers/:sid`              | `ServersView`                    | * Single Server LDAP Management. Only route updated. Same "ServersView" content |
| `/settings`                  | `SettingsView`                    | Application settings                      |
| `/access`                    | `AccessView`                      | * Route and component not needed. AccessControlsTab will remain and is accessed from the |
| `/select/:sid`               | `SelectServerView`                | Selects a server by ID, then redirects    |
| `/group/:group`              | `SelectGroupView`                 | Selects a group, then redirects           |
| `/group/:group/:sid`         | `SelectGroupServerView`           | Selects server by group and ID, then redirects |
| `/group-search/:group`       | `GroupSearchView`                 | Search within a group                     |
| `/servers/:group`            | `ServersGroupView`                | Server list for a group                   |
| `/servers/:group/:sid`       | `ServersGroupServerView`          | Server details for a group and server     |

* = Updates
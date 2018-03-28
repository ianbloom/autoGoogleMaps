# autoGoogleMaps

## Introduction

This DataSource was motivated by a LogicMonitor customer request for a script that automatically creates/updates dashboards
corresponding to groups in the device tree.  These dashboards each contain two widgets.  One widget is a Google Maps widget
with map points corresponding to subgroups/devices.  The other widget is a text widget containing links to the other
dashboards.

## The Process

### Helper Functions

In order to make efficient use of my time, I wrote helper functions for making HTTP requests to the LogicMonitor API.  The 
functions LMGET(), LMPOST(), and LMPUT() do... exactly what you think they would do.  However, they automatically open and
close an HttpClient, and circumvent difficulties involved with making many API calls, primarily variable bloat.

### The Nitty Gritty

The user must define a number of properties on the device this script runs against.  The user must specify an API access ID,
and API access key, the account we will be querying, a dashboard group name, the name of the group they would like the 
script to treat as the root, and the root group's associated LogicMonitor group ID.

We first query the list of dashboards for a dashboard with the same name as the root group.  If this dashboard exists, we
capture its ID, and if not, we create this dashboard.

Then, we query the list of dashboards for dashboards with the same name as the subgroups.  If these dashboards exist, we 
capture their dashboard ID, and if not, they are created.

On the subgroup dashboards, we create a Google Maps widget with map points corresponding to devices in the subgroup.  The menu
widget contains a single link, which refers the user back to the root group dashboard.

Finally, on the root dashboard, we create a Google Maps widget with map points corresponding to subgroups (i.e. states in the
USA), and a menu widget which contains links to each of the subgroup dashboards.

### Still to be done...

- [ ] Beautify the dashboards
- [ ] Handle subgroup deletion
- [ ] Duplicate dashboards according to a user specified template

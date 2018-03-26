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

### Other shiz

Work in progress... Ideally this script will scrub the LogicMonitor device tree for location information, and create a dashboard for each location containing a relevant maps widget, and a text widget linking to the other dashboards containing similar maps.

dashboardTrickery.groovy is the current working file.

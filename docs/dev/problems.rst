Problems
========

Why doesn't it work well on Linux?
----------------------------------

I've experimented a bit on Linux and searched for ways to put a window
as a wallpaper, but at least with KDE Plasma none of them really worked.

This is because the desktop background and the desktop icons appear to be
in the same windows. Hence, it seems to be impossible to put a window between
them.

We change the property ``_NET_WM_WINDOW_TYPE`` of our window to
``_NET_WM_WINDOW_TYPE_DESKTOP`` which makes it appear as the desktop window.
Sadly our window is still above the original desktop window and therefore
covers all symbols and prevents you from interacting with your desktop.

Making sure our window is below the desktop seems hard and wouldn't really help,
because then the desktop window (which included the desktop background) would
cover our window instead.

I need to experiment a bit more;
maybe setting a transparent wallpaper could fix this.

What's the problem on GNOME/Ubuntu?
-----------------------------------

The application uses an icon in the system tray which you can click to open
or close the configuration window.

Ubuntu/GNOME does not support the system tray anymore.

There might be some plugins providing a system tray, but I haven't tested them yet.

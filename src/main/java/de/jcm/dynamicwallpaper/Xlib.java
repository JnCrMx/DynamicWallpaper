package de.jcm.dynamicwallpaper;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.X11;

public interface Xlib extends Library
{
	Xlib INSTANCE = Native.load("X11", Xlib.class);

	int XReparentWindow(X11.Display display, X11.Window w, X11.Window parent, int x, int y);
	int XLowerWindow(X11.Display display, X11.Window w);
}

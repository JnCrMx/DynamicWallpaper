package de.jcm.dynamicwallpaper;

import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.system.windows.User32.*;

public class Utils
{
	public static InputStream openURLStream(URL url) throws IOException
	{
		URLConnection connection = url.openConnection();
		// Some servers block Java 8's HTTP client.
		connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/77.0");
		return connection.getInputStream();
	}

	private static WinDef.HWND getWorkerW()
	{
		WinDef.HWND progman =  User32.INSTANCE.FindWindow("Progman", null);

		User32.INSTANCE.SendMessage(progman, 0x052C, new WinDef.WPARAM(0xD), new WinDef.LPARAM(0));
		User32.INSTANCE.SendMessage(progman, 0x052C, new WinDef.WPARAM(0xD), new WinDef.LPARAM(1));

		AtomicReference<WinDef.HWND> workerRef = new AtomicReference<>();
		User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC()
		{
			@Override
			public boolean callback(WinDef.HWND hWnd, Pointer data)
			{
				if(User32.INSTANCE.FindWindowEx(hWnd, null, "SHELLDLL_DefView", null)==null)
					return true;

				WinDef.HWND worker = User32.INSTANCE.FindWindowEx(null, hWnd, "WorkerW", null);
				if(worker != null)
				{
					workerRef.set(worker);
				}

				return true;
			}
		}, null);
		return workerRef.get();
	}

	public static void makeWallpaper(long window)
	{
		if(Platform.isWindows())
			windowsMakeWallpaper(window);
		else if(Platform.isLinux() && Platform.isX11())
			linuxMakeWallpaper(window);
		else
			throw new UnsupportedOperationException("not supported on this platform");
	}

	public static void destroyWallpaper(long window)
	{
		if(Platform.isWindows())
			windowsDestroyWallpaper(window);
		else if(Platform.isLinux() && Platform.isX11())
		{
			// We don't need to do anything here I think
		}
		else
			throw new UnsupportedOperationException("not supported on this platform");
	}

	private static void windowsMakeWallpaper(long window)
	{
		long nativeWindow = GLFWNativeWin32.glfwGetWin32Window(window);

		// procedure from https://github.com/Francesco149/weebp
		WinDef.HWND thisWindow = new WinDef.HWND(new Pointer(nativeWindow));
		WinDef.HWND workerW = getWorkerW();

		WinDef.RECT rect = new WinDef.RECT();
		User32.INSTANCE.GetWindowRect(thisWindow, rect);

		long style = User32.INSTANCE.GetWindowLong(thisWindow, User32.GWL_STYLE);
		style &= ~(
				WS_CAPTION |
						WS_THICKFRAME |
						WS_SYSMENU |
						WS_MAXIMIZEBOX |
						WS_MINIMIZEBOX
		);
		style |= User32.WS_CHILD;
		User32.INSTANCE.SetWindowLong(thisWindow, User32.GWL_STYLE, (int) style);

		// not sure if we need those, but better keep them in
		long exStyle = User32.INSTANCE.GetWindowLong(thisWindow, User32.GWL_EXSTYLE);
		exStyle &= ~(
				WS_EX_DLGMODALFRAME |
						WS_EX_COMPOSITED |
						WS_EX_WINDOWEDGE |
						WS_EX_CLIENTEDGE |
						WS_EX_LAYERED |
						WS_EX_STATICEDGE |
						WS_EX_TOOLWINDOW |
						WS_EX_APPWINDOW
		);
		User32.INSTANCE.SetWindowLong(thisWindow, User32.GWL_EXSTYLE, (int) exStyle);

		User32.INSTANCE.SetParent(thisWindow, workerW);
		User32.INSTANCE.ShowWindow(thisWindow, User32.SW_SHOW);

		// not sure wtf we do here, but it seems to work (not really well, but idk)
		User32.INSTANCE.MoveWindow(thisWindow, 0, rect.top, rect.right,
		                           rect.bottom+10, false);
		rect.clear();
	}

	private static void windowsDestroyWallpaper(long window)
	{
		long nativeWindow = GLFWNativeWin32.glfwGetWin32Window(window);

		// procedure from https://github.com/Francesco149/weebp
		WinDef.HWND thisWindow = new WinDef.HWND(new Pointer(nativeWindow));

		User32.INSTANCE.SetParent(thisWindow, User32.INSTANCE.GetDesktopWindow());

		long style = User32.INSTANCE.GetWindowLong(thisWindow, User32.GWL_STYLE);
		style |= User32.WS_OVERLAPPEDWINDOW;
		User32.INSTANCE.SetWindowLong(thisWindow, User32.GWL_STYLE, (int) style);

		// not sure if we need those, but better keep them in
		long exStyle = User32.INSTANCE.GetWindowLong(thisWindow, User32.GWL_EXSTYLE);
		exStyle |= WS_EX_APPWINDOW;
		User32.INSTANCE.SetWindowLong(thisWindow, User32.GWL_EXSTYLE, (int) exStyle);

		getWorkerW();
	}

	private static void linuxMakeWallpaper(long window)
	{
		long nativeWindow = GLFWNativeX11.glfwGetX11Window(window);
		X11.Window thisWindow = new X11.Window(nativeWindow);
		X11.Display display = X11.INSTANCE.XOpenDisplay(null);

		Memory memory = new Memory(8);

		/*
		X11.Window desktop = findByWindowType(display, X11.INSTANCE.XDefaultRootWindow(display),
		                                      xAtom(display, "_NET_WM_WINDOW_TYPE_DESKTOP"));
		memory.setNativeLong(0, xAtom(display, "_NET_WM_WINDOW_TYPE_DESKTOP"));
		X11.INSTANCE.XChangeProperty(display, desktop, xAtom(display, "_NET_WM_WINDOW_TYPE"), X11.XA_ATOM,
		                             32, X11.PropModeReplace, memory, 1);
		*/

		memory.setNativeLong(0, xAtom(display, "_NET_WM_WINDOW_TYPE_DESKTOP"));
		X11.INSTANCE.XChangeProperty(display, thisWindow, xAtom(display, "_NET_WM_WINDOW_TYPE"), X11.XA_ATOM,
		                             32, X11.PropModeReplace, memory, 1);

		memory.setNativeLong(0, xAtom(display, "_NET_WM_STATE_BELOW"));
		X11.INSTANCE.XChangeProperty(display, thisWindow, xAtom(display, "_NET_WM_STATE"), X11.XA_ATOM,
		                             32, X11.PropModeAppend, memory, 1);

		X11.INSTANCE.XFlush(display);
		X11.INSTANCE.XCloseDisplay(display);
	}

	/*
	private static X11.Window findByWindowType(X11.Display display, X11.Window window, X11.Atom windowType)
	{
		X11.WindowByReference root = new X11.WindowByReference();
		X11.WindowByReference parent = new X11.WindowByReference();
		PointerByReference children = new PointerByReference();
		IntByReference childrenCount = new IntByReference();

		X11.INSTANCE.XQueryTree(display, window, root, parent, children, childrenCount);

		Pointer pointer = children.getValue();

		for(int i=0; i<childrenCount.getValue(); i++)
		{
			X11.Window child = new X11.Window(pointer.getLong(8*i));

			X11.AtomByReference type = new X11.AtomByReference();
			IntByReference format = new IntByReference();
			NativeLongByReference itemCount = new NativeLongByReference();
			NativeLongByReference bytesToRead = new NativeLongByReference();
			PointerByReference prop = new PointerByReference();

			X11.INSTANCE.XGetWindowProperty(display, child, xAtom(display, "_NET_WM_WINDOW_TYPE"),
			                                new NativeLong(0), new NativeLong(8), false,
			                                X11.XA_ATOM, type, format, itemCount, bytesToRead, prop);

			if(itemCount.getValue().longValue() == 1)
			{
				X11.Atom atom = new X11.Atom(prop.getValue().getLong(0));

				if(atom.equals(windowType))
					return child;
			}

			X11.Window result = findByWindowType(display, child, windowType);
			if(result != null)
				return result;
		}

		return null;
	}
	*/

	private static X11.Atom xAtom(X11.Display display, String name)
	{
		return X11.INSTANCE.XInternAtom(display, name, false);
	}

	public static String read(String path) throws IOException
	{
		try(InputStream in = DynamicWallpaper.class.getResourceAsStream(path))
		{
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
	}
}

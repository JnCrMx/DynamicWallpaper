package de.jcm.dynamicwallpaper;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFWNativeWin32;

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
		else
			throw new UnsupportedOperationException("only available on Windows");
	}

	public static void destroyWallpaper(long window)
	{
		if(Platform.isWindows())
			windowsDestroyWallpaper(window);
		else
			throw new UnsupportedOperationException("only available on Windows");
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

	public static String read(String path) throws IOException
	{
		try(InputStream in = DynamicWallpaper.class.getResourceAsStream(path))
		{
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
	}
}

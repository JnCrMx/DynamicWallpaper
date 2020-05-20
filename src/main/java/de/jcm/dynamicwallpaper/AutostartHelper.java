package de.jcm.dynamicwallpaper;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WTypes;
import mslinks.ShellLink;
import mslinks.ShellLinkException;

import java.io.File;
import java.io.IOException;

public class AutostartHelper
{
	public static final String LINK_NAME = DynamicWallpaper.class.getSimpleName()+".lnk";

	public static boolean hasAutostart()
	{
		return getAutostartFile() != null;
	}

	public static File getAutostartFile()
	{
		String roaming = System.getenv("APPDATA");
		File autostartFolder = new File(roaming, "Microsoft\\Windows\\Start Menu\\Programs\\Startup");

		File[] links = autostartFolder.listFiles(f->f.getName().endsWith(".lnk"));
		for(File link : links)
		{
			if(link.getName().equals(LINK_NAME))
				return link;
			try
			{
				ShellLink shellLink = new ShellLink(link);
				if(shellLink.resolveTarget().equals(getJavaExecutable()) &&
						shellLink.getCMDArgs().equals(getJavaArguments()))
					return link;
			}
			catch(IOException | ShellLinkException ignored)
			{
			}
		}

		return null;
	}

	public static void registerAutostart()
	{
		String roaming = System.getenv("APPDATA");
		File autostartFolder = new File(roaming, "Microsoft\\Windows\\Start Menu\\Programs\\Startup");

		try
		{
			ShellLink.createLink(getJavaExecutable())
					.setCMDArgs(getJavaArguments())
					.setWorkingDir(new File(".").getAbsolutePath())
					.saveTo(autostartFolder.getAbsolutePath()+"\\"+LINK_NAME);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void unregisterAutostart()
	{
		File file = getAutostartFile();
		if(file != null && file.exists())
			file.delete();
	}

	public static String getCommand()
	{
		return Kernel32Processenv.INSTANCE.GetCommandLineA().getValue();
	}

	public static String getJavaExecutable()
	{
		String command = getCommand();

		int javaStart = command.indexOf('"');
		int javaEnd = command.indexOf('"', javaStart+1);

		return command
				.substring(javaStart, javaEnd)
				.replace("\"", "");
	}

	public static String getJavaArguments()
	{
		String command = getCommand();

		int javaStart = command.indexOf('"');
		int javaEnd = command.indexOf('"', javaStart+1);

		return command
				.substring(javaEnd+2);
	}

	interface Kernel32Processenv extends Library
	{
		Kernel32Processenv INSTANCE = Native.load("Kernel32", Kernel32Processenv.class);

		WTypes.LPSTR GetCommandLineA();
	}
}

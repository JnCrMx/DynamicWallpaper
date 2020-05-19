package de.jcm.dynamicwallpaper;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutostartHelper
{
	private static Pair<String, String> autostart;
	
	public static void init()
	{
		findAutostarts();
	}
	
	private static void findAutostarts()
	{
		autostart = null;
		
		String key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
		String pattern = "    (.*)    REG_SZ    ((\"|)[A-Z]:\\\\.*(\"|))";
		try
		{
			Process process = new ProcessBuilder("reg", "query", key, "/t", "REG_SZ").redirectOutput(Redirect.PIPE).start();
			InputStream in = process.getInputStream();
			Scanner scanner = new Scanner(in);

			while(scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				Matcher matcher = Pattern.compile(pattern).matcher(line);
				if(matcher.matches())
				{
					String name = matcher.group(1);
					String command = matcher.group(2);

					if(name.equals("DynamicWallpaper"))
					{
						autostart = new ImmutablePair<>(name, command);
						break;
					}
				}
			}
			scanner.close();
			in.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void registerAutostart(String command)
	{
		String key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
		String name = "DynamicWallpaper";
		String value = command.replace("\"", "\\\"");

		try
		{
			ProcessBuilder builder = new ProcessBuilder("reg", "add", key, "/v", name, "/t", "REG_SZ", "/d", value, "/f")
					.redirectOutput(Redirect.INHERIT);
			Process process = builder.start();
			
			int exitCode = process.waitFor();
			if(exitCode != 0)
			{
				new RuntimeException("reg returned exit code "+exitCode).printStackTrace();
			}
		}
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		
		findAutostarts();
	}
	
	public static void unregisterAutostart()
	{
		String key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
		String name = autostart.getLeft();
		
		try
		{
			ProcessBuilder builder = new ProcessBuilder("reg", "delete", key, "/v", name, "/f")
					.redirectOutput(Redirect.INHERIT);
			Process process = builder.start();
			
			int exitCode = process.waitFor();
			if(exitCode != 0)
			{
				new RuntimeException("reg returned exit code "+exitCode).printStackTrace();
			}
		}
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		
		findAutostarts();
	}

	public static Pair<String, String> getAutostart()
	{
		return autostart;
	}

	public static String getCommand()
	{
		File myPath = new File(".").getAbsoluteFile();

		return Kernel32Processenv.INSTANCE.GetCommandLineA().getValue()+
				" \""+myPath.getAbsolutePath()+"\"";
	}

	interface Kernel32Processenv extends Library
	{
		Kernel32Processenv INSTANCE = Native.load("Kernel32", Kernel32Processenv.class);

		WTypes.LPSTR GetCommandLineA();
	}
}

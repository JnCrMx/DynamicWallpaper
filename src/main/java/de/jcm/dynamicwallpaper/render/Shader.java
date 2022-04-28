package de.jcm.dynamicwallpaper.render;

import de.jcm.dynamicwallpaper.Utils;

import java.io.IOException;

import static org.lwjgl.opengl.GL33.*;

public class Shader
{
	private final int id;

	public Shader(int type)
	{
		id = glCreateShader(type);
	}

	public void source(CharSequence source)
	{
		glShaderSource(id, source);
	}

	public void compile()
	{
		glCompileShader(id);

		checkStatus();
	}

	private void checkStatus()
	{
		int status = glGetShaderi(id, GL_COMPILE_STATUS);
		if(status != GL_TRUE)
		{
			throw new RuntimeException(glGetShaderInfoLog(id));
		}
	}

	public void delete()
	{
		glDeleteShader(id);
	}

	public int getID()
	{
		return id;
	}

	public static Shader createShader(int type, CharSequence source)
	{
		Shader shader = new Shader(type);
		shader.source(source);
		shader.compile();

		return shader;
	}

	public static Shader loadShader(int type, String path) throws IOException
	{
		return createShader(type, Utils.read(path));
	}

}

package de.jcm.dynamicwallpaper.render;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;

public class Texture
{
	private final int id;

	private int width, height;

	public Texture()
	{
		id = glGenTextures();
	}

	public void bind()
	{
		glBindTexture(GL_TEXTURE_2D, id);
	}

	public void setParameter(int parameter, int value)
	{
		glTexParameteri(GL_TEXTURE_2D, parameter, value);
	}

	public void uploadData(int width, int height, ByteBuffer data)
	{
		uploadData(GL_RGBA8, width, height, GL_RGBA, data);
	}

	public void uploadData(int internalFormat, int width, int height, int format, ByteBuffer data)
	{
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, data);
	}

	public void delete()
	{
		glDeleteTextures(id);
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setWidth(int width)
	{
		if(width>0)
			this.width = width;
		else
			throw new IllegalArgumentException("width must be > 0");
	}

	public void setHeight(int height)
	{
		if(height>0)
			this.height = height;
		else
			throw new IllegalArgumentException("height must be > 0");
	}

	public int getID()
	{
		return id;
	}

	public static Texture createTexture(int width, int height, ByteBuffer data)
	{
		Texture texture = new Texture();
		texture.setWidth(width);
		texture.setHeight(height);

		texture.bind();

		texture.setParameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		texture.setParameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);

		texture.uploadData(GL_RGBA8, width, height, GL_RGBA, data);

		return texture;
	}
}

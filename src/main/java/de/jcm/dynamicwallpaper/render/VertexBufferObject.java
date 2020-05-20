package de.jcm.dynamicwallpaper.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public class VertexBufferObject implements AutoCloseable
{
	private final int id;

	public VertexBufferObject()
	{
		id = glGenBuffers();
	}

	public void bind(int target)
	{
		glBindBuffer(target, id);
	}

	public void delete()
	{
		glDeleteBuffers(id);
	}

	@Override
	public void close()
	{
		delete();
	}

	public void uploadData(int target, FloatBuffer data, int usage)
	{
		glBufferData(target, data, usage);
	}

	public void uploadData(int target, long size, int usage)
	{
		glBufferData(target, size, usage);
	}

	public void uploadSubData(int target, long offset, FloatBuffer data)
	{
		glBufferSubData(target, offset, data);
	}

	public void uploadData(int target, IntBuffer data, int usage)
	{
		glBufferData(target, data, usage);
	}

	public int getID()
	{
		return id;
	}
}

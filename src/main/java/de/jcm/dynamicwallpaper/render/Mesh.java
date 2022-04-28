package de.jcm.dynamicwallpaper.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Mesh
{
	private final VertexArrayObject voa;

	private final VertexBufferObject vertexBuffer;
	private final VertexBufferObject textureCoordBuffer;
	private final VertexBufferObject elementBuffer;

	public Mesh()
	{
		voa = new VertexArrayObject();
		vertexBuffer = new VertexBufferObject();
		textureCoordBuffer = new VertexBufferObject();
		elementBuffer = new VertexBufferObject();
	}

	public void fill(FloatBuffer vertices, FloatBuffer textureCoords, IntBuffer elements,
	                 ShaderProgram program)
	{
		voa.bind();

		vertexBuffer.bind(GL_ARRAY_BUFFER);
		vertexBuffer.uploadData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

		int posAttrib = program.getAttributeLocation("position");
		program.enableVertexAttribute(posAttrib);
		program.pointVertexAttribute(posAttrib, 3, 3 * Float.BYTES, 0);

		textureCoordBuffer.bind(GL_ARRAY_BUFFER);
		textureCoordBuffer.uploadData(GL_ARRAY_BUFFER, textureCoords, GL_STATIC_DRAW);

		int texAttrib = program.getAttributeLocation("texcoord");
		program.enableVertexAttribute(texAttrib);
		program.pointVertexAttribute(texAttrib, 2, 2 * Float.BYTES, 0);

		elementBuffer.bind(GL_ELEMENT_ARRAY_BUFFER);
		elementBuffer.uploadData(GL_ELEMENT_ARRAY_BUFFER, elements, GL_STATIC_DRAW);
	}

	public void bind()
	{
		voa.bind();
	}

	public void delete()
	{
		vertexBuffer.delete();
		textureCoordBuffer.delete();
		elementBuffer.delete();

		voa.delete();
	}
}

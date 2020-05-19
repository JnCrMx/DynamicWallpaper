package de.jcm.dynamicwallpaper.render;

import org.joml.*;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

public class ShaderProgram
{
	/**
	 * Stores the handle of the program.
	 */
	private final int id;

	/**
	 * Creates a shader program.
	 */
	public ShaderProgram() {
		id = glCreateProgram();
	}

	/**
	 * Attach a shader to this program.
	 *
	 * @param shader Shader to get attached
	 */
	public void attachShader(Shader shader) {
		glAttachShader(id, shader.getID());
	}

	/**
	 * Binds the fragment out color variable.
	 *
	 * @param number Color number you want to bind
	 * @param name   Variable name
	 */
	public void bindFragmentDataLocation(int number, CharSequence name) {
		glBindFragDataLocation(id, number, name);
	}

	/**
	 * Link this program and check it's status afterwards.
	 */
	public void link() {
		glLinkProgram(id);

		checkStatus();
	}

	/**
	 * Gets the location of an attribute variable with specified name.
	 *
	 * @param name Attribute name
	 *
	 * @return Location of the attribute
	 */
	public int getAttributeLocation(CharSequence name) {
		return glGetAttribLocation(id, name);
	}

	/**
	 * Enables a vertex attribute.
	 *
	 * @param location Location of the vertex attribute
	 */
	public void enableVertexAttribute(int location) {
		glEnableVertexAttribArray(location);
	}

	/**
	 * Disables a vertex attribute.
	 *
	 * @param location Location of the vertex attribute
	 */
	public void disableVertexAttribute(int location) {
		glDisableVertexAttribArray(location);
	}

	/**
	 * Sets the vertex attribute pointer.
	 *
	 * @param location Location of the vertex attribute
	 * @param size     Number of values per vertex
	 * @param stride   Offset between consecutive generic vertex attributes in
	 *                 bytes
	 * @param offset   Offset of the first component of the first generic vertex
	 *                 attribute in bytes
	 */
	public void pointVertexAttribute(int location, int size, int stride, int offset) {
		glVertexAttribPointer(location, size, GL_FLOAT, false, stride, offset);
	}

	/**
	 * Gets the location of an uniform variable with specified name.
	 *
	 * @param name Uniform name
	 *
	 * @return Location of the uniform
	 */
	public int getUniformLocation(CharSequence name) {
		return glGetUniformLocation(id, name);
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, int value) {
		glUniform1i(location, value);
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Vector2f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniform2fv(location, value.get(stack.mallocFloat(2)));
		}
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Vector3f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniform3fv(location, value.get(stack.mallocFloat(3)));
		}
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Vector4f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniform4fv(location, value.get(stack.mallocFloat(4)));
		}
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Matrix2f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniformMatrix2fv(location, false, value.get(stack.mallocFloat(2 * 2)));
		}
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Matrix3f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniformMatrix3fv(location, false, value.get(stack.mallocFloat(3 * 3)));
		}
	}

	/**
	 * Sets the uniform variable for specified location.
	 *
	 * @param location Uniform location
	 * @param value    Value to set
	 */
	public void setUniform(int location, Matrix4f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			glUniformMatrix4fv(location, false, value.get(stack.mallocFloat(4 * 4)));
		}
	}

	public void setUniform(int location, float value)
	{
		glUniform1f(location, value);
	}

	/**
	 * Use this shader program.
	 */
	public void use() {
		glUseProgram(id);
	}

	/**
	 * Checks if the program was linked successfully.
	 */
	public void checkStatus() {
		int status = glGetProgrami(id, GL_LINK_STATUS);
		if (status != GL_TRUE) {
			throw new RuntimeException(glGetProgramInfoLog(id));
		}
	}

	/**
	 * Deletes the shader program.
	 */
	public void delete() {
		glDeleteProgram(id);
	}
}

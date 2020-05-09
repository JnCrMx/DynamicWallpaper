package de.jcm.dynamicwallpaper.extra;

import de.jcm.dynamicwallpaper.render.Mesh;
import de.jcm.dynamicwallpaper.render.Shader;
import de.jcm.dynamicwallpaper.render.ShaderProgram;
import de.jcm.dynamicwallpaper.render.Texture;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

public class LoadingScreen
{
	private static final int BUBBLE_COUNT = 6;
	private static final float BUBBLE_OFFSET = 1.5f;
	private static final float BUBBLE_SPEED = 2f;

	private ShaderProgram program;
	private Mesh bubble;
	private Texture texture;

	private int modelMatrixUniform;

	private final Matrix4f projectionMatirx;

	public LoadingScreen()
	{
		// too lazy to do something weird with perspective and stuff, so just use scale
		projectionMatirx = new Matrix4f().scale(1f/16, 1f/9f, 1f);
	}

	public void prepare() throws IOException
	{
		Shader vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "/shaders/loading.vsh");
		Shader fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "/shaders/loading.fsh");

		program = new ShaderProgram();
		program.attachShader(vertexShader);
		program.attachShader(fragmentShader);
		program.bindFragmentDataLocation(0, "fragColor");
		program.link();
		program.use();

		modelMatrixUniform = program.getUniformLocation("modelMatrix");
		int projectionMatrixUniform = program.getUniformLocation("projectionMatrix");

		program.setUniform(projectionMatrixUniform, projectionMatirx);

		try(MemoryStack stack = MemoryStack.stackPush())
		{
			/* Vertex data */
			FloatBuffer vertices = stack.mallocFloat(4 * 3);
			vertices.put(-1.0f).put(-1.0f).put(0.0f);
			vertices.put(-1.0f).put(1.0f).put(0.0f);
			vertices.put(1.0f).put(1.0f).put(0.0f);
			vertices.put(1.0f).put(-1.0f).put(0.0f);
			vertices.flip();

			/* Texture data */
			FloatBuffer textures = stack.mallocFloat(4 * 2);
			textures.put(0f).put(1f);
			textures.put(0f).put(0f);
			textures.put(1f).put(0f);
			textures.put(1f).put(1f);
			textures.flip();

			IntBuffer elements = stack.mallocInt(4 * 3);
			elements.put(0).put(1).put(2);
			elements.put(2).put(3).put(0);
			elements.flip();

			bubble = new Mesh();
			bubble.fill(vertices, textures, elements, program);
		}

		texture = new Texture();
		texture.bind();
		texture.setParameter(GL_TEXTURE_WRAP_S, GL_REPEAT);
		texture.setParameter(GL_TEXTURE_WRAP_T, GL_REPEAT);
		texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		BufferedImage image = ImageIO.read(getClass().getResource("/images/bubble.png"));

		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
		ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

		for(int h = 0; h < image.getHeight(); h++) {
			for(int w = 0; w < image.getWidth(); w++) {
				int pixel = pixels[h * image.getWidth() + w];

				buffer.put((byte) ((pixel >> 16) & 0xFF));
				buffer.put((byte) ((pixel >> 8) & 0xFF));
				buffer.put((byte) (pixel & 0xFF));
				buffer.put((byte) ((pixel >> 24) & 0xFF));
			}
		}

		buffer.flip();
		texture.uploadData(image.getWidth(), image.getHeight(), buffer);
	}

	public void render()
	{
		texture.bind();
		bubble.bind();
		program.use();

		for(int i=0; i<BUBBLE_COUNT; i++)
		{
			Matrix4f modelMatrix = new Matrix4f();
			modelMatrix = modelMatrix.scale(0.25f);

			double angle = Math.cos((GLFW.glfwGetTime()*BUBBLE_SPEED+
					BUBBLE_OFFSET *i/BUBBLE_COUNT)%Math.PI) * Math.PI + Math.PI;

			modelMatrix = modelMatrix.rotate((float) angle, 0.0f, 0.0f, 1.0f);
			modelMatrix = modelMatrix.translate(0.0f, 5.0f, 0.0f);

			program.setUniform(modelMatrixUniform, modelMatrix);

			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}
}

package de.jcm.dynamicwallpaper.extra;

import com.sun.jna.platform.win32.*;
import de.jcm.dynamicwallpaper.render.Mesh;
import de.jcm.dynamicwallpaper.render.Shader;
import de.jcm.dynamicwallpaper.render.ShaderProgram;
import de.jcm.dynamicwallpaper.render.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

public class BatteryOverlay
{
	private int batteryPercent;

	private ShaderProgram program;
	private Mesh battery;
	private Mesh batteryLevel;
	private Texture texture;

	private int modelMatrixUniform;
	private int colorUniform;
	private int wavePositionUniform;
	private int levelUniform;

	private final Matrix4f projectionMatirx;

	public BatteryOverlay()
	{
		// too lazy to do something weird with perspective and stuff, so just use scale
		projectionMatirx = new Matrix4f().scale(1f/16, 1f/9f, 1f);
	}

	public void prepare() throws IOException
	{
		Shader vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "/shaders/loading.vsh");
		Shader fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "/shaders/battery.fsh");

		program = new ShaderProgram();
		program.attachShader(vertexShader);
		program.attachShader(fragmentShader);
		program.bindFragmentDataLocation(0, "fragColor");
		program.link();
		program.use();

		modelMatrixUniform = program.getUniformLocation("modelMatrix");
		colorUniform = program.getUniformLocation("color");
		wavePositionUniform = program.getUniformLocation("wavePosition");
		levelUniform = program.getUniformLocation("level");
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

			batteryLevel = new Mesh();
			batteryLevel.fill(vertices, textures, elements, program);
		}
	}

	public void render()
	{
		batteryLevel.bind();
		program.use();

		Matrix4f modelMatrix = new Matrix4f().identity().scale(2, 1, 1);

		program.setUniform(colorUniform, new Vector3f(0.0f, 1.0f, 0.0f));
		program.setUniform(wavePositionUniform, (float) GLFW.glfwGetTime());
		program.setUniform(levelUniform, ((float) GLFW.glfwGetTime()/10.0f)%1.0f);
		program.setUniform(modelMatrixUniform, modelMatrix);

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
	}

	public void update()
	{
		WinNT.SYSTEM_BATTERY_STATE batteryState = new WinNT.SYSTEM_BATTERY_STATE();
		int result = PowrProf.INSTANCE.CallNtPowerInformation(
				PowrProf.POWER_INFORMATION_LEVEL.SystemBatteryState,
				null, 0,
				batteryState.getPointer(), batteryState.size());
	}
}

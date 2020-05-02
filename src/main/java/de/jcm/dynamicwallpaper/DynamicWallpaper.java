package de.jcm.dynamicwallpaper;

import de.jcm.dynamicwallpaper.colormode.ColorMode;
import de.jcm.dynamicwallpaper.colormode.HueWaveColorMode;
import de.jcm.dynamicwallpaper.render.Mesh;
import de.jcm.dynamicwallpaper.render.Shader;
import de.jcm.dynamicwallpaper.render.ShaderProgram;
import de.jcm.dynamicwallpaper.render.Texture;
import org.apache.commons.io.IOUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGR;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.system.MemoryUtil.NULL;

public class DynamicWallpaper
{
	// The window handle
	private long window;
	public long getWindow()
	{
		return window;
	}

	private ShaderProgram program;
	private Mesh cube;
	private int colorFilter;
	private Texture texture;

	private ControlFrame controlFrame;

	ColorMode colorMode;
	final AtomicReference<FFmpegFrameGrabber> frameGrabber = new AtomicReference<FFmpegFrameGrabber>();

	File videoFile;
	boolean relative;

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public void run()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		loadConfig();
		executor.scheduleAtFixedRate(()->{
			if(colorMode != null)
				colorMode.update();
		}, 1, 1, TimeUnit.SECONDS);

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}

		ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
		controlFrame = new ControlFrame(this);
		controlFrame.setIconImage(icon.getImage());

		SystemTray tray = SystemTray.getSystemTray();
		TrayIcon trayIcon = new TrayIcon(icon.getImage());
		trayIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				controlFrame.setVisible(!controlFrame.isVisible());
			}
		});
		try
		{
			tray.add(trayIcon);
		}
		catch(AWTException e)
		{
			e.printStackTrace();
		}

		init();
		loop();
		shutdown();
	}

	private void loadConfig()
	{
		try
		{
			File config = new File("config.json");
			if(!config.exists())
			{
				config.createNewFile();
			}
			JSONObject configObject;
			try(BufferedReader reader = new BufferedReader(new FileReader(config)))
			{
				String line = IOUtils.toString(reader);
				configObject = new JSONObject(line==null?"{}":line);
			}

			videoFile = new File(configObject.optString("video", "video.mp4"));
			if(!videoFile.isAbsolute())
				relative = true;

			String colorMode = configObject.optString("mode", HueWaveColorMode.class.getName());
			try
			{
				this.colorMode = (ColorMode) Class.forName(colorMode).getConstructor().newInstance();

				JSONObject modeConfig = configObject.optJSONObject("config");
				this.colorMode.load(modeConfig==null?new JSONObject():modeConfig);
			}
			catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void saveConfig()
	{
		JSONObject object = new JSONObject();

		Path p1 = videoFile.toPath().toAbsolutePath().normalize();
		Path p2 = new File(".").toPath().toAbsolutePath().normalize();

		Path videoPath;
		if(p1.getRoot().equals(p2.getRoot()) && relative)
		{
			videoPath = p2.relativize(p1);
		}
		else
		{
			videoPath = p1;
		}

		object.put("video", videoPath.toString());
		object.put("mode", colorMode.getClass().getName());

		JSONObject modeConfig = new JSONObject();
		colorMode.save(modeConfig);
		object.put("config", modeConfig);

		File config = new File("config.json");
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(config)))
		{
			writer.write(object.toString(4));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void shutdown()
	{
		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		Utils.destroyWallpaper(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();

		saveConfig();

		Runtime.getRuntime().halt(0);
	}

	private void init()
	{
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if(!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

		GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		assert mode != null;

		glfwWindowHint(GLFW_RED_BITS, mode.redBits());
		glfwWindowHint(GLFW_GREEN_BITS, mode.greenBits());
		glfwWindowHint(GLFW_BLUE_BITS, mode.blueBits());
		glfwWindowHint(GLFW_REFRESH_RATE, mode.refreshRate());

		// Create the window
		window = glfwCreateWindow(mode.width(), mode.height(), "DynamicWallpaper", NULL, NULL);
		if(window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");
		glfwSetWindowPos(window, 0, 0);
		glfwSetWindowSize(window, mode.width(), mode.height());

		Utils.makeWallpaper(window);

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) ->
		{
			if(key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		frameGrabber.set(new FFmpegFrameGrabber(videoFile));
		frameGrabber.get().setImageWidth(mode.width());
		frameGrabber.get().setImageHeight(mode.height());

		try
		{
			frameGrabber.get().start();
		}
		catch(FrameGrabber.Exception e)
		{
			e.printStackTrace();
		}

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void prepare()
	{
		try
		{
			Shader vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "/plane.vs");
			Shader fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "/video.fs");

			program = new ShaderProgram();
			program.attachShader(vertexShader);
			program.attachShader(fragmentShader);
			program.bindFragmentDataLocation(0, "fragColor");
			program.link();
			program.use();

			colorFilter = program.getUniformLocation("colorFilter");

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

				cube = new Mesh();
				cube.fill(vertices, textures, elements, program);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void loop()
	{
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		glEnable(GL_TEXTURE_2D);

		texture = new Texture();
		texture.bind();
		texture.setParameter(GL_TEXTURE_WRAP_S, GL_REPEAT);
		texture.setParameter(GL_TEXTURE_WRAP_T, GL_REPEAT);
		texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		prepare();

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while(!glfwWindowShouldClose(window))
		{
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			render();

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	void render()
	{
		texture.bind();

		try
		{
			Frame frame = frameGrabber.get().grabImage();
			if(frame == null)
			{
				frameGrabber.get().setFrameNumber(0);
				frame = frameGrabber.get().grabImage();
				if(frame == null)
					return;
			}
			ByteBuffer buf = (ByteBuffer) frame.image[0];
			texture.uploadData(GL_RGB8, frame.imageWidth, frame.imageHeight, GL_BGR, buf);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			return;
		}

		cube.bind();
		program.use();

		program.setUniform(colorFilter, colorMode.getColor());

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
	}

	public static void main(String[] args)
	{
		new DynamicWallpaper().run();
	}
}

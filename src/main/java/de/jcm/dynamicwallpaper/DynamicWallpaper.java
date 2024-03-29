package de.jcm.dynamicwallpaper;

import com.sun.jna.Platform;
import de.jcm.dynamicwallpaper.colormode.ActivityColorMode;
import de.jcm.dynamicwallpaper.colormode.ColorMode;
import de.jcm.dynamicwallpaper.colormode.ConstantColorMode;
import de.jcm.dynamicwallpaper.colormode.HueWaveColorMode;
import de.jcm.dynamicwallpaper.extra.DiscordOverlay;
import de.jcm.dynamicwallpaper.extra.ErrorScreen;
import de.jcm.dynamicwallpaper.extra.LoadingScreen;
import de.jcm.dynamicwallpaper.extra.Overlay;
import de.jcm.dynamicwallpaper.render.Mesh;
import de.jcm.dynamicwallpaper.render.Shader;
import de.jcm.dynamicwallpaper.render.ShaderProgram;
import de.jcm.dynamicwallpaper.render.Texture;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class DynamicWallpaper
{
	static
	{
		ColorMode.MODES.add(ConstantColorMode.class);
		ColorMode.MODES.add(HueWaveColorMode.class);
		ColorMode.MODES.add(ActivityColorMode.class);
	}
	static
	{
		Overlay.OVERLAYS.add(DiscordOverlay.class);
	}

	public static final Pattern linkPattern = Pattern.compile("(http(s|)*://|www\\.)\\S*");

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

	private ColorMode colorMode;
	private final HashMap<String, JSONObject> configurations = new HashMap<>();
	private String[] overlayCache;

	private final AtomicReference<FFmpegFrameGrabber> frameGrabber = new AtomicReference<>();
	private final AtomicReference<WallpaperState> state = new AtomicReference<>(WallpaperState.LOADING);

	private LoadingScreen loadingScreen;
	private ErrorScreen errorScreen;
	private Overlay[] overlays;

	private String video;
	// whether to store the video as a relative path (if possible)
	private boolean relative;

	private boolean hasCache;
	private boolean isFileInput;

	private double fps = 30.0;

	// timestamp (in microseconds) to seek to at beginning or video restart
	private long startTimestamp = 0;
	// timestamp (in microseconds) to trigger video restart when reached, -1 to disable
	private long endTimestamp = -1;

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public void run()
	{
		loadConfig();
		executor.scheduleAtFixedRate(()->{
			if(colorMode != null)
				colorMode.update();
		}, 1, 1, TimeUnit.SECONDS);

		if(Platform.isWindows())
		{
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(ClassNotFoundException | InstantiationException | IllegalAccessException |
			      UnsupportedLookAndFeelException e)
			{
				e.printStackTrace();
			}
		}

		ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/icon.png")));
		controlFrame = new ControlFrame(this);
		controlFrame.setIconImage(icon.getImage());

		SystemTray.FORCE_TRAY_TYPE = SystemTray.TrayType.AppIndicator;
		SystemTray systemTray = SystemTray.get();
		if (systemTray == null) {
			throw new RuntimeException("Unable to load SystemTray!");
		}
		systemTray.installShutdownHook();
		systemTray.getMenu().add(new MenuItem("Open GUI", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent actionEvent)
			{
				controlFrame.setVisible(true);
			}
		}));
		systemTray.setImage(Objects.requireNonNull(getClass().getResource("/images/icon.png")));

		loadingScreen = new LoadingScreen();
		errorScreen = new ErrorScreen();

		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		Thread loadVideo = new Thread(()->{
		try
		{
			if(new File("cache.mp4").exists())
				hasCache = true;

			startFrameGrabber();
		}
		catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}});
		loadVideo.start();

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
				configObject = new JSONObject(line.isEmpty()?"{}":line);
			}

			video = configObject.optString("video", "video.mp4");
			if(!linkPattern.matcher(video).matches())
			{
				if(!new File(video).isAbsolute())
				{
					relative = true;
				}
			}
			JSONObject videoConfig = configObject.optJSONObject("videoConfig");
			if(videoConfig != null)
			{
				startTimestamp = videoConfig.optLong("startTimestamp", 0);
				endTimestamp = videoConfig.optLong("endTimestamp", -1);
			}

			JSONArray configs = configObject.optJSONArray("modeConfigs");
			if(configs != null)
			{
				for(int i = 0; i < configs.length(); i++)
				{
					JSONObject obj = configs.getJSONObject(i);
					String name = obj.getString("mode");
					JSONObject cfg = obj.getJSONObject("config");

					configurations.put(name, cfg);
				}
			}

			String colorMode = configObject.optString("mode", ConstantColorMode.class.getName());
			try
			{
				this.colorMode = (ColorMode) Class.forName(colorMode).getConstructor().newInstance();
				this.colorMode.load(configurations.computeIfAbsent(this.colorMode.getClass().getName(), k->new JSONObject()));
			}
			catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}

			JSONArray overlays = configObject.optJSONArray("overlays");
			if(overlays != null)
			{
				this.overlays = overlays.toList().stream()
						.map(Object::toString)
						.map(s ->
						     {
							     try
							     {
								     return Class.forName(s);
							     }
							     catch(ClassNotFoundException e)
							     {
								     e.printStackTrace();
								     return null;
							     }
						     })
						.filter(Objects::nonNull)
						.filter(Overlay.class::isAssignableFrom)
						.map(c->{
							try
							{
								return (Overlay) c.getConstructor().newInstance();
							}
							catch(Exception e)
							{
								e.printStackTrace();
								return null;
							}})
						.filter(Objects::nonNull)
						.toArray(Overlay[]::new);
			}
			else
			{
				this.overlays = new Overlay[0];
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	void saveConfig()
	{
		JSONObject object = new JSONObject();

		if(linkPattern.matcher(video).matches())
		{
			object.put("video", video);
		}
		else
		{
			Path p1 = new File(video).toPath().toAbsolutePath().normalize();
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
		}

		JSONObject videoConfig = new JSONObject();
		videoConfig.put("startTimestamp", startTimestamp);
		videoConfig.put("endTimestamp", endTimestamp);
		if(hasCache)
			videoConfig.put("cachedVideo", video);
		object.put("videoConfig", videoConfig);

		object.put("mode", colorMode.getClass().getName());
		configurations.forEach((mode, cfg)-> {
			if(colorMode.getClass().getName().equals(mode))
				colorMode.save(configurations.compute(mode, (k,v)->new JSONObject()));

			JSONObject o2 = new JSONObject();
			o2.put("mode", mode);
			o2.put("config", cfg);
			object.append("modeConfigs", o2);
		});

		object.put("overlays", Arrays.asList(getOverlayCache()));

		File config = new File("config.json");
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(config)))
		{
			object.write(writer, 4, 0);
			writer.write('\n');
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

		try
		{
			if(frameGrabber.get() != null)
				frameGrabber.get().close();
		}
		catch(FrameGrabber.Exception e)
		{
			e.printStackTrace();
		}

		saveConfig();

		/*
		Shutdown all overlays; do this after saving the config to make sure
		we don't loose any unsaved changes in case we crash
		 */
		for(Overlay overlay : overlays)
		{
			overlay.shutdown();
		}

		File cache = new File("cache.mp4");
		if(!hasCache && cache.exists())
		{
			if(!cache.delete()) // delete cache if it's incomplete
			{
				System.out.println("Could not delete incomplete cache. Please do this manually.");
			}
		}

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

		glfwWindowHint(GLFW_SAMPLES, 16);

		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);

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

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Disable v-sync
		glfwSwapInterval(0);

		// Make the window visible
		glfwShowWindow(window);
	}

	public void startFrameGrabber() throws IOException, InterruptedException
	{
		if(frameGrabber.get()!=null)
		{
			fps = 30.0;
			state.set(WallpaperState.LOADING);
			frameGrabber.get().close();
		}

		try
		{
			FFmpegFrameGrabber grabber;

			Object o = openVideoStream();
			if(o instanceof InputStream)
			{
				grabber = new FFmpegFrameGrabber((InputStream) o, 0);
				grabber.setFormat("mp4");
				grabber.start(false);
				isFileInput = false;
			}
			else if(o instanceof File)
			{
				grabber = new FFmpegFrameGrabber((File) o);
				grabber.start();
				isFileInput = true;
			}
			else
			{
				throw new RuntimeException("cannot open video stream");
			}
			grabber.setTimestamp(startTimestamp);

			fps = grabber.getVideoFrameRate();

			frameGrabber.set(grabber);
			state.set(WallpaperState.PLAYING);
		}
		catch(Exception e)
		{
			state.set(WallpaperState.ERROR);

			throw e;
		}
	}

	public Object openVideoStream() throws IOException, InterruptedException
	{
		if(linkPattern.matcher(video).matches())
		{
			if(hasCache)
			{
				System.out.println("Using cached video for "+video);
				return new File("cache.mp4");
			}

			if(video.endsWith(".mp4"))
			{
				URL url = new URL(video);
				return createCacheStream(Utils.openURLStream(url));
			}
			else
			{
				Process process = new ProcessBuilder()
						.command("youtube-dl", "-f", "bestvideo[ext=mp4]/best[ext=mp4]", "-g", video)
						.redirectError(ProcessBuilder.Redirect.INHERIT)
						.start();
				int code;
				if((code = process.waitFor())==0)
				{
					String urlString = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
					URL url = new URL(urlString);
					return createCacheStream(Utils.openURLStream(url));
				}
				else
				{
					throw new IOException("youtube-dl returned exit code "+code);
				}
			}
		}
		else
		{
			return new File(video);
		}
	}

	private TeeInputStream createCacheStream(InputStream in) throws FileNotFoundException
	{
		return new TeeInputStream(in, new FileOutputStream("cache.mp4")
		{
			@Override
			public void close() throws IOException
			{
				super.close();
				hasCache = true;
				System.out.println("Finished caching video!");
			}
		}, true);
	}

	private void prepare()
	{
		try
		{
			Shader vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "/shaders/plane.vsh");
			Shader fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "/shaders/video.fsh");

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

			loadingScreen.prepare();
			errorScreen.prepare();

			for(Overlay overlay : overlays)
			{
				overlay.prepare();
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

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_BLEND);

		glEnable(GL_MULTISAMPLE);

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

		// used to freeze animations when paused
		double lastTime = glfwGetTime();
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while(!glfwWindowShouldClose(window))
		{
			long frameStart = System.currentTimeMillis();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			WallpaperState state = this.state.get();
			if(state == WallpaperState.PLAYING)
			{
				updateTexture();
				lastTime = glfwGetTime();
			}
			render();
			for(Overlay overlay : overlays)
			{
				overlay.render(lastTime);
			}
			if(state == WallpaperState.LOADING)
			{
				loadingScreen.render();
			}
			if(state == WallpaperState.ERROR)
			{
				errorScreen.render();
			}

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();

			long frameEnd = System.currentTimeMillis();
			long frameTime = frameEnd - frameStart;
			long sleepTime = (long) (1000 / fps - frameTime);
			if(sleepTime < 0)
			{
				System.out.printf("Cannot sync at %.02f FPS. Running %d ms behind.\n",
				                  fps, -sleepTime);
			}
			else
			{
				try
				{
					Thread.sleep(sleepTime);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	void updateTexture()
	{
		texture.bind();

		try
		{
			if(endTimestamp > 0 && frameGrabber.get().getTimestamp() >= endTimestamp)
			{
				frameGrabber.get().setTimestamp(startTimestamp);
			}

			Frame frame = frameGrabber.get().grabImage();
			if(frame == null)
			{
				if(isFileInput)
				{
					frameGrabber.get().setTimestamp(startTimestamp);
				}
				else
				{
					startFrameGrabber();
				}

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
		}
	}

	void render()
	{
		texture.bind();

		cube.bind();
		program.use();

		program.setUniform(colorFilter, colorMode.getColor());

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
	}

	public ColorMode getColorMode()
	{
		return colorMode;
	}

	public String getVideo()
	{
		return video;
	}

	public long getVideoStartTimestamp()
	{
		return startTimestamp;
	}

	public long getVideoEndTimestamp()
	{
		return endTimestamp;
	}

	public boolean isRelativeVideoPath()
	{
		return relative;
	}

	public ColorMode computeColorMode(String className)
	{
		if(colorMode.getClass().getName().equals(className))
		{
			return colorMode;
		}
		else
		{
			try
			{
				ColorMode colorMode = (ColorMode) Class.forName(className).getConstructor().newInstance();
				colorMode.load(configurations.computeIfAbsent(className, k->new JSONObject()));
				return colorMode;
			}
			catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public void setVideo(String video)
	{
		this.video = video;
		hasCache = false;
	}

	public void setRelativeVideoPath(boolean relative)
	{
		this.relative = relative;
	}

	public void setVideoStartTimestamp(long startTimestamp)
	{
		this.startTimestamp = startTimestamp;
		if(this.startTimestamp < 0)
			this.startTimestamp = 0;
	}

	public void setVideoEndTimestamp(long endTimestamp)
	{
		if(endTimestamp > this.endTimestamp)
			hasCache = false;   // We need to download the part of the video we skipped before

		this.endTimestamp = endTimestamp;
		if(this.endTimestamp <= 0)
			this.endTimestamp = -1;
	}

	public void setColorMode(ColorMode colorMode)
	{
		this.colorMode.save(configurations.compute(this.colorMode.getClass().getName(), (k,v)->new JSONObject()));
		this.colorMode = colorMode;
		this.colorMode.save(configurations.compute(this.colorMode.getClass().getName(), (k,v)->new JSONObject()));
	}

	public void setPaused(boolean paused)
	{
		if(state.get() == WallpaperState.LOADING)
			return;

		state.set(paused?WallpaperState.PAUSED:WallpaperState.PLAYING);
	}

	public boolean isPaused()
	{
		return state.get() == WallpaperState.PAUSED;
	}

	public static void main(String[] args)
	{
		new DynamicWallpaper().run();
	}

	public void setOverlayCache(String[] overlayCache)
	{
		this.overlayCache = overlayCache;
	}

	public String[] getOverlayCache()
	{
		if(overlayCache != null)
			return overlayCache;
		else
			return Stream.of(overlays).map(Overlay::getClass).map(Class::getName).toArray(String[]::new);
	}
}

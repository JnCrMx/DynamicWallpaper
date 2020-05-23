package de.jcm.dynamicwallpaper.extra;

import de.jcm.discordgamesdk.*;
import de.jcm.discordgamesdk.image.ImageDimensions;
import de.jcm.discordgamesdk.image.ImageHandle;
import de.jcm.discordgamesdk.image.ImageType;
import de.jcm.discordgamesdk.user.OnlineStatus;
import de.jcm.discordgamesdk.user.Relationship;
import de.jcm.dynamicwallpaper.Utils;
import de.jcm.dynamicwallpaper.render.Mesh;
import de.jcm.dynamicwallpaper.render.Shader;
import de.jcm.dynamicwallpaper.render.ShaderProgram;
import de.jcm.dynamicwallpaper.render.Texture;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

public class DiscordOverlay extends DiscordEventAdapter implements Overlay
{
	private static final int CIRCLE_VERTEX_COUNT = 36;
	private static final float ROTATION_SPEED = 0.5f;

	private Core core;

	private Mesh avatar;
	private ShaderProgram program;

	private int modelMatrixUniform;
	private int outlineColorUniform;

	public static File downloadDiscordLibrary() throws IOException
	{
		// Find out which name Discord's library has (.dll for Windows, .so for Linux)
		String name = "discord_game_sdk";
		String suffix;
		if(System.getProperty("os.name").toLowerCase().contains("windows"))
		{
			suffix = ".dll";
		}
		else
		{
			suffix = ".so";
		}

		// Path of Discord's library inside the ZIP
		String zipPath = "lib/x86_64/"+name+suffix;

		// Open the URL as a ZipInputStream
		URL downloadUrl = new URL("https://dl-game-sdk.discordapp.net/latest/discord_game_sdk.zip");
		ZipInputStream zin = new ZipInputStream(Utils.openURLStream(downloadUrl));

		// Search for the right file inside the ZIP
		ZipEntry entry;
		while((entry = zin.getNextEntry())!=null)
		{
			if(entry.getName().equals(zipPath))
			{
				// Create a new temporary directory
				// We need to do this, because we may not change the filename on Windows
				File tempDir = new File(System.getProperty("java.io.tmpdir"), "java-"+name+System.nanoTime());
				if(!tempDir.mkdir())
					throw new IOException("Cannot create temporary directory");
				tempDir.deleteOnExit();

				// Create a temporary file inside our directory (with a "normal" name)
				File temp = new File(tempDir, name+suffix);
				temp.deleteOnExit();

				// Open an OutputStream to this file...
				FileOutputStream fout = new FileOutputStream(temp);
				// ...and copy the file from the ZIP to it
				IOUtils.copy(zin, fout);    // Java 8 replacement for InputStream.transferTo(OutputStream)
				fout.close();

				// We are done, so close the input stream
				zin.close();

				// Return our temporary file
				return temp;
			}
			// next entry
			zin.closeEntry();
		}
		zin.close();
		// We couldn't find the library inside the ZIP
		return null;
	}

	public static Vector4f getStatusColor(OnlineStatus status)
	{
		switch(status)
		{
			case ONLINE:
				return new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
			case IDLE:
				return new Vector4f(1.0f, 1.0f, 0.0f, 1.0f);
			case DO_NO_DISTURB:
				return new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
			default:
				return new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
		}
	}

	public DiscordOverlay()
	{

	}

	private final Queue<Triple<Long, ImageDimensions, byte[]>> images = new ArrayDeque<>();
	private final Map<Long, Texture> textures = new HashMap<>();
	private final Map<Long, OnlineStatus> onlineUsers = new HashMap<>();
	private long currentUser;

	@Override
	public void onRelationshipRefresh()
	{
		core.relationshipManager()
				.filter(RelationshipManager.FRIEND_FILTER
						        .and(RelationshipManager.OFFLINE_FILTER.negate()));

		onlineUsers.clear();

		List<Relationship> relationships = core.relationshipManager().asList();
		for(Relationship relationship : relationships)
		{
			long uid = relationship.getUser().getUserId();
			ImageHandle handle = new ImageHandle(ImageType.USER, uid, 256);
			if(textures.containsKey(uid))
			{
				if(!onlineUsers.containsKey(uid))
					onlineUsers.put(uid, relationship.getPresence().getStatus());
			}
			else
			{
				core.imageManager().fetch(handle, false, (result, h) ->
				{
					if(result == Result.OK)
					{
						ImageDimensions dim = core.imageManager().getDimensions(handle);
						byte[] data = core.imageManager().getData(handle, dim);
						images.add(new ImmutableTriple<>(uid, dim, data));

						if(!onlineUsers.containsKey(uid))
							onlineUsers.put(uid, relationship.getPresence().getStatus());
					}
					else
					{
						System.out.printf("Cannot fetch avatar of %s#%s!\n",
								                   relationship.getUser().getUsername(),
								                   relationship.getUser().getDiscriminator());
					}
				});
			}
		}
	}

	@Override
	public void onRelationshipUpdate(Relationship relationship)
	{
		onRelationshipRefresh();
	}

	@Override
	public void onCurrentUserUpdate()
	{
		long uid = core.userManager().getCurrentUser().getUserId();
		ImageHandle handle = new ImageHandle(ImageType.USER, uid, 256);
		core.imageManager().fetch(handle, false, (result, h) ->
		{
			if(result == Result.OK)
			{
				ImageDimensions dim = core.imageManager().getDimensions(handle);
				byte[] data = core.imageManager().getData(handle, dim);
				images.add(new ImmutableTriple<>(uid, dim, data));

				currentUser = uid;
			}
		});
	}

	public void prepare() throws IOException
	{
		// Init Discord API
		try
		{
			Core.init(Objects.requireNonNull(downloadDiscordLibrary()));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		CreateParams params = new CreateParams();
		params.registerEventHandler(this);
		core = new Core(params);

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(core::runCallbacks, 0,
				                     16, TimeUnit.MILLISECONDS);

		// Init OpenGL stuff
		Shader vertexShader = Shader.loadShader(GL_VERTEX_SHADER, "/shaders/loading.vsh");
		Shader fragmentShader = Shader.loadShader(GL_FRAGMENT_SHADER, "/shaders/avatar.fsh");

		program = new ShaderProgram();
		program.attachShader(vertexShader);
		program.attachShader(fragmentShader);
		program.bindFragmentDataLocation(0, "fragColor");
		program.link();
		program.use();

		modelMatrixUniform = program.getUniformLocation("modelMatrix");
		outlineColorUniform = program.getUniformLocation("outlineColor");
		int projectionMatrixUniform = program.getUniformLocation("projectionMatrix");

		// too lazy to do something weird with perspective and stuff, so just use scale
		Matrix4f projectionMatirx = new Matrix4f().scale(1f / 16, 1f / 9f, 1f);
		program.setUniform(projectionMatrixUniform, projectionMatirx);

		try(MemoryStack stack = MemoryStack.stackPush())
		{
			FloatBuffer vertices = stack.mallocFloat(CIRCLE_VERTEX_COUNT * 3);
			FloatBuffer textures = stack.mallocFloat(CIRCLE_VERTEX_COUNT * 2);
			IntBuffer elements = stack.mallocInt(CIRCLE_VERTEX_COUNT * 3);

			for(int i=0; i<CIRCLE_VERTEX_COUNT; i++)
			{
				float x = (float) Math.sin(i*(2*Math.PI/CIRCLE_VERTEX_COUNT));
				float y = (float) Math.cos(i*(2*Math.PI/CIRCLE_VERTEX_COUNT));

				vertices.put(x).put(y).put(0.0f);
				textures.put(x*0.5f+0.5f).put(-y*0.5f+0.5f);
				elements.put(0).put(i).put((i+1)%CIRCLE_VERTEX_COUNT);
			}

			vertices.flip();
			textures.flip();
			elements.flip();

			avatar = new Mesh();
			avatar.fill(vertices, textures, elements, program);
		}
	}

	public void render(double time)
	{
		Triple<Long, ImageDimensions, byte[]> image = images.poll();
		if(image != null)
		{
			Texture texture = new Texture();
			texture.bind();
			texture.setParameter(GL_TEXTURE_WRAP_S, GL_REPEAT);
			texture.setParameter(GL_TEXTURE_WRAP_T, GL_REPEAT);
			texture.setParameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			texture.setParameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			byte[] data = image.getRight();
			ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
			buffer.put(data);
			buffer.flip();

			ImageDimensions dim = image.getMiddle();

			// data is RGBA, but we want to truncate the alpha channel and render it RGB
			texture.uploadData(GL_RGB8, dim.getWidth(), dim.getHeight(), GL_RGBA, buffer);
			textures.put(image.getLeft(), texture);
		}

		avatar.bind();
		program.use();

		if(currentUser != 0 && textures.containsKey(currentUser))
		{
			textures.get(currentUser).bind();

			Matrix4f matrix = new Matrix4f().identity();
			program.setUniform(modelMatrixUniform, matrix);
			program.setUniform(outlineColorUniform, new Vector4f(0.0f, 0.0f, 0.0f, 0.0f));

			glDrawElements(GL_TRIANGLES, CIRCLE_VERTEX_COUNT*3, GL_UNSIGNED_INT, 0);
		}

		Iterator<Map.Entry<Long, OnlineStatus>> it = onlineUsers.entrySet().iterator();
		for(int i=0; i<onlineUsers.size(); i++)
		{
			Map.Entry<Long, OnlineStatus> entry = it.next();
			long uid = entry.getKey();
			if(textures.containsKey(uid))
			{
				textures.get(uid).bind();

				float angle = (float) ((2*Math.PI*i)/onlineUsers.size() -
						time*ROTATION_SPEED);

				Matrix4f matrix = new Matrix4f()
						.rotation(angle, 0, 0, 1)
						.translate(5, 0, 0)
						.rotate(angle, 0, 0, -1);
				program.setUniform(modelMatrixUniform, matrix);

				program.setUniform(outlineColorUniform, getStatusColor(entry.getValue()));

				glDrawElements(GL_TRIANGLES, CIRCLE_VERTEX_COUNT*3, GL_UNSIGNED_INT, 0);
			}
		}
	}

	@Override
	public String getName()
	{
		return "Discord Friends Overlay";
	}
}

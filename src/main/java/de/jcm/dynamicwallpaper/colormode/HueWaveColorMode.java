package de.jcm.dynamicwallpaper.colormode;

import org.joml.Vector3f;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class HueWaveColorMode extends ColorMode
{
	private double slowness;

	@Override
	public Vector3f getColor()
	{
		double hue = (glfwGetTime()/slowness)%1.0;
		Color c = new Color(Color.HSBtoRGB((float) hue, 1.0f, 1.0f));

		return new Vector3f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f);
	}

	@Override
	public void load(JSONObject object)
	{
		this.slowness = object.optDouble("slowness", 10.0);
	}

	@Override
	public void save(JSONObject object)
	{
		object.put("slowness", slowness);
	}

	@Override
	public ColorModeConfigurationPanel createConfigurationPanel()
	{
		return new ColorModeConfigurationPanel()
		{
			private final JSpinner slowness = new JSpinner(
					new SpinnerNumberModel(10.0, 0.1, 100.0, 0.1));
			{
				add(new JLabel("Slowness: "));
				add(slowness);
			}

			@Override
			public void load()
			{
				slowness.setValue(HueWaveColorMode.this.slowness);
			}

			@Override
			public void apply()
			{
				HueWaveColorMode.this.slowness = (double) slowness.getValue();
			}
		};
	}

	@Override
	public String getName()
	{
		return "Hue Wave";
	}
}

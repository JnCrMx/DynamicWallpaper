package de.jcm.dynamicwallpaper.colormode;

import org.joml.Vector3f;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

public class ConstantColorMode extends ColorMode
{
	private Color color;

	@Override
	public Vector3f getColor()
	{
		return new Vector3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
	}

	@Override
	public void load(JSONObject object)
	{
		color = new Color(object.optInt("color", Color.WHITE.getRGB()));
	}

	@Override
	public void save(JSONObject object)
	{
		object.put("color", color.getRGB());
	}

	@Override
	public ColorModeConfigurationPanel createConfigurationPanel()
	{
		return new ColorModeConfigurationPanel()
		{
			private final JColorChooser colorChooser = new JColorChooser();
			{
				add(colorChooser);
			}

			@Override
			public void load()
			{
				colorChooser.setColor(color);
			}

			@Override
			public void apply()
			{
				color = colorChooser.getColor();
			}
		};
	}
}

package de.jcm.dynamicwallpaper.colormode;

import org.joml.Vector3f;
import org.json.JSONObject;

public abstract class ColorMode
{
	public abstract Vector3f getColor();
	public void update() {}

	public abstract void load(JSONObject object);
	public abstract void save(JSONObject object);

	public abstract ColorModeConfigurationPanel createConfigurationPanel();
}

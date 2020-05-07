package de.jcm.dynamicwallpaper.colormode;

import org.joml.Vector3f;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class ColorMode
{
	public static final List<Class<? extends ColorMode>> MODES = new ArrayList<>();

	public abstract Vector3f getColor();
	public void update() {}

	public abstract void load(JSONObject object);
	public abstract void save(JSONObject object);

	public abstract ColorModeConfigurationPanel createConfigurationPanel();
	public abstract String getName();
}

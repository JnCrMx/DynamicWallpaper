package de.jcm.dynamicwallpaper.extra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface Overlay
{
	List<Class<? extends Overlay>> OVERLAYS = new ArrayList<>();

	void prepare() throws IOException;
	void render(double time);

	String getName();
}

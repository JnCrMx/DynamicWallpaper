package de.jcm.dynamicwallpaper.colormode;

import org.json.JSONObject;

import java.awt.*;
import java.util.function.Predicate;

public class ActivityColorEntry implements Predicate<String>, Cloneable
{
	enum FilterType
	{
		PROCESS_NAME,
		WINDOW_TITLE
	}

	enum FilterPolicy
	{
		EQUALS,
		STARTS_WITH,
		ENDS_WITH,
		CONTAINS
	}

	public FilterType type;
	public FilterPolicy policy;
	public String keyword;
	public Color color;

	public void write(JSONObject object)
	{
		object.put("type", type.name());
		object.put("policy", policy.name());
		object.put("keyword", keyword);
		object.put("color", color.getRGB());
	}

	public void read(JSONObject object)
	{
		type = FilterType.valueOf(object.getString("type"));
		policy = FilterPolicy.valueOf(object.getString("policy"));
		keyword = object.getString("keyword");
		color = new Color(object.getInt("color"));
	}

	@Override
	public boolean test(String s)
	{
		switch(policy)
		{
			case EQUALS:
				return s.equals(keyword);
			case STARTS_WITH:
				return s.startsWith(keyword);
			case ENDS_WITH:
				return s.endsWith(keyword);
			case CONTAINS:
				return s.contains(keyword);
		}
		return false;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}

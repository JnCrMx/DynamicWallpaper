package de.jcm.dynamicwallpaper.colormode;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.*;

public class ActivityColorMode extends ColorMode
{
	private final LinkedList<ActivityColorEntry> activityColors = new LinkedList<>();
	private float interpolationStepSize = 2.0f;

	private float currentRed = 255, currentGreen = 255, currentBlue = 255;
	private int targetRed, targetGreen, targetBlue;

	@Override
	public Vector3f getColor()
	{
		interpolate();

		return new Vector3f(currentRed/255f, currentGreen/255f, currentBlue/255f);
	}

	@Override
	public void update()
	{
		targetRed = targetGreen = targetBlue = 255;

		WinNT.HANDLE handle = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, null);
		Tlhelp32.PROCESSENTRY32 process = new Tlhelp32.PROCESSENTRY32();

		Set<String> processNames = new HashSet<>();
		if(Kernel32.INSTANCE.Process32First(handle, process))
		{
			do
			{
				String processName = new String(process.szExeFile).trim();
				processNames.add(processName);
			}
			while(Kernel32.INSTANCE.Process32Next(handle, process));
		}
		process.clear();
		Kernel32.INSTANCE.CloseHandle(handle);

		Set<String> windowTitles = new HashSet<>();
		User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC()
		{
			@Override
			public boolean callback(WinDef.HWND hWnd, Pointer data)
			{
				if(!User32.INSTANCE.IsWindowVisible(hWnd))
					return true;

				char[] buffer = new char[256];
				int len = User32.INSTANCE.GetWindowText(hWnd, buffer, 256);

				String string = new String(buffer, 0, len);
				windowTitles.add(string);

				return true;
			}
		}, null);

		for(ActivityColorEntry entry : activityColors)
		{
			if(entry.type == ActivityColorEntry.FilterType.PROCESS_NAME)
			{
				if(processNames.stream().anyMatch(entry))
				{
					Color color = entry.color;
					targetRed = color.getRed();
					targetGreen = color.getGreen();
					targetBlue = color.getBlue();
					break;
				}
			}
			if(entry.type == ActivityColorEntry.FilterType.WINDOW_TITLE)
			{
				if(windowTitles.stream().anyMatch(entry))
				{
					Color color = entry.color;
					targetRed = color.getRed();
					targetGreen = color.getGreen();
					targetBlue = color.getBlue();
					break;
				}
			}
		}
	}

	private void interpolate()
	{
		if(Math.abs(currentRed - targetRed) < interpolationStepSize)
			currentRed = targetRed;
		else if(currentRed > targetRed)
			currentRed-= interpolationStepSize;
		else if(currentRed < targetRed)
			currentRed+= interpolationStepSize;

		if(Math.abs(currentGreen - targetGreen) < interpolationStepSize)
			currentGreen = targetGreen;
		else if(currentGreen > targetGreen)
			currentGreen-= interpolationStepSize;
		else if(currentGreen < targetGreen)
			currentGreen+= interpolationStepSize;

		if(Math.abs(currentBlue - targetBlue) < interpolationStepSize)
			currentBlue = targetBlue;
		else if(currentBlue > targetBlue)
			currentBlue-= interpolationStepSize;
		else if(currentBlue < targetBlue)
			currentBlue+= interpolationStepSize;
	}

	@Override
	public void load(JSONObject object)
	{
		JSONArray colors = object.optJSONArray("colors");
		if(colors != null)
		{
			for(int i=0; i<colors.length(); i++)
			{
				JSONObject object1 = colors.getJSONObject(i);
				ActivityColorEntry entry = new ActivityColorEntry();
				entry.read(object1);

				activityColors.addLast(entry);
			}
		}

		interpolationStepSize = object.optFloat("interpolation_step_size", 2.0f);
	}

	@Override
	public void save(JSONObject object)
	{
		for(ActivityColorEntry entry : activityColors)
		{
			JSONObject object1 = new JSONObject();
			entry.write(object1);
			object.append("colors", object1);
		}
		object.put("interpolation_step_size", interpolationStepSize);
	}

	@Override
	public ColorModeConfigurationPanel createConfigurationPanel()
	{
		return new ActivityConfigurationPanel();
	}

	public class ActivityConfigurationPanel extends ColorModeConfigurationPanel
	{
		public LinkedList<ActivityColorEntry> list;

		private final JButton editButton;
		private final JButton removeButton;
		private final JButton upButton;
		private final JButton downButton;
		private final JTable table = new JTable();

		private final JSpinner interStepSpinner = new JSpinner(
				new SpinnerNumberModel(1.0, 0.1, 255.0, 1.0));

		public ActivityConfigurationPanel()
		{
			setLayout(new BorderLayout(5, 5));

			JPanel stepPanel = new JPanel();
			{
				JLabel label = new JLabel("Interpolation step size: ");
				stepPanel.add(label);
				stepPanel.setLayout(new BoxLayout(stepPanel, BoxLayout.LINE_AXIS));
				stepPanel.add(interStepSpinner);
			}
			add(stepPanel, BorderLayout.NORTH);

			JPanel programPanel = new JPanel(new BorderLayout());
			programPanel.setBorder(new TitledBorder("Program Colors:"));
			programPanel.add(table, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new GridLayout(5, 1));
			{
				JButton addButton = new JButton("+");
				addButton.setToolTipText("Add a new entry");
				addButton.addActionListener(e->{
					ActivityColorDialog dialog = new ActivityColorDialog(this, null);
					dialog.setVisible(true);
				});
				buttonPanel.add(addButton);

				upButton = new JButton("\u2191");
				upButton.setToolTipText("Increase priority of selected entry");
				upButton.setEnabled(false);
				upButton.addActionListener(e->{
					int index = table.getSelectedRow()-1;
					ActivityColorEntry entry = list.remove(index);
					list.add(index-1, entry);

					table.setRowSelectionInterval(table.getSelectedRow()-1,
					                              table.getSelectedRow()-1);

					revalidate();
					repaint();
				});
				buttonPanel.add(upButton);

				editButton = new JButton("\u270E");
				editButton.setToolTipText("Edit selected entry");
				editButton.setEnabled(false);
				editButton.addActionListener(e->{
					ActivityColorEntry entry = list.get(table.getSelectedRow()-1);
					ActivityColorDialog dialog = new ActivityColorDialog(this, entry);
					dialog.setVisible(true);
				});
				buttonPanel.add(editButton);

				downButton = new JButton("\u2193");
				downButton.setToolTipText("Decrease priority of selected entry");
				downButton.setEnabled(false);
				downButton.addActionListener(e->{
					int index = table.getSelectedRow()-1;
					ActivityColorEntry entry = list.remove(index);
					list.add(index+1, entry);

					table.setRowSelectionInterval(table.getSelectedRow()+1,
					                              table.getSelectedRow()+1);

					revalidate();
					repaint();
				});
				buttonPanel.add(downButton);

				removeButton = new JButton("\u2012");
				removeButton.setToolTipText("Remove selected entry");
				removeButton.setEnabled(false);
				removeButton.addActionListener(e->{
					list.remove(table.getSelectedRow()-1);

					revalidate();
					repaint();
				});
				buttonPanel.add(removeButton);
			}
			programPanel.add(buttonPanel, BorderLayout.EAST);

			add(programPanel, BorderLayout.CENTER);
		}

		@Override
		public void load()
		{
			TableModel model = new AbstractTableModel()
			{
				@Override
				public int getRowCount()
				{
					return list.size()+1;
				}

				@Override
				public int getColumnCount()
				{
					return 2;
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex)
				{
					if(rowIndex==0)
					{
						return columnIndex==0?"Program":"Color";
					}

					if(columnIndex == 0)
					{
						return list.get(rowIndex-1).keyword;
					}
					else
					{
						return list.get(rowIndex-1).color;
					}
				}

				@Override
				public Class<?> getColumnClass(int columnIndex)
				{
					return columnIndex==0?String.class: Color.class;
				}
			};
			table.setModel(model);

			table.setDefaultRenderer(Color.class, new TableCellRenderer()
			{
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
				{
					if(value instanceof String)
					{
						return new JLabel((String) value);
					}
					else if(value instanceof Color)
					{
						Color color = (Color) value;

						JLabel label = new JLabel(String.format("0x%06X", color.getRGB()));
						label.setOpaque(true);
						label.setBackground(color);

						return label;
					}
					return null;
				}
			});
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					editButton.setEnabled(table.getSelectedRow()>0);
					removeButton.setEnabled(table.getSelectedRow()>0);

					upButton.setEnabled(table.getSelectedRow()>1);
					downButton.setEnabled(table.getSelectedRow()>0 &&
							table.getSelectedRow() < list.size());
				}
			});

			interStepSpinner.setValue(interpolationStepSize);

			list = new LinkedList<>();
			activityColors.forEach(e-> {
				try
				{
					list.addLast((ActivityColorEntry) e.clone());
				}
				catch(CloneNotSupportedException ex)
				{
					ex.printStackTrace();
				}
			});
		}

		@Override
		public void apply()
		{
			activityColors.clear();

			list.forEach(e-> {
				try
				{
					activityColors.addLast((ActivityColorEntry) e.clone());
				}
				catch(CloneNotSupportedException ex)
				{
					ex.printStackTrace();
				}
			});

			// toString() and parseFloat() to skip all the annoying and weird casting
			interpolationStepSize = Float.parseFloat(interStepSpinner.getValue().toString());
		}
	}
}

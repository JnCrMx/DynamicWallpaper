package de.jcm.dynamicwallpaper;

import de.jcm.dynamicwallpaper.colormode.*;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Enumeration;

public class ControlFrame extends JFrame
{
	private final DynamicWallpaper wallpaper;
	private final JTextField filePathField;

	private final JPanel colorModePanel;
	private final ButtonGroup typeGroup;
	private final JCheckBox relativePathCheckBox;
	private final JSpinner startTime;
	private final JSpinner endTime;

	private ColorMode colorMode;
	private ColorModeConfigurationPanel modeConfigurationPanel;

	public ControlFrame(DynamicWallpaper wallpaper)
	{
		this.wallpaper = wallpaper;
		this.colorMode = wallpaper.colorMode;

		setTitle(BuildConfig.NAME+" version "+BuildConfig.VERSION);

		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		{
			JPanel filePanel = new JPanel();
			filePanel.setBorder(new TitledBorder("Video File"));
			filePanel.setLayout(new BorderLayout());
			{
				filePathField = new JTextField(25);
				filePathField.setText(wallpaper.video);
				filePanel.add(filePathField, BorderLayout.CENTER);

				JButton browseButton = new JButton("Browse");
				browseButton.setToolTipText("Open dialog to select file");
				browseButton.addActionListener(e->{
					JFileChooser chooser = new JFileChooser();
					chooser.setSelectedFile(new File(wallpaper.video));
					FileFilter mp4Filter = new FileFilter()
					{
						@Override
						public boolean accept(File f)
						{
							return f.isDirectory() || f.getName().endsWith(".mp4");
						}

						@Override
						public String getDescription()
						{
							return "MP4-Video (*.mp4)";
						}
					};
					chooser.addChoosableFileFilter(mp4Filter);
					chooser.setFileFilter(mp4Filter);

					int result = chooser.showOpenDialog(null);
					if(result == JFileChooser.APPROVE_OPTION)
					{
						File file = chooser.getSelectedFile();
						filePathField.setText(file.getAbsolutePath());
					}
				});
				filePanel.add(browseButton, BorderLayout.EAST);

				JPanel moreOptions = new JPanel();
				moreOptions.setLayout(new BoxLayout(moreOptions, BoxLayout.LINE_AXIS));
				{
					relativePathCheckBox = new JCheckBox("relative path");
					relativePathCheckBox.setToolTipText("Store as relative path in config");
					relativePathCheckBox.setSelected(wallpaper.relative);
					moreOptions.add(relativePathCheckBox);

					moreOptions.add(Box.createHorizontalStrut(5));
					moreOptions.add(Box.createHorizontalGlue());
					moreOptions.add(Box.createHorizontalStrut(5));

					JLabel startTimeLabel = new JLabel("start time");
					moreOptions.add(startTimeLabel);

					startTime = new JSpinner(
							new SpinnerNumberModel(wallpaper.startTimestamp/1000000.0,
							                       0.0, 10000.0, 1.0));
					moreOptions.add(startTime);

					JLabel startTimeUnitLabel = new JLabel(" s");
					moreOptions.add(startTimeUnitLabel);

					moreOptions.add(Box.createHorizontalStrut(5));
					moreOptions.add(Box.createHorizontalGlue());
					moreOptions.add(Box.createHorizontalStrut(5));

					JLabel endTimeLabel = new JLabel("end time");
					moreOptions.add(endTimeLabel);

					endTime = new JSpinner(
							new SpinnerNumberModel(wallpaper.endTimestamp<=0?
									                       -1:
									                       wallpaper.endTimestamp/1000000.0,
							                       -1.0, 10000.0, 1.0));
					moreOptions.add(endTime);

					JLabel endTimeUnitLabel = new JLabel(" s");
					moreOptions.add(endTimeUnitLabel);
				}
				filePanel.add(moreOptions, BorderLayout.SOUTH);
			}
			contentPane.add(filePanel, BorderLayout.NORTH);

			JPanel colorConfigurationPanel = new JPanel(new BorderLayout(5, 5));
			colorConfigurationPanel.setBorder(new TitledBorder("Color Configuration"));
			{
				JPanel typePanel = new JPanel();
				typePanel.setBorder(new TitledBorder("Type"));
				{
					typeGroup = new ButtonGroup();

					JRadioButton constantRadio = new JRadioButton("Constant Color");
					constantRadio.getModel().setActionCommand(ConstantColorMode.class.getName());
					constantRadio.addActionListener(e->selectColorType());
					typePanel.add(constantRadio);
					typeGroup.add(constantRadio);

					JRadioButton hueWaveRadio = new JRadioButton("Hue Wave");
					hueWaveRadio.getModel().setActionCommand(HueWaveColorMode.class.getName());
					hueWaveRadio.addActionListener(e->selectColorType());
					typePanel.add(hueWaveRadio);
					typeGroup.add(hueWaveRadio);

					JRadioButton activityRadio = new JRadioButton("Activity Detection");
					activityRadio.getModel().setActionCommand(ActivityColorMode.class.getName());
					activityRadio.addActionListener(e->selectColorType());
					typePanel.add(activityRadio);
					typeGroup.add(activityRadio);

					Enumeration<AbstractButton> models = typeGroup.getElements();
					while(models.hasMoreElements())
					{
						AbstractButton button = models.nextElement();
						if(button.getModel().getActionCommand().equals(colorMode.getClass().getName()))
							button.setSelected(true);
					}
				}
				colorConfigurationPanel.add(typePanel, BorderLayout.NORTH);

				colorModePanel = new JPanel(new BorderLayout());
				{
					modeConfigurationPanel = colorMode.createConfigurationPanel();
					modeConfigurationPanel.load();
					colorModePanel.add(modeConfigurationPanel, BorderLayout.CENTER);
				}
				colorConfigurationPanel.add(colorModePanel, BorderLayout.CENTER);
			}
			contentPane.add(colorConfigurationPanel, BorderLayout.CENTER);

			JPanel controlButtonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
			controlButtonPanel.setBorder(new TitledBorder("Wallpaper Control"));
			{
				JButton apply = new JButton("Apply");
				apply.addActionListener(e-> apply());
				controlButtonPanel.add(apply);

				JButton exit = new JButton("Exit");
				exit.addActionListener(e->{
					int option = JOptionPane.showConfirmDialog(this,
					                                           "Save changes before exit?",
					                                           "Confirm Exit",
					                                           JOptionPane.YES_NO_CANCEL_OPTION);
					if(option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION)
					{
						return;
					}
					if(option == JOptionPane.YES_OPTION)
					{
						save(false);
					}
					GLFW.glfwSetWindowShouldClose(wallpaper.getWindow(), true);
				});
				controlButtonPanel.add(exit);
			}
			contentPane.add(controlButtonPanel, BorderLayout.SOUTH);
		}

		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(null);
	}

	private void selectColorType()
	{
		colorModePanel.removeAll();

		String className = typeGroup.getSelection().getActionCommand();
		if(wallpaper.colorMode.getClass().getName().equals(className))
		{
			colorMode = wallpaper.colorMode;
		}
		else
		{
			try
			{
				colorMode = (ColorMode) Class.forName(className).getConstructor().newInstance();
				colorMode.load(new JSONObject());
			}
			catch(InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		modeConfigurationPanel = colorMode.createConfigurationPanel();
		modeConfigurationPanel.load();
		colorModePanel.add(modeConfigurationPanel, BorderLayout.CENTER);

		colorModePanel.revalidate();
		colorModePanel.repaint();

		pack();
	}

	public void apply()
	{
		save(true);
	}

	public void save(boolean apply)
	{
		if(!filePathField.getText().isEmpty())
		{
			String video = filePathField.getText();
			if(DynamicWallpaper.linkPattern.matcher(video).matches())
			{
				if(!video.equals(wallpaper.video))
				{
					wallpaper.video = video;
					if(apply)
					{
						try
						{
							wallpaper.startFrameGrabber();
						}
						catch(IOException | InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			else
			{
				File newFile = new File(video);
				try
				{
					try
					{
						if(!Files.isSameFile(newFile.toPath(), new File(wallpaper.video).toPath()))
						{
							wallpaper.video = newFile.getPath();
							if(apply)
							{
								wallpaper.startFrameGrabber();
							}
						}
					}
					catch(InvalidPathException ignored)
					{
						wallpaper.video = newFile.getPath();
						if(apply)
						{
							wallpaper.startFrameGrabber();
						}
					}
				}
				catch(IOException | InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		wallpaper.relative = relativePathCheckBox.isSelected();

		wallpaper.startTimestamp = (long) ((double)startTime.getValue()*1000000);
		wallpaper.endTimestamp = (long) ((double)endTime.getValue()*1000000);
		if(wallpaper.endTimestamp <= 0)
			wallpaper.endTimestamp = -1;

		wallpaper.colorMode = colorMode;
		modeConfigurationPanel.apply();

		// if we are going to shutdown (-> confirm dialog), we don't need to save,
		// but it's safer I guess, just in case we crash on shutdown
		wallpaper.saveConfig();
	}
}

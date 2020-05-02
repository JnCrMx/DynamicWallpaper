package de.jcm.dynamicwallpaper;

import de.jcm.dynamicwallpaper.colormode.*;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
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
import java.util.Enumeration;

public class ControlFrame extends JFrame
{
	private final DynamicWallpaper wallpaper;
	private final JTextField filePathField;

	private final JPanel colorModePanel;
	private final ButtonGroup typeGroup;
	private final JCheckBox relativePathCheckBox;

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
				filePathField.setText(wallpaper.videoFile.getPath());
				filePanel.add(filePathField, BorderLayout.CENTER);

				JButton browseButton = new JButton("Browse");
				browseButton.setToolTipText("Open dialog to select file");
				browseButton.addActionListener(e->{
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(wallpaper.videoFile);
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

				relativePathCheckBox = new JCheckBox("relative path");
				relativePathCheckBox.setToolTipText("Store as relative path in config");
				relativePathCheckBox.setSelected(wallpaper.relative);
				filePanel.add(relativePathCheckBox, BorderLayout.SOUTH);
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
				apply.addActionListener(e->update());
				controlButtonPanel.add(apply);

				JButton exit = new JButton("Exit");
				exit.addActionListener(e-> GLFW.glfwSetWindowShouldClose(wallpaper.getWindow(), true));
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

	public void update()
	{
		if(!filePathField.getText().isEmpty())
		{
			File newFile = new File(filePathField.getText());
			try
			{
				if(!Files.isSameFile(newFile.toPath(), wallpaper.videoFile.toPath()))
				{
					wallpaper.videoFile = newFile;

					try
					{
						wallpaper.frameGrabber.get().close();
						wallpaper.frameGrabber.set(new FFmpegFrameGrabber(wallpaper.videoFile));
						wallpaper.frameGrabber.get().start();
					}
					catch(FrameGrabber.Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		wallpaper.relative = relativePathCheckBox.isSelected();

		wallpaper.colorMode = colorMode;
		modeConfigurationPanel.apply();
	}
}

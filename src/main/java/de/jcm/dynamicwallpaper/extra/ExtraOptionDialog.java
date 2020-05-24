package de.jcm.dynamicwallpaper.extra;

import de.jcm.dynamicwallpaper.AutostartHelper;
import de.jcm.dynamicwallpaper.ControlFrame;
import de.jcm.dynamicwallpaper.DynamicWallpaper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ExtraOptionDialog extends JDialog
{
	public ExtraOptionDialog(ControlFrame parent, DynamicWallpaper wallpaper)
	{
		super(parent, "Extra options", ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);

		JPanel contentPane = new JPanel(new BorderLayout());
		{
			JPanel overlayPanel = new JPanel();
			ArrayList<JCheckBox> checkBoxes = new ArrayList<>();
			overlayPanel.setBorder(BorderFactory.createTitledBorder("Overlays"));
			{
				for(Class<? extends Overlay> clazz : Overlay.OVERLAYS)
				{
					try
					{
						Overlay qInstance = clazz.getConstructor().newInstance();

						JCheckBox checkBox = new JCheckBox(qInstance.getName());
						checkBox.setSelected(Arrays.asList(wallpaper.getOverlayCache())
								                     .contains(clazz.getName()));
						checkBox.setActionCommand(clazz.getName());
						checkBox.addActionListener(e->parent.overlayCache = checkBoxes.stream()
								.filter(JCheckBox::isSelected)
								.map(JCheckBox::getActionCommand)
								.toArray(String[]::new));

						overlayPanel.add(checkBox);
						checkBoxes.add(checkBox);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			contentPane.add(overlayPanel, BorderLayout.CENTER);

			JButton autostartButton = new JButton(AutostartHelper.hasAutostart() ?
					                                      "Unregister autostart":
					                                      "Register autostart");
			autostartButton.addActionListener(e->{
				if(AutostartHelper.hasAutostart())
				{
					AutostartHelper.unregisterAutostart();
				}
				else
				{
					AutostartHelper.registerAutostart();
				}
				autostartButton.setText(AutostartHelper.hasAutostart() ?
						                        "Unregister autostart":
						                        "Register autostart");
				pack();
			});
			contentPane.add(autostartButton, BorderLayout.SOUTH);
		}
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(null);
	}
}

package de.jcm.dynamicwallpaper.extra;

import de.jcm.dynamicwallpaper.AutostartHelper;

import javax.swing.*;

public class ExtraOptionDialog extends JDialog
{
	public ExtraOptionDialog(JFrame parent)
	{
		super(parent, "Extra options", ModalityType.APPLICATION_MODAL);
		setAlwaysOnTop(true);

		JPanel contentPane = new JPanel();
		{
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
			contentPane.add(autostartButton);
		}
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(null);
	}
}

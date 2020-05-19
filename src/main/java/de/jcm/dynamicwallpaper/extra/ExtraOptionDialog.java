package de.jcm.dynamicwallpaper.extra;

import de.jcm.dynamicwallpaper.AutostartHelper;

import javax.swing.*;

public class ExtraOptionDialog extends JDialog
{
	public ExtraOptionDialog(JFrame parent)
	{
		super(parent, "Extra options");

		AutostartHelper.init();

		JPanel contentPane = new JPanel();
		{
			JScrollPane scrollPane = new JScrollPane();

			JTextArea autostartCommand = new JTextArea(1, 50);
			autostartCommand.setText(AutostartHelper.getAutostart()==null?
					                         AutostartHelper.getCommand():
					                         AutostartHelper.getAutostart().getRight());
			scrollPane.setViewportView(autostartCommand);

			contentPane.add(scrollPane);

			JButton autostartButton = new JButton(AutostartHelper.getAutostart()==null ?
					                                      "Register autostart":
					                                      "Unregister autostart");
			autostartButton.addActionListener(e->{
				if(AutostartHelper.getAutostart() == null)
				{
					AutostartHelper.registerAutostart(autostartCommand.getText());
				}
				else
				{
					AutostartHelper.unregisterAutostart();
				}
				AutostartHelper.init();
				autostartButton.setText(AutostartHelper.getAutostart()==null ?
						                        "Register autostart":
						                        "Unregister autostart");
				pack();
			});
			contentPane.add(autostartButton);
		}
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(null);
	}
}

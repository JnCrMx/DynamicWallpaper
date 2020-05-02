package de.jcm.dynamicwallpaper.colormode;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ActivityColorDialog extends JDialog
{
	public ActivityColorDialog(ActivityColorMode.ActivityConfigurationPanel cPanel,
	                           ActivityColorEntry entry)
	{
		super(SwingUtilities.getWindowAncestor(cPanel), ModalityType.APPLICATION_MODAL);

		setAlwaysOnTop(true);

		JPanel panel = new JPanel(new BorderLayout());

		JTextField keyword;
		JComboBox<ActivityColorEntry.FilterType> typeBox;
		JComboBox<ActivityColorEntry.FilterPolicy> policyBox;

		JPanel matcherPanel = new JPanel();
		matcherPanel.setBorder(new TitledBorder("Matcher"));
		matcherPanel.setLayout(new BoxLayout(matcherPanel, BoxLayout.LINE_AXIS));
		{
			typeBox = new JComboBox<>(ActivityColorEntry.FilterType.values());
			typeBox.setSelectedItem(ActivityColorEntry.FilterType.PROCESS_NAME);
			matcherPanel.add(typeBox);

			policyBox = new JComboBox<>(ActivityColorEntry.FilterPolicy.values());
			policyBox.setSelectedItem(ActivityColorEntry.FilterPolicy.EQUALS);
			matcherPanel.add(policyBox);

			keyword = new JTextField(10);
			matcherPanel.add(keyword);
		}
		panel.add(matcherPanel, BorderLayout.NORTH);

		JColorChooser colorChooser = new JColorChooser();
		colorChooser.setBorder(new TitledBorder("Color"));
		panel.add(colorChooser, BorderLayout.CENTER);

		if(entry!=null)
		{
			keyword.setText(entry.keyword);
			typeBox.setSelectedItem(entry.type);
			policyBox.setSelectedItem(entry.policy);
			colorChooser.setColor(entry.color);
		}

		JButton saveButton = new JButton(entry==null?"Add":"Save");
		saveButton.addActionListener(e->{
			ActivityColorEntry entry1 = entry;
			if(entry1 == null)
			{
				entry1 = new ActivityColorEntry();
				cPanel.list.addLast(entry1);
			}
			entry1.type = (ActivityColorEntry.FilterType) typeBox.getSelectedItem();
			entry1.policy = (ActivityColorEntry.FilterPolicy) policyBox.getSelectedItem();
			entry1.keyword = keyword.getText();
			entry1.color = colorChooser.getColor();

			setVisible(false);
			cPanel.revalidate();
			cPanel.repaint();
		});
		panel.add(saveButton, BorderLayout.SOUTH);

		setContentPane(panel);

		pack();
	}
}

// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cef.browser.CefBrowser;

@SuppressWarnings("serial")
public class SearchDialog extends JDialog
{
	private final CefBrowser browser;
	private final JTextField searchField = new JTextField(30);
	private final JCheckBox caseCheckBox = new JCheckBox("Case sensitive");
	private final JButton prevButton = new JButton("Prev");
	private final JButton nextButton = new JButton("Next");

	public SearchDialog(Frame owner, CefBrowser browser)
	{
		super(owner, "Find...", false);
		this.browser = browser;

		setLayout(new BorderLayout());
		setSize(400, 100);

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		searchPanel.add(Box.createHorizontalStrut(5));
		searchPanel.add(new JLabel("Search:"));
		searchPanel.add(searchField);

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
		controlPanel.add(Box.createHorizontalStrut(5));

		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(e -> {
			if (searchField.getText() == null
					|| searchField.getText().isEmpty()) {
				return;
			}

			setTitle("Find \"" + searchField.getText() + "\"");
			boolean matchCase = caseCheckBox.isSelected();
			browser.find(searchField.getText(), true, matchCase, false);
			prevButton.setEnabled(true);
			nextButton.setEnabled(true);
		});
		controlPanel.add(searchButton);

		prevButton.addActionListener(e -> {
			boolean matchCase = caseCheckBox.isSelected();
			setTitle("Find \"" + searchField.getText() + "\"");
			browser.find(searchField.getText(), false, matchCase, true);
		});
		prevButton.setEnabled(false);
		controlPanel.add(prevButton);

		nextButton.addActionListener(e -> {
			boolean matchCase = caseCheckBox.isSelected();
			setTitle("Find \"" + searchField.getText() + "\"");
			browser.find(searchField.getText(), true, matchCase, true);
		});
		nextButton.setEnabled(false);
		controlPanel.add(nextButton);

		controlPanel.add(Box.createHorizontalStrut(50));

		JButton doneButton = new JButton("Done");
		doneButton.addActionListener(e -> {
			setVisible(false);
			dispose();
		});
		controlPanel.add(doneButton);

		add(searchPanel, BorderLayout.NORTH);
		add(caseCheckBox);
		add(controlPanel, BorderLayout.SOUTH);
	}

	@Override
	public void dispose()
	{
		browser.stopFinding(true);
		super.dispose();
	}
}

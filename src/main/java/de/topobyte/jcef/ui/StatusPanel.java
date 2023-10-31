// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.ui;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial")
public class StatusPanel extends JPanel
{
	private final JProgressBar progressBar;
	private final JLabel statusField;

	public StatusPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(Box.createHorizontalStrut(5));
		add(Box.createHorizontalStrut(5));

		progressBar = new JProgressBar();
		Dimension progressBarSize = progressBar.getMaximumSize();
		progressBarSize.width = 100;
		progressBar.setMinimumSize(progressBarSize);
		progressBar.setMaximumSize(progressBarSize);
		add(progressBar);
		add(Box.createHorizontalStrut(5));

		statusField = new JLabel("Info");
		statusField.setAlignmentX(LEFT_ALIGNMENT);
		add(statusField);
		add(Box.createHorizontalStrut(5));
		add(Box.createVerticalStrut(21));
	}

	public void setIsInProgress(boolean inProgress)
	{
		progressBar.setIndeterminate(inProgress);
	}

	public void setStatusText(String text)
	{
		statusField.setText(text);
	}
}

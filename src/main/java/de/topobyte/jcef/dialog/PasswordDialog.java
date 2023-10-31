// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.dialog;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.cef.callback.CefAuthCallback;

@SuppressWarnings("serial")
public class PasswordDialog extends JDialog implements Runnable
{
	private final JTextField username = new JTextField(20);
	private final JPasswordField password = new JPasswordField(20);
	private final CefAuthCallback callback;

	public PasswordDialog(Frame owner, CefAuthCallback callback)
	{
		super(owner, "Authentication required", true);
		this.callback = callback;
		setSize(400, 100);
		setLayout(new GridLayout(0, 2));
		add(new JLabel("Username:"));
		add(username);
		add(new JLabel("Password:"));
		add(password);

		JButton abortButton = new JButton("Abort");
		abortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				callback.cancel();
				setVisible(false);
				dispose();
			}
		});
		add(abortButton);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (username.getText().isEmpty()) {
					return;
				}
				String password = new String(
						PasswordDialog.this.password.getPassword());
				callback.Continue(username.getText(), password);
				setVisible(false);
				dispose();
			}
		});
		add(okButton);
	}

	@Override
	public void run()
	{
		setVisible(true);
	}
}

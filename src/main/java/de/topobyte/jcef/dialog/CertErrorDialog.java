// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.dialog;

import java.awt.Frame;

import javax.swing.JOptionPane;

import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler.ErrorCode;

public class CertErrorDialog implements Runnable
{
	private final Frame owner;
	private final ErrorCode certError;
	private final String requestUrl;
	private final CefCallback callback;

	public CertErrorDialog(Frame owner, ErrorCode certError,
			String requestUrl, CefCallback callback)
	{
		this.owner = owner;
		this.certError = certError;
		this.requestUrl = requestUrl;
		this.callback = callback;
	}

	@Override
	public void run()
	{
		int dialogResult = JOptionPane.showConfirmDialog(owner,
				"An certificate error (" + certError + ") occurreed "
						+ "while requesting\n" + requestUrl
						+ "\nDo you want to proceed anyway?",
				"Certificate error", JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE);
		if (dialogResult == JOptionPane.YES_OPTION) {
			callback.Continue();
		} else {
			callback.cancel();
		}
	}
}

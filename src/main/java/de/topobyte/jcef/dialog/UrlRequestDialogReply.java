// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefURLRequestClient;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;
import org.cef.network.CefURLRequest.Status;

@SuppressWarnings("serial")
public class UrlRequestDialogReply extends JDialog
		implements CefURLRequestClient
{
	private long nativeRef = 0;
	private final JLabel statusLabel = new JLabel("HTTP-Request status: ");
	private final JTextArea sentRequest = new JTextArea();
	private final JTextArea repliedResult = new JTextArea();
	private final JButton cancelButton = new JButton("Cancel");
	private CefURLRequest urlRequest = null;
	private final Frame owner;
	private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

	public UrlRequestDialogReply(Frame owner, String title)
	{
		super(owner, title, false);
		setLayout(new BorderLayout());
		setSize(800, 600);

		this.owner = owner;

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
		JButton doneButton = new JButton("Done");
		doneButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				urlRequest.dispose();
				setVisible(false);
				dispose();
			}
		});
		controlPanel.add(doneButton);

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (urlRequest != null) {
					urlRequest.cancel();
				}
			}
		});
		cancelButton.setEnabled(false);
		controlPanel.add(cancelButton);

		JPanel requestPane = createPanelWithTitle("Sent HTTP-Request", 1, 0);
		requestPane.add(new JScrollPane(sentRequest));

		JPanel replyPane = createPanelWithTitle("Reply from the server", 1, 0);
		replyPane.add(new JScrollPane(repliedResult));

		JPanel contentPane = new JPanel(new GridLayout(2, 0));
		contentPane.add(requestPane);
		contentPane.add(replyPane);

		add(statusLabel, BorderLayout.PAGE_START);
		add(contentPane, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.PAGE_END);
	}

	private JPanel createPanelWithTitle(String title, int rows, int cols)
	{
		JPanel result = new JPanel(new GridLayout(rows, cols));
		result.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(title),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		return result;
	}

	public void send(CefRequest request)
	{
		if (request == null) {
			statusLabel.setText("HTTP-Request status: FAILED");
			sentRequest.append("Can't send CefRequest because it is NULL");
			cancelButton.setEnabled(false);
			return;
		}

		urlRequest = CefURLRequest.create(request, this);
		if (urlRequest == null) {
			statusLabel.setText("HTTP-Request status: FAILED");
			sentRequest.append(
					"Can't send CefRequest because creation of CefURLRequest failed.");
			repliedResult.append(
					"The native code (CEF) returned a NULL-Pointer for CefURLRequest.");
			cancelButton.setEnabled(false);
		} else {
			sentRequest.append(request.toString());
			cancelButton.setEnabled(true);
			updateStatus("", false);
		}
	}

	private void updateStatus(final String updateMsg,
			final boolean printByteStream)
	{
		final Status status = urlRequest.getRequestStatus();
		Runnable runnable = new Runnable() {
			@Override
			public void run()
			{
				statusLabel.setText("HTTP-Request status: " + status);
				if (status != Status.UR_UNKNOWN
						&& status != Status.UR_IO_PENDING) {
					cancelButton.setEnabled(false);
				}
				repliedResult.append(updateMsg);
				if (printByteStream) {
					try {
						repliedResult
								.append("\n\n" + byteStream.toString("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						repliedResult.append("\n\n" + byteStream.toString());
					}
				}
			}
		};

		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			SwingUtilities.invokeLater(runnable);
		}
	}

	// CefURLRequestClient

	@Override
	public void setNativeRef(String identifer, long nativeRef)
	{
		this.nativeRef = nativeRef;
	}

	@Override
	public long getNativeRef(String identifer)
	{
		return nativeRef;
	}

	@Override
	public void onRequestComplete(CefURLRequest request)
	{
		String updateStr = "onRequestCompleted\n\n";
		CefResponse response = request.getResponse();
		boolean isText = response.getHeaderByName("Content-Type")
				.startsWith("text");
		updateStr += response.toString();
		updateStatus(updateStr, isText);
	}

	@Override
	public void onUploadProgress(CefURLRequest request, int current, int total)
	{
		updateStatus("onUploadProgress: " + current + "/" + total + " bytes\n",
				false);
	}

	@Override
	public void onDownloadProgress(CefURLRequest request, int current,
			int total)
	{
		updateStatus(
				"onDownloadProgress: " + current + "/" + total + " bytes\n",
				false);
	}

	@Override
	public void onDownloadData(CefURLRequest request, byte[] data,
			int dataLength)
	{
		byteStream.write(data, 0, dataLength);
		updateStatus("onDownloadData: " + dataLength + " bytes\n", false);
	}

	@Override
	public boolean getAuthCredentials(boolean isProxy, String host, int port,
			String realm, String scheme, CefAuthCallback callback)
	{
		SwingUtilities.invokeLater(new PasswordDialog(owner, callback));
		return true;
	}
}

// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;

@SuppressWarnings("serial")
public class DownloadDialog extends JDialog implements CefDownloadHandler
{
	private final Frame owner;
	private final Map<Integer, DownloadObject> downloadObjects = new HashMap<>();
	private final JPanel downloadPanel = new JPanel();
	private final DownloadDialog dialog;

	public DownloadDialog(Frame owner)
	{
		super(owner, "Downloads", false);
		setVisible(false);
		setSize(400, 300);

		this.owner = owner;
		this.dialog = this;
		downloadPanel.setLayout(new BoxLayout(downloadPanel, BoxLayout.Y_AXIS));
		add(downloadPanel);
	}

	private class DownloadObject extends JPanel
	{
		private boolean isHidden = true;
		private final int identifier;
		private JLabel fileName = new JLabel();
		private JLabel status = new JLabel();
		private JButton dlAbort = new JButton();
		private JButton dlRemoveEntry = new JButton("x");
		private CefDownloadItemCallback callback;
		private Color bgColor;

		DownloadObject(CefDownloadItem downloadItem, String suggestedName)
		{
			super();
			setOpaque(true);
			setLayout(new BorderLayout());
			setMaximumSize(new Dimension(dialog.getWidth() - 10, 80));
			identifier = downloadItem.getId();
			bgColor = identifier % 2 == 0 ? Color.WHITE : Color.YELLOW;
			setBackground(bgColor);

			fileName.setText(suggestedName);
			add(fileName, BorderLayout.NORTH);

			status.setAlignmentX(LEFT_ALIGNMENT);
			add(status, BorderLayout.CENTER);

			JPanel controlPane = new JPanel();
			controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.X_AXIS));
			controlPane.setOpaque(true);
			controlPane.setBackground(bgColor);
			dlAbort.setText("Abort");
			dlAbort.setEnabled(false);
			dlAbort.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (callback != null) {
						fileName.setText("ABORTED - " + fileName.getText());
						callback.cancel();
					}
				}
			});
			controlPane.add(dlAbort);

			dlRemoveEntry.setEnabled(false);
			dlRemoveEntry.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					DownloadObject removed = downloadObjects.remove(identifier);
					if (removed != null) {
						downloadPanel.remove(removed);
						dialog.repaint();
					}
				}
			});
			controlPane.add(dlRemoveEntry);
			add(controlPane, BorderLayout.SOUTH);

			update(downloadItem, null);
		}

		// The method humanReadableByteCount() is based on
		// http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
		String humanReadableByteCount(long bytes)
		{
			int unit = 1024;
			if (bytes < unit) {
				return bytes + " B";
			}

			int exp = (int) (Math.log(bytes) / Math.log(unit));
			String pre = "" + ("kMGTPE").charAt(exp - 1);
			return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}

		void update(CefDownloadItem downloadItem,
				CefDownloadItemCallback callback)
		{
			int percentComplete = downloadItem.getPercentComplete();
			String rcvBytes = humanReadableByteCount(
					downloadItem.getReceivedBytes());
			String totalBytes = humanReadableByteCount(
					downloadItem.getTotalBytes());
			String speed = humanReadableByteCount(
					downloadItem.getCurrentSpeed()) + "it/s";

			if (downloadItem.getReceivedBytes() >= 5 && isHidden) {
				dialog.setVisible(true);
				dialog.toFront();
				owner.toBack();
				isHidden = false;
			}
			Runtime.getRuntime().runFinalization();

			this.callback = callback;
			status.setText(rcvBytes + " of " + totalBytes + " - "
					+ percentComplete + "%" + " - " + speed);
			dlAbort.setEnabled(downloadItem.isInProgress());
			dlRemoveEntry.setEnabled(!downloadItem.isInProgress()
					|| downloadItem.isCanceled() || downloadItem.isComplete());
			if (!downloadItem.isInProgress() && !downloadItem.isCanceled()
					&& !downloadItem.isComplete()) {
				fileName.setText("FAILED - " + fileName.getText());
				callback.cancel();
			}
		}
	}

	@Override
	public void onBeforeDownload(CefBrowser browser,
			CefDownloadItem downloadItem, String suggestedName,
			CefBeforeDownloadCallback callback)
	{
		callback.Continue(suggestedName, true);

		DownloadObject dlObject = new DownloadObject(downloadItem,
				suggestedName);
		downloadObjects.put(downloadItem.getId(), dlObject);
		downloadPanel.add(dlObject);
	}

	@Override
	public void onDownloadUpdated(CefBrowser browser,
			CefDownloadItem downloadItem, CefDownloadItemCallback callback)
	{
		DownloadObject dlObject = downloadObjects.get(downloadItem.getId());
		if (dlObject == null) {
			return;
		}
		dlObject.update(downloadItem, callback);
	}
}

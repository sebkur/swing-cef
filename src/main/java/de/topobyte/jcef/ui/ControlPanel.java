// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.ui;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cef.OS;
import org.cef.browser.CefBrowser;

import com.formdev.flatlaf.extras.FlatSVGIcon;

@SuppressWarnings("serial")
public class ControlPanel extends JPanel
{
	private final JButton backButton;
	private final JButton forwardButton;
	private final JButton reloadButton;
	private final JTextField addressField;
	private final JLabel zoomLabel;
	private double zoomLevel = 0;
	private final CefBrowser browser;

	private static final int ICON_SIZE = 16;

	public ControlPanel(CefBrowser browser)
	{
		assert browser != null;
		this.browser = browser;

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(Box.createHorizontalStrut(5));
		add(Box.createHorizontalStrut(5));

		backButton = new JButton();
		backButton.setToolTipText("Back");
		backButton.setIcon(icon("icons/back.svg"));
		backButton.setFocusable(false);
		backButton.setAlignmentX(LEFT_ALIGNMENT);
		backButton.addActionListener(e -> {
			browser.goBack();
		});
		add(backButton);
		add(Box.createHorizontalStrut(5));

		forwardButton = new JButton();
		forwardButton.setToolTipText("Forward");
		forwardButton.setIcon(icon("icons/forward.svg"));
		forwardButton.setFocusable(false);
		forwardButton.setAlignmentX(LEFT_ALIGNMENT);
		forwardButton.addActionListener(e -> {
			browser.goForward();
		});
		add(forwardButton);
		add(Box.createHorizontalStrut(5));

		reloadButton = new JButton();
		reloadButton.setToolTipText("Reload");
		reloadButton.setIcon(icon("icons/refresh.svg"));
		reloadButton.setFocusable(false);
		reloadButton.setAlignmentX(LEFT_ALIGNMENT);
		reloadButton.addActionListener(e -> {
			if (reloadButton.getText().equalsIgnoreCase("reload")) {
				int mask = OS.isMacintosh() ? ActionEvent.META_MASK
						: ActionEvent.CTRL_MASK;
				if ((e.getModifiers() & mask) != 0) {
					System.out.println("Reloading - ignoring cached values");
					browser.reloadIgnoreCache();
				} else {
					System.out.println("Reloading - using cached values");
					browser.reload();
				}
			} else {
				browser.stopLoad();
			}
		});
		add(reloadButton);
		add(Box.createHorizontalStrut(5));

		addressField = new HintTextField(100, "Type an address here");
		addressField.setAlignmentX(LEFT_ALIGNMENT);
		addressField.addActionListener(e -> {
			browser.loadURL(getAddress());
		});
		add(addressField);
		add(Box.createHorizontalStrut(5));

		JButton goButton = new JButton();
		goButton.setToolTipText("Go");
		goButton.setIcon(icon("icons/return.svg"));
		goButton.setFocusable(false);
		goButton.setAlignmentX(LEFT_ALIGNMENT);
		goButton.addActionListener(e -> {
			browser.loadURL(getAddress());
		});
		add(goButton);
		add(Box.createHorizontalStrut(5));

		zoomLabel = new JLabel("0.0");

		JButton minusButton = new JButton();
		minusButton.setToolTipText("Decrease zoom");
		minusButton.setIcon(icon("icons/zoomOut.svg"));
		minusButton.setFocusable(false);
		minusButton.setAlignmentX(CENTER_ALIGNMENT);
		minusButton.addActionListener(e -> {
			browser.setZoomLevel(--zoomLevel);
			zoomLabel.setText(Double.valueOf(zoomLevel).toString());
		});
		add(minusButton);

		add(zoomLabel);

		JButton plusButton = new JButton("");
		plusButton.setToolTipText("Increase zoom");
		plusButton.setIcon(icon("icons/zoomIn.svg"));
		plusButton.setFocusable(false);
		plusButton.setAlignmentX(CENTER_ALIGNMENT);
		plusButton.addActionListener(e -> {
			browser.setZoomLevel(++zoomLevel);
			zoomLabel.setText(Double.valueOf(zoomLevel).toString());
		});
		add(plusButton);
	}

	private FlatSVGIcon icon(String string)
	{
		return new FlatSVGIcon(string, ICON_SIZE, ICON_SIZE);
	}

	public void update(CefBrowser browser, boolean isLoading, boolean canGoBack,
			boolean canGoForward)
	{
		if (this.browser == browser) {
			backButton.setEnabled(canGoBack);
			forwardButton.setEnabled(canGoForward);
			if (isLoading) {
				reloadButton.setToolTipText("Abort");
				reloadButton.setIcon(icon("icons/cancel.svg"));
			} else {
				reloadButton.setToolTipText("Reload");
				reloadButton.setIcon(icon("icons/refresh.svg"));
			}
		}
	}

	public String getAddress()
	{
		String address = addressField.getText();
		// If the URI format is unknown "new URI" will throw an
		// exception. In this case we interpret the value of the
		// address field as search request. Therefore we simply add
		// the "search" scheme.
		try {
			address = address.replaceAll(" ", "%20");
			URI test = new URI(address);
			if (test.getScheme() != null) {
				return address;
			}
			if (test.getHost() != null && test.getPath() != null) {
				return address;
			}
			String specific = test.getSchemeSpecificPart();
			if (specific.indexOf('.') == -1) {
				throw new URISyntaxException(specific, "No dot inside domain");
			}
		} catch (URISyntaxException e1) {
			address = "search://" + address;
		}
		return address;
	}

	public void setAddress(CefBrowser browser, String address)
	{
		if (this.browser == browser) {
			addressField.setText(address);
		}
	}

	public JTextField getAddressField()
	{
		return addressField;
	}
}

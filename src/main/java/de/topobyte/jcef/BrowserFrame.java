// Copyright (c) 2018 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;

public class BrowserFrame extends JFrame
{
	private static final long serialVersionUID = 1L;

	private volatile boolean isClosed = false;
	private CefBrowser browser = null;
	private static int browserCount = 0;
	private Runnable afterParentChangedAction = null;

	public BrowserFrame()
	{
		this(null);
	}

	public BrowserFrame(String title)
	{
		super(title);

		// Browser window closing works as follows:
		// 1. Clicking the window X button calls WindowAdapter.windowClosing.
		// 2. WindowAdapter.windowClosing calls CefBrowser.close(false).
		// 3. CEF calls CefLifeSpanHandler.doClose() which calls
		// CefBrowser.doClose()
		// which returns true (canceling the close).
		// 4. CefBrowser.doClose() triggers another call to
		// WindowAdapter.windowClosing.
		// 5. WindowAdapter.windowClosing calls CefBrowser.close(true).
		// 6. For windowed browsers CEF destroys the native window handle. For
		// OSR
		// browsers CEF calls CefLifeSpanHandler.doClose() which calls
		// CefBrowser.doClose() again which returns false (allowing the close).
		// 7. CEF calls CefLifeSpanHandler.onBeforeClose and the browser is
		// destroyed.
		//
		// On macOS pressing Cmd+Q results in a call to
		// CefApp.handleBeforeTerminate
		// which calls CefBrowser.close(true) for each existing browser. CEF
		// then calls
		// CefLifeSpanHandler.onBeforeClose and the browser is destroyed.
		//
		// Application shutdown works as follows:
		// 1. CefLifeSpanHandler.onBeforeClose calls
		// CefApp.getInstance().dispose()
		// when the last browser window is destroyed.
		// 2. CefAppHandler.stateHasChanged terminates the application by
		// calling
		// System.exit(0) when the state changes to CefAppState.TERMINATED.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (browser == null) {
					// If there's no browser we can dispose immediately.
					isClosed = true;
					System.out.println(
							"BrowserFrame.windowClosing Frame.dispose");
					dispose();
					return;
				}

				boolean isClosed = BrowserFrame.this.isClosed;

				if (isClosed) {
					// Cause browser.doClose() to return false so that OSR
					// browsers
					// can close.
					browser.setCloseAllowed();
				}

				// Results in another call to this method.
				System.out
						.println("BrowserFrame.windowClosing CefBrowser.close("
								+ isClosed + ")");
				browser.close(isClosed);
				if (!isClosed) {
					BrowserFrame.this.isClosed = true;
				}
				if (isClosed) {
					// Dispose after the 2nd call to this method.
					System.out.println(
							"BrowserFrame.windowClosing Frame.dispose");
					dispose();
				}
			}
		});
	}

	public void setBrowser(CefBrowser browser)
	{
		if (this.browser == null) {
			this.browser = browser;
		}

		browser.getClient().removeLifeSpanHandler();
		browser.getClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
			@Override
			public void onAfterCreated(CefBrowser browser)
			{
				System.out.println("BrowserFrame.onAfterCreated id="
						+ browser.getIdentifier());
				browserCount++;
			}

			@Override
			public void onAfterParentChanged(CefBrowser browser)
			{
				System.out.println("BrowserFrame.onAfterParentChanged id="
						+ browser.getIdentifier());
				if (afterParentChangedAction != null) {
					SwingUtilities.invokeLater(afterParentChangedAction);
					afterParentChangedAction = null;
				}
			}

			@Override
			public boolean doClose(CefBrowser browser)
			{
				boolean result = browser.doClose();
				System.out.println(
						"BrowserFrame.doClose id=" + browser.getIdentifier()
								+ " CefBrowser.doClose=" + result);
				return result;
			}

			@Override
			public void onBeforeClose(CefBrowser browser)
			{
				System.out.println("BrowserFrame.onBeforeClose id="
						+ browser.getIdentifier());
				if (--browserCount == 0) {
					System.out.println(
							"BrowserFrame.onBeforeClose CefApp.dispose");
					CefApp.getInstance().dispose();
				}
			}
		});
	}

	public void removeBrowser(Runnable r)
	{
		System.out.println("BrowserFrame.removeBrowser");
		afterParentChangedAction = r;
		remove(browser.getUIComponent());
		// The removeNotify() notification should be sent as a result of calling
		// remove().
		// However, it isn't in all cases so we do it manually here.
		browser.getUIComponent().removeNotify();
		browser = null;
	}

	public CefBrowser getBrowser()
	{
		return browser;
	}
}

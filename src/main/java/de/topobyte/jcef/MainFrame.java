// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef;

import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefCookieManager;

import com.formdev.flatlaf.FlatLightLaf;

import de.topobyte.jcef.dialog.DownloadDialog;
import de.topobyte.jcef.handler.ContextMenuHandler;
import de.topobyte.jcef.handler.DragHandler;
import de.topobyte.jcef.handler.JSDialogHandler;
import de.topobyte.jcef.handler.KeyboardHandler;
import de.topobyte.jcef.handler.MessageRouterHandler;
import de.topobyte.jcef.handler.MessageRouterHandlerEx;
import de.topobyte.jcef.handler.RequestHandler;
import de.topobyte.jcef.ui.ControlPanel;
import de.topobyte.jcef.ui.MenuBar;
import de.topobyte.jcef.ui.StatusPanel;
import de.topobyte.jcef.util.DataUri;
import de.topobyte.shared.preferences.SharedPreferences;
import de.topobyte.swing.util.SwingUtils;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;

public class MainFrame extends BrowserFrame
{
	private static final long serialVersionUID = -2295538706810864538L;

	public static CefApp cefApp;

	public static void main(String[] args)
			throws IOException, UnsupportedPlatformException,
			InterruptedException, CefInitializationException
	{
		boolean test = false;
		for (String arg : args) {
			if (arg.equals("--test")) {
				test = true;
			}
		}

		double factor = 1;
		if (SharedPreferences.isUIScalePresent()) {
			SwingUtils.setUiScale(SharedPreferences.getUIScale());
			factor = SharedPreferences.getUIScale();
		}

		FlatLightLaf.setup();

		// OSR mode is enabled by default on Linux.
		// and disabled by default on Windows and Mac OS X.
		boolean osrEnabledArg = false;
		boolean transparentPaintingEnabledArg = false;
		boolean createImmediately = false;
		for (String arg : args) {
			arg = arg.toLowerCase();
			if (arg.equals("--off-screen-rendering-enabled")) {
				osrEnabledArg = true;
			} else if (arg.equals("--transparent-painting-enabled")) {
				transparentPaintingEnabledArg = true;
			} else if (arg.equals("--create-immediately")) {
				createImmediately = true;
			}
		}

		CefAppBuilder builder = new CefAppBuilder();

		// Configure the builder instance
		builder.setInstallDir(new File("jcef-bundle")); // Default
		builder.setProgressHandler(new ConsoleProgressHandler()); // Default
		builder.getCefSettings().windowless_rendering_enabled = osrEnabledArg;
		// Default - select OSR mode

		// Set an app handler. Do not use CefApp.addAppHandler(...), it will
		// break your code on MacOSX!
		builder.setAppHandler(new MavenCefAppHandlerAdapter() {
			// CefApp is responsible for the global CEF context. It loads all
			// required native libraries, initializes CEF accordingly, starts a
			// background task to handle CEF's message loop and takes care of
			// shutting down CEF after disposing it.
			@Override
			public void stateHasChanged(org.cef.CefApp.CefAppState state)
			{
				// Shutdown the app if the native CEF part is terminated
				if (state == CefAppState.TERMINATED) {
					System.exit(0);
				}
			}
		});

		cefApp = builder.build();

		System.out.println("Offscreen rendering "
				+ (osrEnabledArg ? "enabled" : "disabled"));

		// MainFrame keeps all the knowledge to display the embedded browser
		// frame.
		final MainFrame frame = new MainFrame(cefApp, osrEnabledArg,
				transparentPaintingEnabledArg, createImmediately, test);
		try (InputStream input = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("icon.png")) {
			BufferedImage image = ImageIO.read(input);
			frame.setIconImage(image);
		} catch (IOException e) {
			// ignore, continue without icon
		}
		frame.setSize((int) (800 * factor), (int) (600 * factor));
		frame.setVisible(true);
	}

	private final CefClient client;
	private String errorMsg = "";
	private ControlPanel controlPane;
	private StatusPanel statusPanel;
	private boolean browserFocus = true;
	private boolean osrEnabled;
	private boolean transparentPaintingEnabled;

	public MainFrame(CefApp myApp, boolean osrEnabled,
			boolean transparentPaintingEnabled, boolean createImmediately,
			boolean test)
	{
		this.osrEnabled = osrEnabled;
		this.transparentPaintingEnabled = transparentPaintingEnabled;

		// By calling the method createClient() the native part
		// of JCEF/CEF will be initialized and an instance of
		// CefClient will be created. You can create one to many
		// instances of CefClient.
		client = myApp.createClient();

		// 2) You have the ability to pass different handlers to your
		// instance of CefClient. Each handler is responsible to
		// deal with different informations (e.g. keyboard input).
		//
		// For each handler (with more than one method) adapter
		// classes exists. So you don't need to override methods
		// you're not interested in.
		DownloadDialog downloadDialog = new DownloadDialog(this);
		client.addContextMenuHandler(new ContextMenuHandler(this));
		client.addDownloadHandler(downloadDialog);
		client.addDragHandler(new DragHandler());
		client.addJSDialogHandler(new JSDialogHandler());
		client.addKeyboardHandler(new KeyboardHandler());
		client.addRequestHandler(new RequestHandler(this));

		// Beside the normal handler instances, we're registering a
		// MessageRouter
		// as well. That gives us the opportunity to reply to JavaScript method
		// calls (JavaScript binding). We're using the default configuration, so
		// that the JavaScript binding methods "cefQuery" and "cefQueryCancel"
		// are used.
		CefMessageRouter msgRouter = CefMessageRouter.create();
		msgRouter.addHandler(new MessageRouterHandler(), true);
		msgRouter.addHandler(new MessageRouterHandlerEx(client), false);
		client.addMessageRouter(msgRouter);

		// 2.1) We're overriding CefDisplayHandler as nested anonymous class
		// to update our address-field, the title of the panel as well
		// as for updating the status-bar on the bottom of the browser
		client.addDisplayHandler(new CefDisplayHandlerAdapter() {
			@Override
			public void onAddressChange(CefBrowser browser, CefFrame frame,
					String url)
			{
				controlPane.setAddress(browser, url);
			}

			@Override
			public void onTitleChange(CefBrowser browser, String title)
			{
				setTitle(title);
			}

			@Override
			public void onStatusMessage(CefBrowser browser, String value)
			{
				statusPanel.setStatusText(value);
			}
		});

		// 2.2) To disable/enable navigation buttons and to display a prgress
		// bar
		// which indicates the load state of our website, we're overloading
		// the CefLoadHandler as nested anonymous class. Beside this, the
		// load handler is responsible to deal with (load) errors as well.
		// For example if you navigate to a URL which does not exist, the
		// browser will show up an error message.
		client.addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadingStateChange(CefBrowser browser,
					boolean isLoading, boolean canGoBack, boolean canGoForward)
			{
				controlPane.update(browser, isLoading, canGoBack, canGoForward);
				statusPanel.setIsInProgress(isLoading);

				if (!isLoading && !errorMsg.isEmpty()) {
					browser.loadURL(DataUri.create("text/html", errorMsg));
					errorMsg = "";
				}
			}

			@Override
			public void onLoadError(CefBrowser browser, CefFrame frame,
					ErrorCode errorCode, String errorText, String failedUrl)
			{
				if (errorCode != ErrorCode.ERR_NONE
						&& errorCode != ErrorCode.ERR_ABORTED) {
					errorMsg = "<html><head>";
					errorMsg += "<title>Error while loading</title>";
					errorMsg += "</head><body>";
					errorMsg += "<h1>" + errorCode + "</h1>";
					errorMsg += "<h3>Failed to load " + failedUrl + "</h3>";
					errorMsg += "<p>" + (errorText == null ? "" : errorText)
							+ "</p>";
					errorMsg += "</body></html>";
					browser.stopLoad();
				}
			}
		});

		// Create the browser.
		CefBrowser browser = client.createBrowser("http://www.duckduckgo.com",
				osrEnabled, transparentPaintingEnabled, null);
		setBrowser(browser);

		// Set up the UI for this example implementation.
		JPanel contentPanel = createContentPanel();
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		// Clear focus from the browser when the address field gains focus.
		controlPane.getAddressField().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e)
			{
				if (!browserFocus) {
					return;
				}
				browserFocus = false;
				KeyboardFocusManager.getCurrentKeyboardFocusManager()
						.clearGlobalFocusOwner();
				controlPane.getAddressField().requestFocus();
			}
		});

		// Clear focus from the address field when the browser gains focus.
		client.addFocusHandler(new CefFocusHandlerAdapter() {
			@Override
			public void onGotFocus(CefBrowser browser)
			{
				if (browserFocus) {
					return;
				}
				browserFocus = true;
				KeyboardFocusManager.getCurrentKeyboardFocusManager()
						.clearGlobalFocusOwner();
				browser.setFocus(true);
			}

			@Override
			public void onTakeFocus(CefBrowser browser, boolean next)
			{
				browserFocus = false;
			}
		});

		if (createImmediately) {
			browser.createImmediately();
		}

		// Add the browser to the UI.
		contentPanel.add(getBrowser().getUIComponent(), BorderLayout.CENTER);

		MenuBar menuBar = new MenuBar(this, browser, controlPane,
				downloadDialog, CefCookieManager.getGlobalManager(), test);

		if (test) {
			menuBar.addBookmark("Binding Test",
					"client://tests/binding_test.html");
			menuBar.addBookmark("Binding Test 2",
					"client://tests/binding_test2.html");
			menuBar.addBookmark("Download Test",
					"https://cef-builds.spotifycdn.com/index.html");
			menuBar.addBookmark("Login Test (username:pumpkin, password:pie)",
					"http://www.colostate.edu/~ric/protect/your.html");
			menuBar.addBookmark("Certificate-error Test",
					"https://www.k2go.de");
			menuBar.addBookmark("Resource-Handler Test", "http://www.foo.bar/");
			menuBar.addBookmark("Resource-Handler Set Error Test",
					"http://seterror.test/");
			menuBar.addBookmark("Scheme-Handler Test 1: (scheme \"client\")",
					"client://tests/handler.html");
			menuBar.addBookmark("Scheme-Handler Test 2: (scheme \"search\")",
					"search://do a barrel roll/");
			menuBar.addBookmark("Spellcheck Test",
					"client://tests/spellcheck.html");
			menuBar.addBookmark("LocalStorage Test",
					"client://tests/localstorage.html");
			menuBar.addBookmark("Transparency Test",
					"client://tests/transparency.html");
			menuBar.addBookmarkSeparator();
			menuBar.addBookmark("javachromiumembedded",
					"https://bitbucket.org/chromiumembedded/java-cef");
			menuBar.addBookmark("chromiumembedded",
					"https://bitbucket.org/chromiumembedded/cef");
		}
		setJMenuBar(menuBar);
	}

	private JPanel createContentPanel()
	{
		JPanel contentPanel = new JPanel(new BorderLayout());
		controlPane = new ControlPanel(getBrowser());
		statusPanel = new StatusPanel();
		contentPanel.add(controlPane, BorderLayout.NORTH);
		contentPanel.add(statusPanel, BorderLayout.SOUTH);
		return contentPanel;
	}

	public boolean isOsrEnabled()
	{
		return osrEnabled;
	}

	public boolean isTransparentPaintingEnabled()
	{
		return transparentPaintingEnabled;
	}
}

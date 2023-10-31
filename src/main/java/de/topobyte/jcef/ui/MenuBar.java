// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.cef.OS;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler.FileDialogMode;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.network.CefCookieManager;
import org.cef.network.CefRequest;

import de.topobyte.jcef.BrowserFrame;
import de.topobyte.jcef.MainFrame;
import de.topobyte.jcef.dialog.CookieManagerDialog;
import de.topobyte.jcef.dialog.DevToolsDialog;
import de.topobyte.jcef.dialog.DownloadDialog;
import de.topobyte.jcef.dialog.SearchDialog;
import de.topobyte.jcef.dialog.ShowTextDialog;
import de.topobyte.jcef.dialog.UrlRequestDialog;
import de.topobyte.jcef.util.DataUri;

@SuppressWarnings("serial")
public class MenuBar extends JMenuBar
{
	class SaveAs implements CefStringVisitor
	{
		private PrintWriter fileWriter;

		public SaveAs(String fName)
				throws FileNotFoundException, UnsupportedEncodingException
		{
			fileWriter = new PrintWriter(fName, "UTF-8");
		}

		@Override
		public void visit(String string)
		{
			fileWriter.write(string);
			fileWriter.close();
		}
	}

	private final MainFrame owner;
	private final CefBrowser browser;
	private String lastSelectedFile = "";
	private final JMenu bookmarkMenu;
	private final ControlPanel controlPane;
	private final DownloadDialog downloadDialog;
	private final CefCookieManager cookieManager;
	private boolean reparentPending = false;

	public MenuBar(MainFrame owner, CefBrowser browser,
			ControlPanel controlPane, DownloadDialog downloadDialog,
			CefCookieManager cookieManager)
	{
		this.owner = owner;
		this.browser = browser;
		this.controlPane = controlPane;
		this.downloadDialog = downloadDialog;
		this.cookieManager = cookieManager;

		setEnabled(browser != null);

		JMenu fileMenu = new JMenu("File");

		JMenuItem openFileItem = new JMenuItem("Open file...");
		openFileItem.addActionListener(e -> {
			JFileChooser fc = new JFileChooser(new File(lastSelectedFile));
			// Show open dialog; this method does not return until the
			// dialog is closed.
			fc.showOpenDialog(owner);
			File selectedFile = fc.getSelectedFile();
			if (selectedFile != null) {
				lastSelectedFile = selectedFile.getAbsolutePath();
				browser.loadURL("file:///" + selectedFile.getAbsolutePath());
			}
		});
		fileMenu.add(openFileItem);

		JMenuItem openFileDialog = new JMenuItem("Save as...");
		openFileDialog.addActionListener(e -> {
			CefRunFileDialogCallback callback = new CefRunFileDialogCallback() {
				@Override
				public void onFileDialogDismissed(Vector<String> filePaths)
				{
					if (!filePaths.isEmpty()) {
						try {
							SaveAs saveContent = new SaveAs(filePaths.get(0));
							browser.getSource(saveContent);
						} catch (FileNotFoundException
								| UnsupportedEncodingException e) {
							browser.executeJavaScript(
									"alert(\"Can't save file\");",
									controlPane.getAddress(), 0);
						}
					}
				}
			};
			browser.runFileDialog(FileDialogMode.FILE_DIALOG_SAVE,
					owner.getTitle(), "index.html", null, 0, callback);
		});
		fileMenu.add(openFileDialog);

		JMenuItem printItem = new JMenuItem("Print...");
		printItem.addActionListener(e -> {
			browser.print();
		});
		fileMenu.add(printItem);

		JMenuItem printToPdfItem = new JMenuItem("Print to PDF");
		printToPdfItem.addActionListener(e -> {
			JFileChooser fc = new JFileChooser();
			fc.showSaveDialog(owner);
			File selectedFile = fc.getSelectedFile();
			if (selectedFile != null) {
				CefPdfPrintSettings pdfSettings = new CefPdfPrintSettings();
				pdfSettings.display_header_footer = true;
				// letter page size
				pdfSettings.paper_width = 8.5;
				pdfSettings.paper_height = 11;
				browser.printToPDF(selectedFile.getAbsolutePath(), pdfSettings,
						new CefPdfPrintCallback() {
							@Override
							public void onPdfPrintFinished(String path,
									boolean ok)
							{
								SwingUtilities.invokeLater(() -> {
									if (ok) {
										JOptionPane.showMessageDialog(owner,
												"PDF saved to " + path,
												"Success",
												JOptionPane.INFORMATION_MESSAGE);
									} else {
										JOptionPane.showMessageDialog(owner,
												"PDF failed", "Failed",
												JOptionPane.ERROR_MESSAGE);
									}
								});
							}
						});
			}
		});
		fileMenu.add(printToPdfItem);

		JMenuItem searchItem = new JMenuItem("Search...");
		searchItem.addActionListener(e -> {
			new SearchDialog(owner, browser).setVisible(true);
		});
		fileMenu.add(searchItem);

		fileMenu.addSeparator();

		JMenuItem viewSource = new JMenuItem("View source");
		viewSource.addActionListener(e -> {
			browser.viewSource();
		});
		fileMenu.add(viewSource);

		JMenuItem getSource = new JMenuItem("Get source...");
		getSource.addActionListener(e -> {
			ShowTextDialog visitor = new ShowTextDialog(owner,
					"Source of \"" + controlPane.getAddress() + "\"");
			browser.getSource(visitor);
		});
		fileMenu.add(getSource);

		JMenuItem getText = new JMenuItem("Get text...");
		getText.addActionListener(e -> {
			ShowTextDialog visitor = new ShowTextDialog(owner,
					"Content of \"" + controlPane.getAddress() + "\"");
			browser.getText(visitor);
		});
		fileMenu.add(getText);

		fileMenu.addSeparator();

		JMenuItem showDownloads = new JMenuItem("Show Downloads");
		showDownloads.addActionListener(e -> {
			downloadDialog.setVisible(true);
		});
		fileMenu.add(showDownloads);

		JMenuItem showCookies = new JMenuItem("Show Cookies");
		showCookies.addActionListener(e -> {
			CookieManagerDialog cookieManagerDialog = new CookieManagerDialog(
					owner, "Cookie Manager", cookieManager);
			cookieManagerDialog.setVisible(true);
		});
		fileMenu.add(showCookies);

		fileMenu.addSeparator();

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(e -> {
			owner.dispatchEvent(
					new WindowEvent(owner, WindowEvent.WINDOW_CLOSING));
		});
		fileMenu.add(exitItem);

		bookmarkMenu = new JMenu("Bookmarks");

		JMenuItem addBookmarkItem = new JMenuItem("Add bookmark");
		addBookmarkItem.addActionListener(e -> {
			addBookmark(owner.getTitle(), controlPane.getAddress());
		});
		bookmarkMenu.add(addBookmarkItem);
		bookmarkMenu.addSeparator();

		JMenu testMenu = new JMenu("Tests");

		JMenuItem testJSItem = new JMenuItem("JavaScript alert");
		testJSItem.addActionListener(e -> {
			browser.executeJavaScript("alert('Hello World');",
					controlPane.getAddress(), 1);
		});
		testMenu.add(testJSItem);

		JMenuItem jsAlertItem = new JMenuItem(
				"JavaScript alert (will be suppressed)");
		jsAlertItem.addActionListener(e -> {
			browser.executeJavaScript("alert('Never displayed');",
					"http://dontshow.me", 1);
		});
		testMenu.add(jsAlertItem);

		JMenuItem testShowText = new JMenuItem("Show Text");
		testShowText.addActionListener(e -> {
			browser.loadURL(DataUri.create("text/html",
					"<html><body><h1>Hello World</h1></body></html>"));
		});
		testMenu.add(testShowText);

		JMenuItem showForm = new JMenuItem("RequestHandler Test");
		showForm.addActionListener(e -> {
			String form = "<html><head><title>RequestHandler test</title></head>";
			form += "<body><h1>RequestHandler test</h1>";
			form += "<form action=\"http://www.google.com/\" method=\"post\">";
			form += "<input type=\"text\" name=\"searchFor\"/>";
			form += "<input type=\"submit\"/><br/>";
			form += "<input type=\"checkbox\" name=\"sendAsGet\"> Use GET instead of POST";
			form += "<p>This form tries to send the content of the text field as HTTP-POST request to http://www.google.com.</p>";
			form += "<h2>Testcase 1</h2>";
			form += "Try to enter the word <b>\"ignore\"</b> into the text field and press \"submit\".<br />";
			form += "The request will be rejected by the application.";
			form += "<p>See implementation of <u>tests.RequestHandler.onBeforeBrowse(CefBrowser, CefRequest, boolean)</u> for details</p>";
			form += "<h2>Testcase 2</h2>";
			form += "Due Google doesn't allow the POST method, the server replies with a 405 error.</br>";
			form += "If you activate the checkbox \"Use GET instead of POST\", the application will change the POST request into a GET request.";
			form += "<p>See implementation of <u>tests.RequestHandler.onBeforeResourceLoad(CefBrowser, CefRequest)</u> for details</p>";
			form += "</form>";
			form += "</body></html>";
			browser.loadURL(DataUri.create("text/html", form));
		});
		testMenu.add(showForm);

		JMenuItem httpRequest = new JMenuItem("Manual HTTP request");
		httpRequest.addActionListener(e -> {
			String searchFor = JOptionPane.showInputDialog(owner,
					"Search on google:");
			if (searchFor != null && !searchFor.isEmpty()) {
				CefRequest myRequest = CefRequest.create();
				myRequest.setMethod("GET");
				myRequest.setURL("http://www.google.com/#q=" + searchFor);
				myRequest.setFirstPartyForCookies(
						"http://www.google.com/#q=" + searchFor);
				browser.loadRequest(myRequest);
			}
		});
		testMenu.add(httpRequest);

		JMenuItem showInfo = new JMenuItem("Show Info");
		showInfo.addActionListener(e -> {
			String info = "<html><head><title>Browser status</title></head>";
			info += "<body><h1>Browser status</h1><table border=\"0\">";
			info += "<tr><td>CanGoBack</td><td>" + browser.canGoBack()
					+ "</td></tr>";
			info += "<tr><td>CanGoForward</td><td>" + browser.canGoForward()
					+ "</td></tr>";
			info += "<tr><td>IsLoading</td><td>" + browser.isLoading()
					+ "</td></tr>";
			info += "<tr><td>isPopup</td><td>" + browser.isPopup()
					+ "</td></tr>";
			info += "<tr><td>hasDocument</td><td>" + browser.hasDocument()
					+ "</td></tr>";
			info += "<tr><td>Url</td><td>" + browser.getURL() + "</td></tr>";
			info += "<tr><td>Zoom-Level</td><td>" + browser.getZoomLevel()
					+ "</td></tr>";
			info += "</table></body></html>";
			String js = "var x=window.open(); x.document.open(); x.document.write('"
					+ info + "'); x.document.close();";
			browser.executeJavaScript(js, "", 0);
		});
		testMenu.add(showInfo);

		final JMenuItem showDevTools = new JMenuItem("Show DevTools");
		showDevTools.addActionListener(e -> {
			DevToolsDialog devToolsDlg = new DevToolsDialog(owner, "DEV Tools",
					browser);
			devToolsDlg.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e)
				{
					showDevTools.setEnabled(true);
				}
			});
			devToolsDlg.setVisible(true);
			showDevTools.setEnabled(false);
		});
		testMenu.add(showDevTools);

		JMenuItem testURLRequest = new JMenuItem("URL Request");
		testURLRequest.addActionListener(e -> {
			UrlRequestDialog dlg = new UrlRequestDialog(owner,
					"URL Request Test");
			dlg.setVisible(true);
		});
		testMenu.add(testURLRequest);

		JMenuItem reparent = new JMenuItem("Reparent");
		reparent.addActionListener(e1 -> {
			final BrowserFrame newFrame = new BrowserFrame("New Window");
			newFrame.setLayout(new BorderLayout());
			final JButton reparentButton = new JButton("Reparent <");
			reparentButton.addActionListener(e2 -> {
				if (reparentPending) {
					return;
				}
				reparentPending = true;

				if (reparentButton.getText().equals("Reparent <")) {
					owner.removeBrowser(() -> {
						newFrame.add(browser.getUIComponent(),
								BorderLayout.CENTER);
						newFrame.setBrowser(browser);
						reparentButton.setText("Reparent >");
						reparentPending = false;
					});
				} else {
					newFrame.removeBrowser(() -> {
						JRootPane rootPane = (JRootPane) owner.getComponent(0);
						Container container = rootPane.getContentPane();
						JPanel panel = (JPanel) container.getComponent(0);
						panel.add(browser.getUIComponent());
						owner.setBrowser(browser);
						owner.revalidate();
						reparentButton.setText("Reparent <");
						reparentPending = false;
					});
				}
			});
			newFrame.add(reparentButton, BorderLayout.NORTH);
			newFrame.setSize(400, 400);
			newFrame.setVisible(true);
		});
		testMenu.add(reparent);

		JMenuItem newwindow = new JMenuItem("New window");
		newwindow.addActionListener(e -> {
			final MainFrame frame = new MainFrame(MainFrame.cefApp,
					OS.isLinux(), false, false, null);
			frame.setSize(800, 600);
			frame.setVisible(true);
		});
		testMenu.add(newwindow);

		JMenuItem screenshotSync = new JMenuItem(
				"Screenshot (on AWT thread, native res)");
		screenshotSync.addActionListener(e -> {
			long start = System.nanoTime();
			CompletableFuture<BufferedImage> shot = browser
					.createScreenshot(true);
			System.out.println("Took screenshot from the AWT event thread in "
					+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
					+ " msecs");
			try {
				displayScreenshot(shot.get());
			} catch (InterruptedException | ExecutionException exc) {
				// cannot happen, future is already resolved in this case
			}
		});
		screenshotSync.setEnabled(owner.isOsrEnabled());
		testMenu.add(screenshotSync);

		JMenuItem screenshotSyncScaled = new JMenuItem(
				"Screenshot (on AWT thread, scaled)");
		screenshotSyncScaled.addActionListener(e -> {
			long start = System.nanoTime();
			CompletableFuture<BufferedImage> shot = browser
					.createScreenshot(false);
			System.out.println("Took screenshot from the AWT event thread in "
					+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
					+ " msecs");
			try {
				displayScreenshot(shot.get());
			} catch (InterruptedException | ExecutionException exc) {
				// cannot happen, future is already resolved in this case
			}
		});
		screenshotSyncScaled.setEnabled(owner.isOsrEnabled());
		testMenu.add(screenshotSyncScaled);

		JMenuItem screenshotAsync = new JMenuItem(
				"Screenshot (from other thread, scaled)");
		screenshotAsync.addActionListener(e -> {
			long start = System.nanoTime();
			CompletableFuture<BufferedImage> shot = browser
					.createScreenshot(false);
			shot.thenAccept((image) -> {
				System.out
						.println(
								"Took screenshot asynchronously in "
										+ TimeUnit.NANOSECONDS.toMillis(
												System.nanoTime() - start)
										+ " msecs");
				SwingUtilities.invokeLater(() -> {
					displayScreenshot(image);
				});
			});
		});
		screenshotAsync.setEnabled(owner.isOsrEnabled());
		testMenu.add(screenshotAsync);

		add(fileMenu);
		add(bookmarkMenu);
		add(testMenu);
	}

	public void addBookmark(String name, String URL)
	{
		if (bookmarkMenu == null) {
			return;
		}

		// Test if the bookmark already exists. If yes, update URL
		Component[] entries = bookmarkMenu.getMenuComponents();
		for (Component itemEntry : entries) {
			if (!(itemEntry instanceof JMenuItem)) {
				continue;
			}

			JMenuItem item = (JMenuItem) itemEntry;
			if (item.getText().equals(name)) {
				item.setActionCommand(URL);
				return;
			}
		}

		JMenuItem menuItem = new JMenuItem(name);
		menuItem.setActionCommand(URL);
		menuItem.addActionListener(e -> {
			browser.loadURL(e.getActionCommand());
		});
		bookmarkMenu.add(menuItem);
		validate();
	}

	private void displayScreenshot(BufferedImage aScreenshot)
	{
		JFrame frame = new JFrame("Screenshot");
		ImageIcon image = new ImageIcon();
		image.setImage(aScreenshot);
		frame.setLayout(new FlowLayout());
		JLabel label = new JLabel(image);
		label.setPreferredSize(
				new Dimension(aScreenshot.getWidth(), aScreenshot.getHeight()));
		frame.add(label);
		frame.setVisible(true);
		frame.pack();
	}

	public void addBookmarkSeparator()
	{
		bookmarkMenu.addSeparator();
	}
}

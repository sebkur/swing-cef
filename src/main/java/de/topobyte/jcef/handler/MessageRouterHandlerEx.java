// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.handler;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class MessageRouterHandlerEx extends CefMessageRouterHandlerAdapter
{
	private final CefClient client;
	private final CefMessageRouterConfig config = new CefMessageRouterConfig(
			"myQuery", "myQueryAbort");
	private CefMessageRouter router = null;

	public MessageRouterHandlerEx(final CefClient client)
	{
		this.client = client;
	}

	@Override
	public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
			String request, boolean persistent, CefQueryCallback callback)
	{
		if (request.startsWith("hasExtension")) {
			if (router != null) {
				callback.success("");
			} else {
				callback.failure(0, "");
			}
		} else if (request.startsWith("enableExt")) {
			if (router != null) {
				callback.failure(-1, "Already enabled");
			} else {
				router = CefMessageRouter.create(config,
						new JavaVersionMessageRouter());
				client.addMessageRouter(router);
				callback.success("");
			}
		} else if (request.startsWith("disableExt")) {
			if (router == null) {
				callback.failure(-2, "Already disabled");
			} else {
				client.removeMessageRouter(router);
				router.dispose();
				router = null;
				callback.success("");
			}
		} else {
			// not handled
			return false;
		}
		return true;
	}

	private class JavaVersionMessageRouter
			extends CefMessageRouterHandlerAdapter
	{
		@Override
		public boolean onQuery(CefBrowser browser, CefFrame frame,
				long queryId, String request, boolean persistent,
				CefQueryCallback callback)
		{
			if (request.startsWith("jcefJava")) {
				callback.success(System.getProperty("java.version"));
				return true;
			}
			return false;
		};
	}
}

// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.handler;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;

public class JSDialogHandler extends CefJSDialogHandlerAdapter
{
	@Override
	public boolean onJSDialog(CefBrowser browser, String originUrl,
			JSDialogType dialogType, String messageText,
			String defaultPromptText, CefJSDialogCallback callback,
			BoolRef suppressMessage)
	{
		if (messageText.equalsIgnoreCase("Never displayed")) {
			suppressMessage.set(true);
			System.out.println("The " + dialogType + " from origin \""
					+ originUrl + "\" was suppressed.");
			System.out.println("   The content of the suppressed dialog was: \""
					+ messageText + "\"");
		}
		return false;
	}
}

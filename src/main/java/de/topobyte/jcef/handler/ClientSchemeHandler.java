// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package de.topobyte.jcef.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

/**
 * The example for the second scheme with domain handling is a more complex
 * example and is taken from the parent project CEF. Please see CEF:
 * "cefclient/scheme_test.cpp" for futher details
 */
public class ClientSchemeHandler extends CefResourceHandlerAdapter
{
	public static final String scheme = "client";
	public static final String domain = "tests";

	private byte[] data;
	private String mimeType;
	private int offset = 0;

	public ClientSchemeHandler()
	{
		super();
	}

	@Override
	public synchronized boolean processRequest(CefRequest request,
			CefCallback callback)
	{
		boolean handled = false;
		String url = request.getURL();
		if (url.indexOf("handler.html") != -1) {
			// Build the response html
			String html;
			html = "<html><head><title>Client Scheme Handler</title></head>"
					+ "<body bgcolor=\"white\">"
					+ "This contents of this page page are served by the "
					+ "ClientSchemeHandler class handling the client:// protocol."
					+ "<br/>You should see an image:"
					+ "<br/><img src=\"client://tests/logo.png\"><pre>";

			// Output a string representation of the request
			html += request.toString();

			html += "</pre><br/>Try the test form:"
					+ "<form method=\"POST\" action=\"handler.html\">"
					+ "<input type=\"text\" name=\"field1\">"
					+ "<input type=\"text\" name=\"field2\">"
					+ "<input type=\"submit\">" + "</form></body></html>";

			data = html.getBytes();

			handled = true;
			// Set the resulting mime type
			mimeType = "text/html";
		} else if (url.endsWith(".png")) {
			handled = loadContent(url.substring(url.lastIndexOf('/') + 1));
			mimeType = "image/png";
		} else if (url.endsWith(".html")) {
			handled = loadContent(url.substring(url.lastIndexOf('/') + 1));
			mimeType = "text/html";
			if (!handled) {
				String html = "<html><head><title>Error 404</title></head>";
				html += "<body><h1>Error 404</h1>";
				html += "File  " + url.substring(url.lastIndexOf('/') + 1)
						+ " ";
				html += "does not exist</body></html>";
				data = html.getBytes();
				handled = true;
			}
		}

		if (handled) {
			// Indicate the headers are available.
			callback.Continue();
			return true;
		}

		return false;
	}

	@Override
	public void getResponseHeaders(CefResponse response, IntRef responseLength,
			StringRef redirectUrl)
	{
		response.setMimeType(mimeType);
		response.setStatus(200);

		// Set the resulting response length
		responseLength.set(data.length);
	}

	@Override
	public synchronized boolean readResponse(byte[] dataOut, int bytesToRead,
			IntRef bytesRead, CefCallback callback)
	{
		boolean hasData = false;

		if (offset < data.length) {
			// Copy the next block of data into the buffer.
			int transferSize = Math.min(bytesToRead,
					(data.length - offset));
			System.arraycopy(data, offset, dataOut, 0, transferSize);
			offset += transferSize;

			bytesRead.set(transferSize);
			hasData = true;
		} else {
			offset = 0;
			bytesRead.set(0);
		}

		return hasData;
	}

	private boolean loadContent(String resName)
	{
		InputStream inStream = getClass().getResourceAsStream(resName);
		if (inStream != null) {
			try {
				ByteArrayOutputStream outFile = new ByteArrayOutputStream();
				int readByte = -1;
				while ((readByte = inStream.read()) >= 0) {
					outFile.write(readByte);
				}
				data = outFile.toByteArray();
				return true;
			} catch (IOException e) {
			}
		}
		return false;
	}
}

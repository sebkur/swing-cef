package de.topobyte.jcef.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JTextField;

// See https://stackoverflow.com/a/24571681/1268759
public class HintTextField extends JTextField
{

	private static final long serialVersionUID = -8671980886216131130L;

	private final String hint;

	public HintTextField(int columns, String hint)
	{
		super(columns);
		this.hint = hint;
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		if (getText().length() == 0) {
			int h = getHeight();
			((Graphics2D) g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			Insets ins = getInsets();
			FontMetrics fm = g.getFontMetrics();
			int c0 = getBackground().getRGB();
			int c1 = getForeground().getRGB();
			int m = 0xfefefefe;
			int c2 = ((c0 & m) >>> 1) + ((c1 & m) >>> 1);
			g.setColor(new Color(c2, true));
			g.drawString(hint, ins.left, h / 2 + fm.getAscent() / 2 - 2);
		}
	}

}
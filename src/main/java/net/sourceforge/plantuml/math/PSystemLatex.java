/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2024, Arnaud Roques
 *
 * Project Info:  https://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * https://plantuml.com/patreon (only 1$ per month!)
 * https://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 * 
 *
 */
package net.sourceforge.plantuml.math;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.plantuml.AbstractPSystem;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.core.DiagramDescription;
import net.sourceforge.plantuml.core.ImageData;
import net.sourceforge.plantuml.core.UmlSource;
import net.sourceforge.plantuml.klimt.color.ColorMapper;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.color.HColorSet;
import net.sourceforge.plantuml.log.Logme;
import net.sourceforge.plantuml.preproc.PreprocessingArtifact;

public class PSystemLatex extends AbstractPSystem {

	private String latex = "";
	private float scale = 1;
	private Color color = Color.BLACK;
	private Color backColor = Color.WHITE;

	public PSystemLatex(UmlSource source, PreprocessingArtifact preprocessing) {
		super(source, preprocessing);
	}

	public DiagramDescription getDescription() {
		return new DiagramDescription("(Latex)");
	}

	@Override
	final protected ImageData exportDiagramNow(OutputStream os, int num, FileFormatOption fileFormat)
			throws IOException {
		final ScientificEquationSafe asciiMath = ScientificEquationSafe.fromLatex(latex);
		return asciiMath.export(os, fileFormat, scale, color, backColor);
	}

	public void doCommandLine(String line) {
		final String lineLower = StringUtils.trin(StringUtils.goLowerCase(line));
		final String colorParam = "color ";
		final String backParam = "backgroundcolor ";
		if (lineLower.startsWith(colorParam)) {
			final Color col3 = getColor(line.substring(colorParam.length()));
			if (col3 != null) {
				color = col3;
			}
		} else if (lineLower.startsWith(backParam)) {
			final Color col3 = getColor(line.substring(backParam.length()));
			if (col3 != null) {
				backColor = col3;
			}
		} else if (lineLower.startsWith("scale ")) {
			final String value = line.substring("scale ".length());
			try {
				final float scale1 = Float.parseFloat(value);
				if (scale1 > 0) {
					scale = scale1;
				}
			} catch (Exception e) {
				Logme.error(e);
			}
		} else if (lineLower.startsWith("dpi ")) {
			final String value = line.substring("dpi ".length());
			try {
				final float dpi1 = Float.parseFloat(value);
				if (dpi1 > 0) {
					scale = dpi1 / 96;
				}
			} catch (Exception e) {
				Logme.error(e);
			}
		} else {
			this.latex = line;
		}
	}

	private Color getColor(final String col) {
		final HColor col2 = col == null ? null : HColorSet.instance().getColorOrWhite(col);
		final Color col3 = col2.toColor(ColorMapper.IDENTITY);
		return col3;
	}

}

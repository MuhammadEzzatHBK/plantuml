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
package net.sourceforge.plantuml.style;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import net.sourceforge.plantuml.klimt.Fashion;
import net.sourceforge.plantuml.klimt.LineBreakStrategy;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.Colors;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.color.HColorSet;
import net.sourceforge.plantuml.klimt.color.HColors;
import net.sourceforge.plantuml.klimt.creole.CreoleMode;
import net.sourceforge.plantuml.klimt.creole.Display;
import net.sourceforge.plantuml.klimt.drawing.UGraphic;
import net.sourceforge.plantuml.klimt.font.FontConfiguration;
import net.sourceforge.plantuml.klimt.font.FontStack;
import net.sourceforge.plantuml.klimt.font.UFont;
import net.sourceforge.plantuml.klimt.geom.HorizontalAlignment;
import net.sourceforge.plantuml.klimt.shape.TextBlock;
import net.sourceforge.plantuml.klimt.shape.TextBlockUtils;

public class Style {
	// ::remove file when __HAXE__

	private final Map<PName, Value> map;
	private final StyleSignatureBasic signature;

	public Style(StyleSignatureBasic signature, Map<PName, Value> map) {
		this.map = map;
		this.signature = signature;
	}

	public Style deltaPriority(int delta) {
		if (signature.isStarred() == false)
			throw new UnsupportedOperationException();

		final EnumMap<PName, Value> copy = new EnumMap<PName, Value>(PName.class);
		for (Entry<PName, Value> ent : this.map.entrySet())
			copy.put(ent.getKey(), ((ValueImpl) ent.getValue()).addPriority(delta));

		return new Style(this.signature, copy);

	}

	public void printMe() {
		if (map.size() == 0)
			return;

		System.err.println(signature + " {");
		for (Entry<PName, Value> ent : map.entrySet())
			System.err.println("  " + ent.getKey() + ": " + ent.getValue().asString());

		System.err.println("}");

	}

	@Override
	public String toString() {
		return signature + " " + map;
	}

	public Value value(PName name) {
		final Value result = map.get(name);
		if (result == null)
			return ValueNull.NULL;

		return result;
	}

	public double getShadowing() {
		final Value result = map.get(PName.Shadowing);
		if (result == null)
			return 0;

		return value(PName.Shadowing).asDoubleDefaultTo(1.5);
	}

	public boolean hasValue(PName name) {
		return map.containsKey(name);
	}

	public Style mergeWith(Style other, MergeStrategy strategy) {
		if (other == null)
			return this;

		final EnumMap<PName, Value> both = new EnumMap<PName, Value>(this.map);
		for (Entry<PName, Value> ent : other.map.entrySet()) {
			final Value previous = this.map.get(ent.getKey());
			if (previous != null && previous.getPriority() > StyleLoader.DELTA_PRIORITY_FOR_STEREOTYPE
					&& strategy == MergeStrategy.KEEP_EXISTING_VALUE_OF_STEREOTYPE)
				continue;
			final PName key = ent.getKey();
			both.put(key, ((ValueImpl) ent.getValue()).mergeWith(previous));
		}
		return new Style(this.signature.mergeWith(other.getSignature()), both);
	}

	public Style eventuallyOverride(PName param, HColor color) {
		if (color == null)
			return this;

		final EnumMap<PName, Value> result = new EnumMap<PName, Value>(this.map);
		final Value old = result.get(param);
		result.put(param, new ValueColor(color, old.getPriority()));
		return new Style(this.signature, result);
	}

	public Style eventuallyOverride(PName param, double value) {
		return eventuallyOverride(param, "" + value);
	}

	public Style eventuallyOverride(PName param, String value) {
		final EnumMap<PName, Value> result = new EnumMap<PName, Value>(this.map);
		result.put(param, ValueImpl.regular(value, Integer.MAX_VALUE));
		return new Style(this.signature, result);
	}

	public Style eventuallyOverride(Colors colors) {
		Style result = this;
		if (colors != null) {
			final HColor back = colors.getColor(ColorType.BACK);
			if (back != null)
				result = result.eventuallyOverride(PName.BackGroundColor, back);

			final HColor line = colors.getColor(ColorType.LINE);
			if (line != null)
				result = result.eventuallyOverride(PName.LineColor, line);

			final HColor text = colors.getColor(ColorType.TEXT);
			if (text != null)
				result = result.eventuallyOverride(PName.FontColor, text);

		}
		return result;
	}

	public Style eventuallyOverride(Fashion symbolContext) {
		Style result = this;
		if (symbolContext != null) {
			final HColor back = symbolContext.getBackColor();
			if (back != null)
				result = result.eventuallyOverride(PName.BackGroundColor, back);

		}
		return result;
	}

	public StyleSignatureBasic getSignature() {
		return signature;
	}

	public UFont getUFont() {
		final String fontName = value(PName.FontName).asString();
		// final String family = FontStack.getExistingFontFamily(fontName);
		final int fontStyle = value(PName.FontStyle).asFontStyle();
		int size = value(PName.FontSize).asInt(true);
		if (size == -1)
			size = 14;
		return UFont.build(fontName, fontStyle, size);
	}

	public FontConfiguration getFontConfiguration(HColorSet set) {
		return getFontConfiguration(set, null);
	}

	public FontConfiguration getFontConfiguration(HColorSet set, Colors colors) {
		final UFont font = getUFont();
		HColor color = colors == null ? null : colors.getColor(ColorType.TEXT);
		if (color == null)
			color = value(PName.FontColor).asColor(set);

		final HColor hyperlinkColor = value(PName.HyperLinkColor).asColor(set);
		final UStroke stroke = getStroke(PName.HyperlinkUnderlineThickness, PName.HyperlinkUnderlineStyle);
		return FontConfiguration.create(font, color, hyperlinkColor, stroke);
	}

	public Fashion getSymbolContext(HColorSet set, Colors colors) {
		HColor backColor = colors == null ? null : colors.getColor(ColorType.BACK);
		if (backColor == null)
			backColor = value(PName.BackGroundColor).asColor(set);
		HColor foreColor = colors == null ? null : colors.getColor(ColorType.LINE);
		if (foreColor == null)
			foreColor = value(PName.LineColor).asColor(set);
		final double deltaShadowing = getShadowing();
		final double roundCorner = value(PName.RoundCorner).asDouble();
		final double diagonalCorner = value(PName.DiagonalCorner).asDouble();
		return new Fashion(backColor, foreColor).withStroke(getStroke()).withDeltaShadow(deltaShadowing)
				.withCorner(roundCorner, diagonalCorner);
	}

	public Fashion getSymbolContext(HColorSet set) {
		return getSymbolContext(set, null);
	}

	public Style eventuallyOverride(UStroke stroke) {
		if (stroke == null)
			return this;

		Style result = this.eventuallyOverride(PName.LineThickness, stroke.getThickness());
		final double space = stroke.getDashSpace();
		final double visible = stroke.getDashVisible();
		result = result.eventuallyOverride(PName.LineStyle, "" + visible + "-" + space);
		return result;
	}

	public UStroke getStroke() {
		return getStroke(PName.LineThickness, PName.LineStyle);
	}

	private UStroke getStroke(final PName thicknessParam, final PName styleParam) {
		final double thickness = value(thicknessParam).asDouble();
		final String dash = value(styleParam).asString();
		if (dash.length() == 0)
			return UStroke.withThickness(thickness);

		try {
			final StringTokenizer st = new StringTokenizer(dash, "-;,");
			final double dashVisible = Double.parseDouble(st.nextToken().trim());
			double dashSpace = dashVisible;
			if (st.hasMoreTokens())
				dashSpace = Double.parseDouble(st.nextToken().trim());

			return new UStroke(dashVisible, dashSpace, thickness);
		} catch (Exception e) {
			return UStroke.withThickness(thickness);
		}
	}

	public UStroke getStroke(Colors colors) {
		final UStroke stroke = colors.getSpecificLineStroke();
		if (stroke == null)
			return getStroke();

		return stroke;
	}

	public LineBreakStrategy wrapWidth() {
		final String value = value(PName.MaximumWidth).asString();
		return new LineBreakStrategy(value);
	}

	public ClockwiseTopRightBottomLeft getPadding() {
		final String padding = value(PName.Padding).asString();
		return ClockwiseTopRightBottomLeft.read(padding);
	}

	public ClockwiseTopRightBottomLeft getMargin() {
		final String margin = value(PName.Margin).asString();
		return ClockwiseTopRightBottomLeft.read(margin);
	}

	public HorizontalAlignment getHorizontalAlignment() {
		return value(PName.HorizontalAlignment).asHorizontalAlignment();
	}

	public static final String ID_TITLE = "_title";
	public static final String ID_CAPTION = "_caption";
	public static final String ID_LEGEND = "_legend";

	public TextBlock createTextBlockBordered(Display note, HColorSet set, ISkinSimple spriteContainer, String id,
			LineBreakStrategy lineBreak) {
		final HorizontalAlignment alignment = this.getHorizontalAlignment();
		final FontConfiguration fc = this.getFontConfiguration(set);

		final TextBlock textBlock = note.create0(fc, alignment, spriteContainer, lineBreak, CreoleMode.FULL, null,
				null);

		final HColor backgroundColor = this.value(PName.BackGroundColor).asColor(set);
		final HColor lineColor = this.value(PName.LineColor).asColor(set);
		final UStroke stroke = this.getStroke();
		final int cornersize = this.value(PName.RoundCorner).asInt(false);
		final ClockwiseTopRightBottomLeft margin = this.getMargin();
		final ClockwiseTopRightBottomLeft padding = this.getPadding();
		final TextBlock result = TextBlockUtils.bordered(textBlock, stroke, lineColor, backgroundColor, cornersize,
				padding, id);
		return TextBlockUtils.withMargin(result, margin);
	}

	public UGraphic applyStrokeAndLineColor(UGraphic ug, HColorSet colorSet) {
		final HColor color = value(PName.LineColor).asColor(colorSet);
		if (color == null)
			ug = ug.apply(HColors.none());
		else
			ug = ug.apply(color);

		ug = ug.apply(getStroke());
		return ug;
	}

}
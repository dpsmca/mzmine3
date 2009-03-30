/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.identification.complexsearch;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class ComplexSearchParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter rtTolerance = new SimpleParameter(
			ParameterType.DOUBLE,
			"Time tolerance",
			"Maximum allowed retention time difference to set a relationship between peaks",
			null, new Double(10.0), new Double(0.0), null, MZmineCore
					.getRTFormat());

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Tolerance value of the m/z difference between peaks", "m/z",
			new Double(0.1), new Double(0.0), null, MZmineCore.getMZFormat());
	
	public static final Parameter maxComplexHeight = new SimpleParameter(
			ParameterType.DOUBLE,
			"Max fragment peak height",
			"Maximum height of the recognized fragment peak, relative to the main peak",
			"%", new Double(0.20), new Double(0.0), null, percentFormat);

	public ComplexSearchParameters() {
		super(new Parameter[] { rtTolerance, mzTolerance, maxComplexHeight });
	}

}

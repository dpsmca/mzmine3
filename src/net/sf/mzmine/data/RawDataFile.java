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

package net.sf.mzmine.data;

import net.sf.mzmine.util.Range;

public interface RawDataFile {

	/**
	 * Returns the name of this data file (can be a descriptive name, not
	 * necessarily the original file name)
	 */
	public String getName();

	/**
	 * Change the name of this data file
	 */
	public void setName(String name);

	public String getPath();

	public int getNumOfScans();

	public int getNumOfScans(int msLevel);

	/**
	 * Returns sorted array of all MS levels in this file
	 */
	public int[] getMSLevels();

	/**
	 * Returns sorted array of all scan numbers in this file
	 * 
	 * @return Sorted array of scan numbers, never returns null
	 */
	public int[] getScanNumbers();

	/**
	 * Returns sorted array of all scan numbers in given MS level
	 * 
	 * @param msLevel
	 *            MS level
	 * @return Sorted array of scan numbers, never returns null
	 */
	public int[] getScanNumbers(int msLevel);

	/**
	 * Returns sorted array of all scan numbers in given MS level and retention
	 * time range
	 * 
	 * @param msLevel
	 *            MS level
	 * @param rtRange
	 *            Retention time range
	 * @return Sorted array of scan numbers, never returns null
	 */
	public int[] getScanNumbers(int msLevel, Range rtRange);

	/**
	 * 
	 * @param scan
	 *            Desired scan number
	 * @return Desired scan
	 */
	public Scan getScan(int scan);

	public Range getDataMZRange();

	public Range getDataRTRange();

	public Range getDataMZRange(int msLevel);

	public Range getDataRTRange(int msLevel);

	public double getDataMaxBasePeakIntensity(int msLevel);

	public double getDataMaxTotalIonCurrent(int msLevel);

	/**
	 * Close the file in case it is removed from the project
	 */
	public void close();

}

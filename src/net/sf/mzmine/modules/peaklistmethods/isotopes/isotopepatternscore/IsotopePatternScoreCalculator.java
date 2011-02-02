/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore;

import java.util.ArrayList;
import java.util.Arrays;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class IsotopePatternScoreCalculator {

	/**
	 * Returns a calculated similarity score of two isotope patterns in the
	 * range of 0 (not similar at all) to 1 (100% same).
	 */
	public static double getSimilarityScore(IsotopePattern ip1,
			IsotopePattern ip2, double mzTolerance) {

		assert ip1 != null;
		assert ip2 != null;

		// Normalize the isotopes to intensity 0..1
		IsotopePattern nip1 = IsotopePatternCalculator
				.normalizeIsotopePattern(ip1);
		IsotopePattern nip2 = IsotopePatternCalculator
				.normalizeIsotopePattern(ip2);

		// Merge the data points from both isotope patterns into a single array.
		// Data points from first pattern will have positive intensities, data
		// points from second pattern will have negative intensities.
		ArrayList<DataPoint> mergedDataPoints = new ArrayList<DataPoint>();
		for (DataPoint dp : nip1.getDataPoints())
			mergedDataPoints.add(dp);
		for (DataPoint dp : nip2.getDataPoints()) {
			DataPoint negativeDP = new SimpleDataPoint(dp.getMZ(),
					dp.getIntensity() * -1);
			mergedDataPoints.add(negativeDP);
		}
		DataPoint mergedDPArray[] = mergedDataPoints.toArray(new DataPoint[0]);

		// Sort the merged data points by m/z
		Arrays.sort(mergedDPArray, new DataPointSorter(SortingProperty.MZ,
				SortingDirection.Ascending));

		// Iterate the merged data points and sum all isotopes within m/z
		// tolerance
		for (int i = 0; i < mergedDPArray.length - 1; i++) {

			double mzDifference = Math.abs(mergedDPArray[i].getMZ()
					- mergedDPArray[i + 1].getMZ());

			if (mzDifference > mzTolerance)
				continue;

			double summedIntensity = mergedDPArray[i].getIntensity()
					+ mergedDPArray[i + 1].getIntensity();

			double newMZ = mergedDPArray[i + 1].getMZ();

			// Update the next data point and remove the current one
			mergedDPArray[i + 1] = new SimpleDataPoint(newMZ, summedIntensity);
			mergedDPArray[i] = null;

		}

		// Calculate the resulting score. Ideal score is 1, in case the final
		// data point array is empty.
		double result = 1;

		for (DataPoint dp : mergedDPArray) {
			if (dp == null)
				continue;
			double remainingIntensity = Math.abs(dp.getIntensity());

			// In case some large isotopes were grouped together, the summed
			// intensity may be over 1
			if (remainingIntensity > 1)
				remainingIntensity = 1;

			// Decrease the score with each remaining peak
			result *= 1 - remainingIntensity;
		}

		return result;
	}

}

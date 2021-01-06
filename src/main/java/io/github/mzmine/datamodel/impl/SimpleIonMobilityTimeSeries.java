/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IonMobilityTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Used to store ion mobility-LC-MS data
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilityTimeSeries implements IonMobilityTimeSeries {

  private static final Logger logger = Logger.getLogger(SimpleMsTimeSeries.class.getName());

  protected final List<SimpleIonMobilitySeries> simpleIonMobilitySeries;
  protected final List<Frame> frames;
  protected DoubleBuffer intensityValues;
  protected DoubleBuffer mzValues;

  public SimpleIonMobilityTimeSeries(@Nonnull MemoryMapStorage storage, @Nonnull List<SimpleIonMobilitySeries> simpleIonMobilitySeries) {

    List<Frame> tempFrames = new ArrayList<Frame>(simpleIonMobilitySeries.size());
    this.simpleIonMobilitySeries = simpleIonMobilitySeries;

    double[] summedIntensities = new double[simpleIonMobilitySeries.size()];
    double[] weightedMzs = new double[simpleIonMobilitySeries.size()];

    for (int i = 0; i < simpleIonMobilitySeries.size(); i++) {
      SimpleIonMobilitySeries ims = simpleIonMobilitySeries.get(i);
      tempFrames.add(ims.getScans().get(0).getFrame());

      DoubleBuffer intensities = ims.getIntensityValues();
      DoubleBuffer mzValues = ims.getMzValues();
      for(int j = 0; j < intensities.capacity(); j++) {
        summedIntensities[i] = intensities.get(i);
      }

      // calculate an intensity weighted average for mz
      // todo use CenterFunction maybe?
      double weightedMz = 0;
      for (int j = 0; j < mzValues.capacity(); j++) {
        weightedMz += mzValues.get(j) * (intensities.get(j) / summedIntensities[i]);
      }
      weightedMzs[i] = weightedMz;
    }

    try {
      intensityValues = storage.storeData(weightedMzs);
      mzValues = storage.storeData(weightedMzs);
    } catch (IOException e) {
      e.printStackTrace();
      intensityValues = DoubleBuffer.wrap(summedIntensities);
      mzValues = DoubleBuffer.wrap(weightedMzs);
    }

    frames = Collections.unmodifiableList(tempFrames);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  /**
   * @return The frames.
   */
  @Override
  public List<Frame> getScans() {
    return frames;
  }

}
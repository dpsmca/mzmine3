/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a spectrum based on multiple individual mass spectra. Compatible with the {@link Scan}
 * interface. {@link SimpleMergedMassSpectrum#getScanNumber()} will return -1 to represent
 * the artificial state of this spectrum.
 */
public class SimpleMergedMassSpectrum extends AbstractStorableSpectrum implements
    MergedMassSpectrum {

  private static final Logger logger = Logger.getLogger(SimpleMergedMsMsSpectrum.class.getName());

  protected final List<MassSpectrum> sourceSpectra;
  protected final MergingType mergingType;
  protected final CenterFunction centerFunction;
  protected final RawDataFile rawDataFile;
  protected final float retentionTime;
  protected final int msLevel;
  protected final Range<Double> scanningMzRange;
  protected final PolarityType polarity;
  protected String scanDefinition; // cannot be final due to subclasses
  protected MassList massList = null;

  /**
   * @param storage         The storage to use. may be null.
   * @param mzValues        The merged mz values
   * @param intensityValues The merged intensities
   * @param msLevel         The ms level
   * @param sourceSpectra   The source spectra used to create this spectrum
   * @param mergingType     The merging type this spectrum was created with.
   * @param centerFunction  The center function this spectrum was created with.
   */
  public SimpleMergedMassSpectrum(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, int msLevel,
      @Nonnull List<? extends MassSpectrum> sourceSpectra,
      @Nonnull MergingType mergingType, @Nonnull CenterFunction centerFunction) {
    super(storage, mzValues, intensityValues);

    assert !sourceSpectra.isEmpty();

    RawDataFile file = null;
    PolarityType tempPolarity = null;
    Range<Double> tempScanningMzRange = null;
    float tempRt = 0f;
    for (MassSpectrum spectrum : sourceSpectra) {
      if (file == null) {
        if (spectrum instanceof Scan) {
          file = ((Scan) spectrum).getDataFile();
          tempPolarity = ((Scan) spectrum).getPolarity();
          tempScanningMzRange = ((Scan) spectrum).getScanningMZRange();
          tempRt = ((Scan) spectrum).getRetentionTime();
        }
      }
      if (spectrum instanceof Scan && file != ((Scan) spectrum).getDataFile()) {
        logger.warning("Merging spectra with different raw data files");
      }
    }
    rawDataFile = file;

    this.retentionTime = tempRt;
    this.polarity = tempPolarity;
    this.scanningMzRange = tempScanningMzRange;
    this.sourceSpectra = (List<MassSpectrum>) sourceSpectra;
    this.mergingType = mergingType;
    this.centerFunction = centerFunction;
    this.msLevel = msLevel;
    this.scanDefinition = ScanUtils.scanToString(this, true);
  }

  @Override
  public List<MassSpectrum> getSourceSpectra() {
    return Collections.unmodifiableList(sourceSpectra);
  }

  @Override
  public MergingType getMergingType() {
    return mergingType;
  }

  @Override
  public CenterFunction getCenterFunction() {
    return centerFunction;
  }

  @Nonnull
  @Override
  public RawDataFile getDataFile() {
    return rawDataFile;
  }

  @Nonnull
  @Override
  public String getScanDefinition() {
    return scanDefinition;
  }

  @Override
  public int getMSLevel() {
    return msLevel;
  }

  @Override
  public float getRetentionTime() {
    return retentionTime;
  }

  @Nonnull
  @Override
  public Range<Double> getScanningMZRange() {
    return scanningMzRange;
  }

  @Override
  public double getPrecursorMZ() {
    return 0;
  }

  @Nonnull
  @Override
  public PolarityType getPolarity() {
    return polarity;
  }

  @Override
  public int getPrecursorCharge() {
    return 0;
  }

  @Nullable
  @Override
  public MassList getMassList() {
    return massList;
  }

  @Override
  public synchronized void addMassList(final @Nonnull MassList massList) {
    // we are not going into any details if this.massList equals massList
    // do not call listeners if the same object is passed multiple times
    if (this.massList == massList) {
      return;
    }
    MassList old = this.massList;
    this.massList = massList;

    if (rawDataFile != null) {
      rawDataFile.applyMassListChanged(this, old, massList);
    }
  }

}

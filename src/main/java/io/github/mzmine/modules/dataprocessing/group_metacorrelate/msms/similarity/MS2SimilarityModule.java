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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityParameters.Mode;
import java.util.Collection;
import javax.annotation.Nonnull;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class MS2SimilarityModule implements MZmineProcessingModule {

  private static final String NAME = "MS2 similarity";

  private static final String DESCRIPTION =
      "Checks MS2 similarity of all rows within the groups or on all networks and between networks";

  @Override
  public @Nonnull String getName() {
    return NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return MS2SimilarityParameters.class;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull final ParameterSet parameters,
                            @Nonnull final Collection<Task> tasks) {

    Mode mode = parameters.getParameter(MS2SimilarityParameters.MODE).getValue();
    ModularFeatureList[] featureLists = parameters.getParameter(MS2SimilarityParameters.PEAK_LISTS).getValue()
        .getMatchingFeatureLists();
    for (ModularFeatureList pkl : featureLists) {
      switch (mode) {
        case ION_NETWORKS -> {
          IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, true);
          tasks.add(new MS2SimilarityTask(parameters, pkl, nets));
        }
        case ALL_ROWS -> {
          tasks.add(new MS2SimilarityTask(parameters, pkl, pkl.getRows()));
        }
      }
    }
    return ExitCode.OK;
  }
}

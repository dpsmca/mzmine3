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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_gnps.fbmn;

import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.rowfilter.RowFilter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureMeasurementType;

public class GnpsFbmnExportAndSubmitParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Base name of the output files (.MGF and .CSV). "
          + "Use pattern \"{}\" in the file name to substitute with feature list name. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.",
      "mgf", FileSelectionType.SAVE);

  public static final OptionalModuleParameter<GnpsFbmnSubmitParameters> SUBMIT =
      new OptionalModuleParameter<>("Submit to GNPS",
          "Directly submits a GNPS job", new GnpsFbmnSubmitParameters());

  public static final ComboParameter<FeatureListRowsFilter> FILTER = new ComboParameter<>(
      "Filter rows", "Limit the exported rows to those with MS/MS data or annotated rows",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.ONLY_WITH_MS2);

  // public static final BooleanParameter OPEN_GNPS = new
  // BooleanParameter("Open GNPS website",
  // "Opens the super quick start of GNPS feature based networking in the
  // standard browser.",
  // false);

  public static final BooleanParameter OPEN_FOLDER =
      new BooleanParameter("Open folder", "Opens the export folder", false);

  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER =
      new OptionalModuleParameter<>("Merge MS/MS (experimental)",
          "Merge high-quality MS/MS instead of exporting just the most intense one.",
          new MsMsSpectraMergeParameters(), true);

  public static final ComboParameter<FeatureMeasurementType> FEATURE_INTENSITY =
      new ComboParameter<>("Feature intensity", "Intensity in the quantification table (csv).",
          FeatureMeasurementType.values(), FeatureMeasurementType.AREA);


  public GnpsFbmnExportAndSubmitParameters() {
    super(new Parameter[]{FEATURE_LISTS, FILENAME, MERGE_PARAMETER, FILTER, FEATURE_INTENSITY, SUBMIT,
        OPEN_FOLDER});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html><strong>About the GNPS Export/Submit Module:</strong>"
        + "<p>The GNPS Export module was designed for the <strong>Feature-Based Molecular Networking</strong> (FBMN) workflow on GNPS <a href=\"http://gnps.ucsd.edu\">http://gnps.ucsd.edu</a>.<br>"
        + "See the <a href=\"https://ccms-ucsd.github.io/GNPSDocumentation/featurebasedmolecularnetworking/\"><strong>FBMN documentation here</strong></a> (or a youtube <a href=\"https://www.youtube.com/watch?v=vFcGG7T_44E&list=PL4L2Xw5k8ITzd9hx5XIP94vFPxj1sSafB&index=4&t=146s\">playlist here</a>) and <strong>please cite</strong>:<br>"
        + "<ul>"
        + "<li>our preprint on <strong>FBMN</strong>: Nothias et al.: <a href=\"https://www.biorxiv.org/content/10.1101/812404v1\">bioRxiv 812404 (2019)</a>.</li>"
        + "<li>the <strong>GNPS</strong> paper: Wang et al.:<a href=\"https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html\">Nature Biotechnology 34.8 (2016): 828-837</a></li>"
        + "<li>and the <strong>MZmine</strong> paper: Pluskal et al.: <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">BMC Bioinformatics, 11, 395 (2010)</a></li>"
        + "</ul></p>";
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}

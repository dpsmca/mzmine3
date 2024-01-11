/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.export_rawdata_netcdf;

import io.github.msdk.MSDKMethod;
import io.github.msdk.io.mzml.MzMLFileExportMethod;
import io.github.msdk.io.mzml.data.MzMLCompressionType;
import io.github.msdk.io.netcdf.NetCDFFileExportMethod;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.MZmineToMSDKRawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class NetCDFExportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NetCDFExportTask.class.getName());
  private final RawDataFile dataFile;
  private final File outFilename;
  private MSDKMethod<?> msdkMethod = null;

  /**
   * @param dataFile
   * @param parameters
   */
  public NetCDFExportTask(RawDataFile dataFile, File outFilename,
      final @NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.dataFile = dataFile;
    this.outFilename = outFilename;
  }

  public String getTaskDescription() {
    return "Exporting file " + dataFile + " to " + outFilename;
  }

  public double getFinishedPercentage() {
    if ((msdkMethod == null) || (msdkMethod.getFinishedPercentage() == null)) {
      return 0;
    }
    return msdkMethod.getFinishedPercentage().doubleValue();
  }

  public void run() {
    try {
      if (isCanceled()) {
        return;
      }
      setStatus(TaskStatus.PROCESSING);

      logger.info("Started export of file " + dataFile + " to " + outFilename);

      MZmineToMSDKRawDataFile msdkDataFile = new MZmineToMSDKRawDataFile(dataFile);

      if (outFilename.getName().toLowerCase().endsWith("mzml")) {
        msdkMethod = new MzMLFileExportMethod(msdkDataFile, outFilename, MzMLCompressionType.ZLIB,
            MzMLCompressionType.ZLIB);
      }

      if (outFilename.getName().toLowerCase().endsWith("cdf")) {
        msdkMethod = new NetCDFFileExportMethod(msdkDataFile, outFilename);
      }
      msdkMethod.execute();

      setStatus(TaskStatus.FINISHED);

      logger.info("Finished export of file " + dataFile + " to " + outFilename);

    } catch (Exception e) {
      logger.log(Level.WARNING, "Error in netcdf export", e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error in file export: " + e.getMessage());
    }
  }

  @Override
  public void cancel() {
    super.cancel();
    if (msdkMethod != null) {
      msdkMethod.cancel();
    }
  }
}

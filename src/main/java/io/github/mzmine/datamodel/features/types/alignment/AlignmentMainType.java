/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.alignment;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main type that holds alignment scores
 */
public class AlignmentMainType extends DataType<AlignmentScores> implements SubColumnsFactory,
    AnnotationType {

  private static final Logger logger = Logger.getLogger(AlignmentMainType.class.getName());


  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "alignment_scores";
  }

  @NotNull
  public List<DataType> getSubDataTypes() {
    return AlignmentScores.subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Alignment";
  }

  @Override
  public Property<AlignmentScores> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof AlignmentScores scores)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }

    for (int i = 0; i < AlignmentScores.subTypes.size(); i++) {
      DataType sub = AlignmentScores.subTypes.get(i);
      Object subValue = getSubColValue(sub, scores);
      if (subValue != null) {
        writer.writeStartElement(CONST.XML_DATA_TYPE_ELEMENT);
        writer.writeAttribute(CONST.XML_DATA_TYPE_ID_ATTR, sub.getUniqueID());

        try { // catch here, so we can easily debug and don't destroy the flist while saving in case an unexpected exception happens
          sub.saveToXML(writer, subValue, flist, row, feature, file);
        } catch (XMLStreamException e) {
          final Object finalVal = subValue;
          logger.warning(() -> "Error while writing data type " + sub.getClass().getSimpleName()
              + " with value " + finalVal + " to xml.");
          e.printStackTrace();
        }

        writer.writeEndElement();
      }
    }
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    AlignmentScores scores = null;
    while (reader.hasNext()) {
      int next = reader.next();

      if (next == XMLEvent.END_ELEMENT && reader.getLocalName()
          .equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        break;
      }
      if (reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)) {
        DataType type = DataTypes.getTypeForId(
            reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR));
        Object o = type.loadFromXML(reader, project, flist, row, feature, file);
        if (scores == null) {
          scores = new AlignmentScores();
        }
        scores = scores.modify(type, o);
      }
    }
    return scores;
  }

  @Override
  public Class<AlignmentScores> getValueClass() {
    return AlignmentScores.class;
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      DataType type = subTypes.get(index);
      if (this.getClass().isInstance(type)) {
        // create a special column for this type that actually represents the list of data
        cols.add(DataType.createStandardColumn(type, raw, this, index));
      } else {
        // create all other columns
        var col = type.createColumn(raw, this, index);
        // override type in CellValueFactory with this parent type
        cols.add(col);
      }
    }
    return cols;
  }

  @Override
  public <T> void valueChanged(ModularDataModel model, DataType<T> subType, int subColumnIndex,
      T newValue) {
    try {
      AlignmentScores scores = Objects.requireNonNullElse(model.get(this), new AlignmentScores());
      scores = scores.modify(subType, newValue);
      // finally set annotation
      model.set(AlignmentMainType.class, scores);
    } catch (Exception ex) {
      logger.log(Level.WARNING, () -> String.format(
          "Cannot handle change in subtype %s at index %d in parent type %s with new value %s",
          subType.getClass().getName(), subColumnIndex, this.getClass().getName(), newValue));
    }
  }

  @Override
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Override
  public @Nullable String getHeader(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getHeaderString();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  public @Nullable String getUniqueID(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getUniqueID();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }


  @Override
  public @NotNull DataType<?> getType(int index) {
    if (index < 0 || index >= getSubDataTypes().size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index,
              getSubDataTypes().size()));
    }
    return getSubDataTypes().get(index);
  }


  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object cellData) {
    DataType sub = getType(subcolumn);
    return sub == null ? null : getSubColValue(sub, cellData);
  }

  @Override
  public @Nullable Object getSubColValue(DataType sub, Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof AlignmentScores scores) {
      return scores.getValue(sub);
    } else {
      throw new IllegalArgumentException(
          String.format("value of type %s needs to be of type AlignmentScores",
              value.getClass().getName()));
    }
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }
}

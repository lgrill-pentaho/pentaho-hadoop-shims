/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.hadoop.shim.common.format.parquet;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaBigNumber;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaDate;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.row.value.ValueMetaTimestamp;
import org.pentaho.di.core.util.Assert;
import org.pentaho.hadoop.shim.api.format.IPentahoOutputFormat;
import org.pentaho.hadoop.shim.api.format.IPentahoOutputFormat.IPentahoRecordWriter;
import org.pentaho.hadoop.shim.api.format.IPentahoParquetOutputFormat.COMPRESSION;
import org.pentaho.hadoop.shim.api.format.IPentahoParquetOutputFormat.VERSION;
import org.pentaho.hadoop.shim.api.format.ParquetSpec;

public class PentahoParquetOutputFormatTest {

//#if shim_type!="MAPR"
  @Test
  public void createRecordWriterWhenSchemaAndPathIsNotNull() throws Exception {
    PentahoParquetOutputFormat pentahoParquetOutputFormat = new PentahoParquetOutputFormat();

    String tempFile = Files.createTempDirectory( "parquet" ).toUri().toString();
    pentahoParquetOutputFormat.setOutputFile( tempFile.toString() + "test", true );
    pentahoParquetOutputFormat.setFields( ParquetUtils.createOutputFields() );

    IPentahoOutputFormat.IPentahoRecordWriter recordWriter = pentahoParquetOutputFormat.createRecordWriter();

    Assert.assertNotNull( recordWriter, "recordWriter should NOT be null!" );
    Assert.assertTrue( recordWriter instanceof IPentahoOutputFormat.IPentahoRecordWriter,
      "recordWriter should be instance of IPentahoInputFormat.IPentahoRecordReader" );
  }
//#endif

  @Test( expected = RuntimeException.class )
  public void createRecordWriterWhenSchemaIsNull() throws Exception {

    String tempFile = Files.createTempDirectory( "parquet" ).toUri().toString();

    PentahoParquetOutputFormat pentahoParquetOutputFormat = new PentahoParquetOutputFormat();

    pentahoParquetOutputFormat.setOutputFile( tempFile.toString() + "test1", true );
    pentahoParquetOutputFormat.setFields( null );

    pentahoParquetOutputFormat.createRecordWriter();
  }

  @Test( expected = RuntimeException.class )
  public void createRecordWriterWhenPathIsNull() throws Exception {

    String tempFile = null;

    PentahoParquetOutputFormat pentahoParquetOutputFormat = new PentahoParquetOutputFormat();

    pentahoParquetOutputFormat.setOutputFile( tempFile, true );
    pentahoParquetOutputFormat.setFields( ParquetUtils.createOutputFields() );

    pentahoParquetOutputFormat.createRecordWriter();
  }

//#if shim_type!="MAPR"
  @Test
  public void testFileOutput() throws Exception {
    long sz1un = writeData( "1_uncompressed_nodict.par", VERSION.VERSION_1_0, COMPRESSION.UNCOMPRESSED, true );
    long sz1gn = writeData( "1_gzip_nodict.par", VERSION.VERSION_1_0, COMPRESSION.GZIP, true );
    // long sz1ln=writeData( "1_lzo_nodict.par", VERSION.VERSION_1_0, COMPRESSION.LZO, false );
    long sz1sn = writeData( "1_snappy_nodict.par", VERSION.VERSION_1_0, COMPRESSION.SNAPPY, true );
    long sz1ud = writeData( "1_uncompressed_dict.par", VERSION.VERSION_1_0, COMPRESSION.UNCOMPRESSED, false );
    if ( sz1un == sz1gn ) {
      throw new Exception( "GZipped file should have different length than uncompressed" );
    }
    if ( sz1un == sz1sn ) {
      throw new Exception( "Snapped file should have different length than uncompressed" );
    }

    long sz2un = writeData( "2_uncompressed_nodict.par", VERSION.VERSION_2_0, COMPRESSION.UNCOMPRESSED, true );
    long sz2gn = writeData( "2_gzip_nodict.par", VERSION.VERSION_2_0, COMPRESSION.GZIP, true );
    // long sz2ln=writeData( "2_lzo_nodict.par", VERSION.VERSION_2_0, COMPRESSION.LZO, false );
    long sz2sn = writeData( "2_snappy_nodict.par", VERSION.VERSION_2_0, COMPRESSION.SNAPPY, true );
    long sz2ud = writeData( "2_uncompressed_dict.par", VERSION.VERSION_2_0, COMPRESSION.UNCOMPRESSED, false );
    if ( sz2un == sz2gn ) {
      throw new Exception( "GZipped file should have different length than uncompressed" );
    }
    if ( sz2un == sz2sn ) {
      throw new Exception( "Snapped file should have different length than uncompressed" );
    }
  }

  @Test
  public void testSpacesInOutputFilePath() {
    Exception exception = null;
    try {
      PentahoParquetOutputFormat pentahoParquetOutputFormat = new PentahoParquetOutputFormat();
      pentahoParquetOutputFormat.setOutputFile( "/test test/output.parquet", true );
    } catch ( Exception e ) {
      exception = e;
    }
    //BACKLOG-19435: After this change URISyntaxException is not exceptied
    Assert.assertNull( exception );
  }

  private long writeData( String file, VERSION ver, COMPRESSION compr, boolean dictionary ) throws Exception {
    PentahoParquetOutputFormat of = new PentahoParquetOutputFormat();
    of.setVersion( ver );
    of.setCompression( compr );
    of.enableDictionary( dictionary );

    List<ParquetOutputField> fields = new ArrayList<>();

    ParquetOutputField outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fnum" );
    outputField.setPentahoFieldName( "fnum" );
    outputField.setFormatType( ParquetSpec.DataType.UTF8 );
    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fstring" );
    outputField.setPentahoFieldName( "fstring" );
    outputField.setFormatType( ParquetSpec.DataType.UTF8 );
    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fdate" );
    outputField.setPentahoFieldName( "fdate" );
    outputField.setFormatType( ParquetSpec.DataType.DATE );
    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fbool" );
    outputField.setPentahoFieldName( "fbool" );
    outputField.setFormatType( ParquetSpec.DataType.BOOLEAN );
    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fint" );
    outputField.setPentahoFieldName( "fint" );
    outputField.setFormatType( ParquetSpec.DataType.INT_64 );
    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "fbignum" );
    outputField.setPentahoFieldName( "fbignum" );
    outputField.setFormatType( ParquetSpec.DataType.DECIMAL );
    outputField.setScale( "2" );
    outputField.setPrecision( "10" );

    outputField.setAllowNull( true );
    fields.add( outputField );

    outputField = new ParquetOutputField();
    outputField.setFormatFieldName( "ftime" );
    outputField.setPentahoFieldName( "ftime" );
    outputField.setFormatType( ParquetSpec.DataType.TIMESTAMP_MILLIS );
    outputField.setAllowNull( true );
    fields.add( outputField );

    of.setFields( fields );
    of.setOutputFile( "testparquet/" + file, true );
    IPentahoRecordWriter wr = of.createRecordWriter();
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaNumber( "fnum" ) );
    rowMeta.addValueMeta( new ValueMetaString( "fstring" ) );
    rowMeta.addValueMeta( new ValueMetaDate( "fdate" ) );
    rowMeta.addValueMeta( new ValueMetaBoolean( "fbool" ) );
    rowMeta.addValueMeta( new ValueMetaInteger( "fint" ) );
    rowMeta.addValueMeta( new ValueMetaBigNumber( "fbignum" ) );
    rowMeta.addValueMeta( new ValueMetaTimestamp( "ftime" ) );

    SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    df.setTimeZone( TimeZone.getTimeZone( "Europe/Minsk" ) );

    wr.write( new RowMetaAndData( rowMeta, 2.1, "John", df.parse( "2018-01-01 13:00:00" ), true, 1L,
        new BigDecimal( 4.5 ), null ) );
    wr.write( new RowMetaAndData( rowMeta, null, "Paul", df.parse( "2018-01-01 09:10:15" ), false, 3L, null,
        new Timestamp( df.parse( "2018-05-01 13:00:00" ).getTime() ) ) );
    wr.write( new RowMetaAndData( rowMeta, 2.1, "George", null, true, null, new BigDecimal( 4.5 ),
        new Timestamp( df.parse( "2018-05-01 13:00:00" ).getTime() ) ) );
    wr.write( new RowMetaAndData( rowMeta, 2.1, "Ringo", df.parse( "2018-01-01 09:10:35" ), null, 4L,
        new BigDecimal( 4.5 ), new Timestamp( df.parse( "2018-05-01 13:00:00" ).getTime() ) ) );
    wr.close();

    File f = new File( "testparquet/" + file );
    long sz = f.length();
    if ( sz == 0 ) {
      throw new Exception( "File " + f.getAbsolutePath() + " is empty" );
    }
    return sz;
  }
//#endif
}

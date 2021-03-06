/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push.reports;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;

/**
 * Wraps a report that is downloaded from the web interface, e.g. a CSV file, Excel workbook or PDF.  The report
 * contains a snapshot of a view client's data.
 * @deprecated Reports are being redesigned
 */
public class Report {

  /** Name of the report file */
  private final String _filename;

  /** Stream for reading the report */
  private final InputStream _inputStream;

  /** Media type of the report file */
  private final MediaType _mediaType;

  public Report(String filename, InputStream inputStream, MediaType mediaType) {
    _filename = filename;
    _inputStream = inputStream;
    _mediaType = mediaType;
  }

  /**
   * @return Input stream for reading the report
   */
  public InputStream getInputStream() {
    return _inputStream;
  }

  /**
   * @return Name of the report file
   */
  public String getFilename() {
    return _filename;
  }

  /**
   * @return HTTP media type of the report file
   */
  public MediaType getMediaType() {
    return _mediaType;
  }
}

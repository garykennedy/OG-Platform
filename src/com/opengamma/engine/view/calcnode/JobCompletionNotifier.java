/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.view.calcnode;

/**
 * 
 *
 * @author kirk
 */
public interface JobCompletionNotifier {
  
  void jobCompleted(CalculationJobResult jobResult);

}

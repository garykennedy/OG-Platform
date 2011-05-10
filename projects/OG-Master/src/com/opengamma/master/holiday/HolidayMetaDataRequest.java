/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.holiday;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.beans.BeanDefinition;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaProperty;

import com.opengamma.master.AbstractMetaDataRequest;
import com.opengamma.util.PublicSPI;

/**
 * Request for meta-data about the holiday master.
 * <p>
 * This will return meta-data valid for the whole master.
 */
@PublicSPI
@BeanDefinition
public class HolidayMetaDataRequest extends AbstractMetaDataRequest {

  /**
   * Whether to fetch the holiday types meta-data, true by default.
   */
  @PropertyDefinition
  private boolean _holidayTypes = true;

  /**
   * Creates an instance.
   */
  public HolidayMetaDataRequest() {
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code HolidayMetaDataRequest}.
   * @return the meta-bean, not null
   */
  public static HolidayMetaDataRequest.Meta meta() {
    return HolidayMetaDataRequest.Meta.INSTANCE;
  }

  @Override
  public HolidayMetaDataRequest.Meta metaBean() {
    return HolidayMetaDataRequest.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName) {
    switch (propertyName.hashCode()) {
      case 15120129:  // holidayTypes
        return isHolidayTypes();
    }
    return super.propertyGet(propertyName);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue) {
    switch (propertyName.hashCode()) {
      case 15120129:  // holidayTypes
        setHolidayTypes((Boolean) newValue);
        return;
    }
    super.propertySet(propertyName, newValue);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether to fetch the holiday types meta-data, true by default.
   * @return the value of the property
   */
  public boolean isHolidayTypes() {
    return _holidayTypes;
  }

  /**
   * Sets whether to fetch the holiday types meta-data, true by default.
   * @param holidayTypes  the new value of the property
   */
  public void setHolidayTypes(boolean holidayTypes) {
    this._holidayTypes = holidayTypes;
  }

  /**
   * Gets the the {@code holidayTypes} property.
   * @return the property, not null
   */
  public final Property<Boolean> holidayTypes() {
    return metaBean().holidayTypes().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code HolidayMetaDataRequest}.
   */
  public static class Meta extends AbstractMetaDataRequest.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code holidayTypes} property.
     */
    private final MetaProperty<Boolean> _holidayTypes = DirectMetaProperty.ofReadWrite(this, "holidayTypes", Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map;

    @SuppressWarnings({"unchecked", "rawtypes" })
    protected Meta() {
      LinkedHashMap temp = new LinkedHashMap(super.metaPropertyMap());
      temp.put("holidayTypes", _holidayTypes);
      _map = Collections.unmodifiableMap(temp);
    }

    @Override
    public HolidayMetaDataRequest createBean() {
      return new HolidayMetaDataRequest();
    }

    @Override
    public Class<? extends HolidayMetaDataRequest> beanType() {
      return HolidayMetaDataRequest.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code holidayTypes} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> holidayTypes() {
      return _holidayTypes;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
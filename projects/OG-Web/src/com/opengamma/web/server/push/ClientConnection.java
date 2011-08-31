/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push;

import com.opengamma.core.change.ChangeEvent;
import com.opengamma.core.change.ChangeListener;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Associated with one client connection (i.e. one browser window / tab / client app instance).
 */
/* package */ class ClientConnection implements ChangeListener {

  private final String _userId;
  private final String _clientId;
  private final RestUpdateListener _listener;
  private final ViewportFactory _viewportFactory;

  /** REST URLs for entities keyed on the entity's {@link UniqueId} */
  private final Map<ObjectId, String> _entityUrls = new ConcurrentHashMap<ObjectId, String>();

  /* package */ ClientConnection(String userId, String clientId, RestUpdateListener listener, ViewportFactory viewportFactory) {
    // TODO check args
    _viewportFactory = viewportFactory;
    _userId = userId;
    _listener = listener;
    _clientId = clientId;
  }

  /* package */ String getClientId() {
    return _clientId;
  }

  /* package */ String getUserId() {
    return _userId;
  }

  /**
   * Creates a new subscription for a view client, replacing any existing subscription for that view client.
   * @param viewportDefinition
   */
  /* package */ void createViewport(ViewportDefinition viewportDefinition, String viewportUrl, String dataUrl, String gridUrl) {
    AnalyticsListener listener = new AnalyticsListener(dataUrl, gridUrl, _listener);
    _viewportFactory.createViewport(_clientId, viewportUrl, viewportDefinition, listener);
  }

  /* package */ void disconnect() {
    _entityUrls.clear();
    _viewportFactory.clientDisconnected(_clientId);
  }

  /* package */ void subscribe(UniqueId uid, String url) {
    // TODO check args?
    _entityUrls.put(uid.getObjectId(), url);
  }

  @Override
  public void entityChanged(ChangeEvent event) {
    String url = _entityUrls.remove(event.getAfterId().getObjectId());
    if (url != null) {
      _listener.itemUpdated(url);
    }
  }

  /* package */ Viewport getViewport(String viewportUrl) {
    return _viewportFactory.getViewport(viewportUrl);
  }
}

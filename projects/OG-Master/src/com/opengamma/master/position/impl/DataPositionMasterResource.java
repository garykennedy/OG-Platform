/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.position.impl;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.opengamma.id.ObjectId;
import com.opengamma.master.position.PositionDocument;
import com.opengamma.master.position.PositionMaster;
import com.opengamma.master.position.PositionSearchRequest;
import com.opengamma.master.position.PositionSearchResult;
import com.opengamma.transport.jaxrs.FudgeRest;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.rest.AbstractDataResource;

/**
 * RESTful resource for positions.
 * <p>
 * The positions resource receives and processes RESTful calls to the position master.
 */
@Path("positionMaster")
public class DataPositionMasterResource extends AbstractDataResource {

  /**
   * The position master.
   */
  private final PositionMaster _posMaster;

  /**
   * Creates the resource, exposing the underlying master over REST.
   * 
   * @param positionMaster  the underlying position master, not null
   */
  public DataPositionMasterResource(final PositionMaster positionMaster) {
    ArgumentChecker.notNull(positionMaster, "positionMaster");
    _posMaster = positionMaster;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the position master.
   * 
   * @return the position master, not null
   */
  public PositionMaster getPositionMaster() {
    return _posMaster;
  }

  //-------------------------------------------------------------------------
  @HEAD
  @Path("positions")
  public Response status() {
    // simple HEAD to quickly return, avoiding loading the whole database
    return Response.ok().build();
  }

  @POST
  @Path("positionSearches")
  @Consumes(FudgeRest.MEDIA)
  public Response search(PositionSearchRequest request) {
    PositionSearchResult result = getPositionMaster().search(request);
    return Response.ok(result).build();
  }

  @POST
  @Path("positions")
  @Consumes(FudgeRest.MEDIA)
  public Response add(@Context UriInfo uriInfo, PositionDocument request) {
    PositionDocument result = getPositionMaster().add(request);
    URI createdUri = DataPositionResource.uriVersion(uriInfo.getBaseUri(), result.getUniqueId());
    return Response.created(createdUri).entity(result).build();
  }

  //-------------------------------------------------------------------------
  @Path("positions/{positionId}")
  public DataPositionResource findPosition(@PathParam("positionId") String idStr) {
    ObjectId id = ObjectId.parse(idStr);
    return new DataPositionResource(this, id);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a URI.
   * 
   * @param baseUri  the base URI, not null
   * @return the URI, not null
   */
  public static URI uriSearch(URI baseUri) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("positionSearches");
    return bld.build();
  }

  /**
   * Builds a URI.
   * 
   * @param baseUri  the base URI, not null
   * @return the URI, not null
   */
  public static URI uriAdd(URI baseUri) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("positions");
    return bld.build();
  }

}
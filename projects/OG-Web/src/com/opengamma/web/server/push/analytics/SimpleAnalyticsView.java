/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push.analytics;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.opengamma.engine.ComputationTargetResolver;
import com.opengamma.engine.view.ViewResultModel;
import com.opengamma.engine.view.calc.ViewCycle;
import com.opengamma.engine.view.compilation.CompiledViewDefinition;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ class SimpleAnalyticsView implements AnalyticsView {

  private static final Logger s_logger = LoggerFactory.getLogger(SimpleAnalyticsView.class);

  private final ResultsCache _cache = new ResultsCache();
  private final AnalyticsViewListener _listener;
  private final ComputationTargetResolver _targetResolver;

  private MainAnalyticsGrid _portfolioGrid;
  private MainAnalyticsGrid _primitivesGrid;
  private CompiledViewDefinition _compiledViewDefinition;

  public SimpleAnalyticsView(AnalyticsViewListener listener,
                             String portoflioGridId,
                             String primitivesGridId,
                             ComputationTargetResolver targetResolver) {
    ArgumentChecker.notNull(listener, "listener");
    ArgumentChecker.notNull(portoflioGridId, "portoflioGridId");
    ArgumentChecker.notNull(primitivesGridId, "primitivesGridId");
    ArgumentChecker.notNull(targetResolver, "targetResolver");
    _targetResolver = targetResolver;
    _portfolioGrid = MainAnalyticsGrid.emptyPortfolio(portoflioGridId, _targetResolver);
    _primitivesGrid = MainAnalyticsGrid.emptyPrimitives(primitivesGridId, targetResolver);
    _listener = listener;
  }

  @Override
  public void updateStructure(CompiledViewDefinition compiledViewDefinition) {
    _compiledViewDefinition = compiledViewDefinition;
    // TODO this loses all dependency graphs. new grid needs to rebuild graphs from old grid. need stable row and col IDs to do that
    _portfolioGrid = MainAnalyticsGrid.portfolio(_compiledViewDefinition, _portfolioGrid.getGridId(), _targetResolver);
    _primitivesGrid = MainAnalyticsGrid.primitives(_compiledViewDefinition, _primitivesGrid.getGridId(), _targetResolver);
    List<String> gridIds = new ArrayList<String>();
    gridIds.add(_portfolioGrid.getGridId());
    gridIds.add(_primitivesGrid.getGridId());
    gridIds.addAll(_portfolioGrid.getDependencyGraphGridIds());
    gridIds.addAll(_primitivesGrid.getDependencyGraphGridIds());
    _listener.gridStructureChanged(gridIds);
  }

  @Override
  public void updateResults(ViewResultModel results, ViewCycle viewCycle) {
    _cache.put(results);
    List<String> updatedIds = Lists.newArrayList();
    updatedIds.addAll(_portfolioGrid.updateResults(_cache, viewCycle));
    updatedIds.addAll(_primitivesGrid.updateResults(_cache, viewCycle));
    _listener.gridDataChanged(updatedIds);
  }

  private MainAnalyticsGrid getGrid(GridType gridType) {
    switch (gridType) {
      case PORTFORLIO:
        return _portfolioGrid;
      case PRIMITIVES:
        return _primitivesGrid;
      default:
        throw new IllegalArgumentException("Unexpected grid type " + gridType);
    }
  }

  @Override
  public GridStructure getGridStructure(GridType gridType) {
    s_logger.debug("Getting grid structure for the {} grid", gridType);
    return getGrid(gridType).getGridStructure();
  }

  @Override
  public long createViewport(GridType gridType, String viewportId, String dataId, ViewportSpecification viewportSpec) {
    long version = getGrid(gridType).createViewport(viewportId, dataId, viewportSpec);
    _listener.gridDataChanged(dataId);
    s_logger.debug("Created viewport ID {} for the {} grid from {}", new Object[]{viewportId, gridType, viewportSpec});
    return version;
  }

  @Override
  public long updateViewport(GridType gridType, String viewportId, ViewportSpecification viewportSpec) {
    s_logger.debug("Updating viewport {} for {} grid to {}", new Object[]{viewportId, gridType, viewportSpec});
    long version = getGrid(gridType).updateViewport(viewportId, viewportSpec);
    _listener.gridDataChanged(getGrid(gridType).getViewport(viewportId).getDataId());
    return version;
  }

  @Override
  public void deleteViewport(GridType gridType, String viewportId) {
    s_logger.debug("Deleting viewport {} from the {} grid", viewportId, gridType);
    getGrid(gridType).deleteViewport(viewportId);
  }

  @Override
  public ViewportResults getData(GridType gridType, String viewportId) {
    s_logger.debug("Getting data for viewport {} of the {} grid", viewportId, gridType);
    return getGrid(gridType).getData(viewportId);
  }

  @Override
  public void openDependencyGraph(GridType gridType, String graphId, String gridId, int row, int col) {
    s_logger.debug("Opening dependency graph for cell ({}, {}) of the {} grid", new Object[]{row, col, gridType});
    getGrid(gridType).openDependencyGraph(graphId, gridId, row, col, _compiledViewDefinition);
    _listener.gridStructureChanged(getGrid(gridType).getDependencyGraph(graphId).getGridId());
  }

  @Override
  public void closeDependencyGraph(GridType gridType, String graphId) {
    s_logger.debug("Closing dependency graph {} of the {} grid", graphId, gridType);
    getGrid(gridType).closeDependencyGraph(graphId);
  }

  @Override
  public GridStructure getGridStructure(GridType gridType, String graphId) {
    s_logger.debug("Getting grid structure for dependency graph {} of the {} grid", graphId, gridType);
    return getGrid(gridType).getGridStructure(graphId);
  }

  @Override
  public long createViewport(GridType gridType, String graphId, String viewportId, String dataId, ViewportSpecification viewportSpec) {
    long version = getGrid(gridType).createViewport(graphId, viewportId, dataId, viewportSpec);
    s_logger.debug("Created viewport ID {} for dependency graph {} of the {} grid using {}", new Object[]{viewportId, graphId, gridType, viewportSpec});
    _listener.gridDataChanged(dataId);
    return version;
  }

  @Override
  public long updateViewport(GridType gridType, String graphId, String viewportId, ViewportSpecification viewportSpec) {
    s_logger.debug("Updating viewport for dependency graph {} of the {} grid using {}", new Object[]{graphId, gridType, viewportSpec});
    long version = getGrid(gridType).updateViewport(graphId, viewportId, viewportSpec);
    _listener.gridDataChanged(getGrid(gridType).getViewport(viewportId).getDataId());
    return version;
  }

  @Override
  public void deleteViewport(GridType gridType, String graphId, String viewportId) {
    s_logger.debug("Deleting viewport {} from dependency graph {} of the {} grid", new Object[]{viewportId, graphId, gridType});
    getGrid(gridType).deleteViewport(graphId, viewportId);
  }

  @Override
  public ViewportResults getData(GridType gridType, String graphId, String viewportId) {
    s_logger.debug("Getting data for the viewport {} of the dependency graph {} of the {} grid", new Object[]{viewportId, graphId, gridType});
    return getGrid(gridType).getData(graphId, viewportId);
  }

}

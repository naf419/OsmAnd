package net.osmand.plus.onlinerouting.engine;

import static net.osmand.plus.onlinerouting.engine.EngineType.GPX_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.LocationsHolder;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.VehicleType;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingEnvironment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;
import net.osmand.router.network.NetworkRouteGpxApproximator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GpxEngine extends OnlineRoutingEngine {

	private static final String ONLINE_ROUTING_GPX_FILE_NAME = "online_routing_gpx";

	public GpxEngine(@Nullable Map<String, String> params) {
		super(params);
	}

	@NonNull
	@Override
	public OnlineRoutingEngine getType() {
		return GPX_TYPE;
	}

	@Override
	@NonNull
	public String getTitle() {
		return "GPX";
	}

	@NonNull
	@Override
	public String getTypeName() {
		return "GPX";
	}

	@Override
	protected void makeFullUrl(@NonNull StringBuilder sb, @NonNull List<LatLon> path, @Nullable Float startBearing) {
		sb.append("?");
		for (int i = 0; i < path.size(); i++) {
			LatLon point = path.get(i);
			sb.append("point=")
					.append(point.getLatitude())
					.append(',')
					.append(point.getLongitude());
			if (i < path.size() - 1) {
				sb.append('&');
			}
		}
		if (startBearing != null) {
			if (sb.charAt(sb.length() - 1) != '?') {
				sb.append('&');
			}
			sb.append("heading=").append(startBearing.intValue());
		}
	}

	@NonNull
	@Override
	public String getStandardUrl() {
		return "";
	}

	@Override
	protected void collectAllowedVehicles(@NonNull List<VehicleType> vehicles) {

	}

	@Override
	protected void collectAllowedParameters(@NonNull Set<EngineParameter> params) {
		params.add(EngineParameter.KEY);
		params.add(EngineParameter.CUSTOM_NAME);
		params.add(EngineParameter.NAME_INDEX);
		params.add(EngineParameter.CUSTOM_URL);
		params.add(EngineParameter.APPROXIMATION_ROUTING_PROFILE);
		params.add(EngineParameter.APPROXIMATION_DERIVED_PROFILE);
		params.add(EngineParameter.NETWORK_APPROXIMATE_ROUTE);
		params.add(EngineParameter.USE_EXTERNAL_TIMESTAMPS);
		params.add(EngineParameter.USE_ROUTING_FALLBACK);
	}

	@Override
	public void updateRouteParameters(@NonNull RouteCalculationParams params, @Nullable RouteCalculationResult previousRoute) {
		super.updateRouteParameters(params, previousRoute);
		if ((previousRoute == null || previousRoute.isEmpty()) && shouldApproximateRoute()) {
			params.initialCalculation = true;
		}
	}

	@Override
	public OnlineRoutingEngine newInstance(Map<String, String> params) {
		return new GpxEngine(params);
	}

	@Override
	@Nullable
	public OnlineRoutingResponse parseResponse(@NonNull String content, @NonNull OsmandApplication app,
	                                           boolean leftSideNavigation, boolean initialCalculation,
	                                           @Nullable RouteCalculationProgress calculationProgress) {
		GPXFile gpxFile = parseGpx(content);
		return gpxFile != null ? prepareResponse(app, gpxFile, initialCalculation, calculationProgress) : null;
	}

	private OnlineRoutingResponse prepareResponse(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                              boolean initialCalculation, @Nullable RouteCalculationProgress calculationProgress) {
		boolean calculatedTimeSpeed = useExternalTimestamps();
		if (shouldApproximateRoute() && !initialCalculation) {
			MeasurementEditingContext ctx = prepareApproximationContext(app, gpxFile, calculationProgress);
			if (ctx != null) {
				GPXFile approximated = ctx.exportGpx(ONLINE_ROUTING_GPX_FILE_NAME);
				if (approximated != null) {
					calculatedTimeSpeed = ctx.hasCalculatedTimeSpeed();
					gpxFile = approximated;
				}
			}
		}
		return new OnlineRoutingResponse(gpxFile, calculatedTimeSpeed);
	}

	@Nullable
	private MeasurementEditingContext prepareApproximationContext(@NonNull OsmandApplication app,
	                                                              @NonNull GPXFile gpxFile,
	                                                              @Nullable RouteCalculationProgress calculationProgress) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		ApplicationMode appMode = routingHelper.getAppMode();
		String oldRoutingProfile = appMode.getRoutingProfile();
		String oldDerivedProfile = appMode.getDerivedProfile();
		try {
			String routingProfile = getApproximationRoutingProfile();
			if (routingProfile != null) {
				appMode.setRoutingProfile(routingProfile);
				appMode.setDerivedProfile(getApproximationDerivedProfile());
			}
			List<WptPt> points = gpxFile.getAllSegmentsPoints();
			LocationsHolder holder = new LocationsHolder(points);
			if (holder.getSize() > 1) {
				LatLon start = holder.getLatLon(0);
				LatLon end = holder.getLatLon(holder.getSize() - 1);
				RoutingEnvironment env = routingHelper.getRoutingEnvironment(app, appMode, start, end);
				GpxRouteApproximation gctx = new GpxRouteApproximation(env.getCtx());
				gctx.ctx.calculationProgress = calculationProgress;
				List<GpxPoint> gpxPoints = routingHelper.generateGpxPoints(env, gctx, holder);
				GpxRouteApproximation gpxApproximation;
				if (shouldNetworkApproximateRoute()) {
					BinaryMapIndexReader[] readers = app.getResourceManager().getRoutingMapFiles();
					NetworkRouteGpxApproximator gpxApproximator = new NetworkRouteGpxApproximator(readers, true);
					try {
						gpxApproximator.approximate(gpxFile, env.getCtx());
					} catch (IOException e) {
						LOG.error(e.getMessage(), e);
					}
					gpxApproximation = prepareApproximationResult(gctx, gpxPoints, gpxApproximator);
					points = Arrays.asList(points.get(0), points.get(points.size() - 1));
				} else {
					gpxApproximation = routingHelper.calculateGpxApproximation(env, gctx, gpxPoints, null);
				}
				MeasurementEditingContext ctx = new MeasurementEditingContext(app);
				ctx.setPoints(gpxApproximation, points, appMode, useExternalTimestamps());
				return ctx;
			}
		} catch (IOException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			appMode.setRoutingProfile(oldRoutingProfile);
			appMode.setDerivedProfile(oldDerivedProfile);
		}
		return null;
	}

	private GpxRouteApproximation prepareApproximationResult(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
	                                                         NetworkRouteGpxApproximator gpxApproximator) {
		GpxPoint first = gpxPoints.get(0);
		first.routeToTarget = gpxApproximator.result;
		GpxPoint last = gpxPoints.get(gpxPoints.size() - 1);
		last.ind = 1;
		last.routeToTarget = new ArrayList<>();
		gctx.finalPoints.addAll(Arrays.asList(first, last));
		gpxPoints.addAll(gctx.finalPoints);
		gctx.result = gpxApproximator.result;
		return gctx;
	}

	@Override
	public boolean isResultOk(@NonNull StringBuilder errorMessage,
	                          @NonNull String content) {
		return parseGpx(content) != null;
	}

	@Nullable
	private GPXFile parseGpx(@NonNull String content) {
		InputStream gpxStream;
		try {
			gpxStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
			return GPXUtilities.loadGPXFile(gpxStream);
		} catch (UnsupportedEncodingException e) {
			LOG.debug("Error when parsing GPX from server response: " + e.getMessage());
		}
		return null;
	}
}

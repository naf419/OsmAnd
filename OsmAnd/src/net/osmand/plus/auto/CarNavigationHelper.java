package net.osmand.plus.auto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Lane;
import androidx.car.app.navigation.model.LaneDirection;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.router.TurnType;

import java.util.TimeZone;

public class CarNavigationHelper {

	private final OsmandApplication app;

	public CarNavigationHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	public Trip buildTrip() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		OsmandSettings settings = app.getSettings();

		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPointsHelper.TargetPoint pointToNavigate = targetPointsHelper.getPointToNavigate();
		Trip.Builder tripBuilder = new Trip.Builder();
		if (pointToNavigate != null) {
			Destination.Builder destBuilder = new Destination.Builder();
			destBuilder.setName(pointToNavigate.getOnlyName());
			destBuilder.setImage(new CarIcon.Builder(IconCompat.createWithResource(app,
					R.drawable.ic_action_point_destination)).build());

			Distance distance = Distance.create(routingHelper.getLeftDistance(), Distance.UNIT_METERS);
			int leftTimeSec = routingHelper.getLeftTime();
			DateTimeWithZone dateTime = DateTimeWithZone.create(System.currentTimeMillis() + leftTimeSec * 1000L, TimeZone.getDefault());
			TravelEstimate.Builder travelEstimateBuilder = new TravelEstimate.Builder(distance, dateTime);
			travelEstimateBuilder.setRemainingTimeSeconds(leftTimeSec);
			Destination destination = destBuilder.build();
			TravelEstimate travelEstimate = travelEstimateBuilder.build();
			tripBuilder.addDestination(destination, travelEstimate);
		}
		boolean routeBeingCalculated = routingHelper.isRouteBeingCalculated();
		tripBuilder.setLoading(routeBeingCalculated);
		if (!routeBeingCalculated) {
			Step.Builder stepBuilder = new Step.Builder();
			Maneuver.Builder turnBuilder;
			TurnType turnType = null;
			boolean leftSide = settings.DRIVING_REGION.get().leftHandDriving;
			boolean deviatedFromRoute = routingHelper.isDeviatedFromRoute();
			int turnImminent = 0;
			int nextTurnDistance = 0;
			RouteCalculationResult.NextDirectionInfo nextDirInfo;
			RouteCalculationResult.NextDirectionInfo calc = new RouteCalculationResult.NextDirectionInfo();
			if (deviatedFromRoute) {
				turnType = TurnType.valueOf(TurnType.OFFR, leftSide);
				nextTurnDistance = (int) routingHelper.getRouteDeviation();
			} else {
				nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, true);
				if (nextDirInfo != null && nextDirInfo.distanceTo > 0 && nextDirInfo.directionInfo != null) {
					turnType = nextDirInfo.directionInfo.getTurnType();
					nextTurnDistance = nextDirInfo.distanceTo;
					turnImminent = nextDirInfo.imminent;
				}
			}
			if (turnType != null) {
				TurnDrawable drawable = new TurnDrawable(app, false);
				int height = (int) app.getResources().getDimension(android.R.dimen.notification_large_icon_height);
				int width = (int) app.getResources().getDimension(android.R.dimen.notification_large_icon_width);
				drawable.setBounds(0, 0, width, height);
				drawable.setTurnType(turnType);
				drawable.setTurnImminent(turnImminent, deviatedFromRoute);
				Bitmap turnBitmap = drawableToBitmap(app, drawable);
				turnBuilder = new Maneuver.Builder(getManeuverType(turnType));
				if (turnType.isRoundAbout()) {
					turnBuilder.setRoundaboutExitNumber(turnType.getExitOut());
				}
				turnBuilder.setIcon(new CarIcon.Builder(IconCompat.createWithBitmap(turnBitmap)).build());
			} else {
				turnBuilder = new Maneuver.Builder(Maneuver.TYPE_UNKNOWN);
			}
			Maneuver maneuver = turnBuilder.build();
			String cue = turnType != null ? RouteCalculationResult.toString(turnType, app, true) : "";
			stepBuilder.setManeuver(maneuver);
			stepBuilder.setCue(cue);

			nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, false);
			if (nextDirInfo != null && nextDirInfo.directionInfo != null && nextDirInfo.directionInfo.getTurnType() != null) {
				int[] lanes = nextDirInfo.directionInfo.getTurnType().getLanes();
				// Do not show too far
				if ((nextDirInfo.distanceTo > 800 && nextDirInfo.directionInfo.getTurnType().isSkipToSpeak()) || nextDirInfo.distanceTo > 1200) {
					lanes = null;
				}
				//int dist = nextDirInfo.distanceTo;
				if (lanes != null) {
					for (int lane : lanes) {
						int firstTurnType = TurnType.getPrimaryTurn(lane);
						int secondTurnType = TurnType.getSecondaryTurn(lane);
						int thirdTurnType = TurnType.getTertiaryTurn(lane);
						Lane.Builder laneBuilder = new Lane.Builder();
						laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(firstTurnType, leftSide)), (lane & 1) == 1));
						if (secondTurnType > 0) {
							laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(secondTurnType, leftSide)), false));
						}
						if (thirdTurnType > 0) {
							laneBuilder.addDirection(LaneDirection.create(getLaneDirection(TurnType.valueOf(thirdTurnType, leftSide)), false));
						}
						stepBuilder.addLane(laneBuilder.build());
					}
					LanesDrawable lanesDrawable = new LanesDrawable(app, 1);
					lanesDrawable.lanes = lanes;
					lanesDrawable.isTurnByTurn = true;
					lanesDrawable.isNightMode = app.getDaynightHelper().isNightMode();
					lanesDrawable.updateBounds(); // prefer 500 x 74 dp
					Bitmap lanesBitmap = drawableToBitmap(app, lanesDrawable, lanesDrawable.getIntrinsicWidth(), lanesDrawable.getIntrinsicHeight());
					stepBuilder.setLanesImage(new CarIcon.Builder(IconCompat.createWithBitmap(lanesBitmap)).build());
				}
			}

			int leftTurnTimeSec = routingHelper.getLeftTimeNextTurn();
			long turnArrivalTime = System.currentTimeMillis() + leftTurnTimeSec * 1000L;
			Distance stepDistance = Distance.create(nextTurnDistance, Distance.UNIT_METERS);
			DateTimeWithZone stepDateTime = DateTimeWithZone.create(turnArrivalTime, TimeZone.getDefault());
			TravelEstimate.Builder stepTravelEstimateBuilder = new TravelEstimate.Builder(stepDistance, stepDateTime);
			stepTravelEstimateBuilder.setRemainingTimeSeconds(leftTurnTimeSec);
			Step step = stepBuilder.build();
			TravelEstimate stepTravelEstimate = stepTravelEstimateBuilder.build();
			tripBuilder.addStep(step, stepTravelEstimate);
			if (!deviatedFromRoute) {
				nextDirInfo = routingHelper.getNextRouteDirectionInfo(calc, true);
				CurrentStreetName currentName = routingHelper.getCurrentName(nextDirInfo);
				tripBuilder.setCurrentRoad(currentName.text);
			}
		}
		return tripBuilder.build();
	}

	@NonNull
	private Bitmap drawableToBitmap(@NonNull Context ctx, @NonNull Drawable drawable) {
		int height = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_width);
		return drawableToBitmap(ctx, drawable, width, height);
	}

	private Bitmap drawableToBitmap(@NonNull Context ctx, @NonNull Drawable drawable, int width, int height) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	private int getManeuverType(@NonNull TurnType turnType) {
		switch (turnType.getValue()) {
			case TurnType.C: return Maneuver.TYPE_STRAIGHT; // continue (go straight)
			case TurnType.TL: return Maneuver.TYPE_TURN_NORMAL_LEFT; // turn left
			case TurnType.TSLL: return Maneuver.TYPE_TURN_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL: return Maneuver.TYPE_TURN_SHARP_LEFT; // turn sharply left
			case TurnType.TR: return Maneuver.TYPE_TURN_NORMAL_RIGHT; // turn right
			case TurnType.TSLR: return Maneuver.TYPE_TURN_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR: return Maneuver.TYPE_TURN_SHARP_RIGHT; // turn sharply right
			case TurnType.KL: return Maneuver.TYPE_KEEP_LEFT; // keep left
			case TurnType.KR: return Maneuver.TYPE_KEEP_RIGHT; // keep right
			case TurnType.TU: return Maneuver.TYPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU: return Maneuver.TYPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR: return Maneuver.TYPE_UNKNOWN; // Off route
			case TurnType.RNDB: return Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW; // Roundabout
			case TurnType.RNLB: return Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW; // Roundabout left
			default: return Maneuver.TYPE_UNKNOWN;
		}
	}

	private int getLaneDirection(@NonNull TurnType turnType) {
		switch (turnType.getValue()) {
			case TurnType.C: return LaneDirection.SHAPE_STRAIGHT; // continue (go straight)
			case TurnType.TL: return LaneDirection.SHAPE_NORMAL_LEFT; // turn left
			case TurnType.TSLL: return LaneDirection.SHAPE_SLIGHT_LEFT; // turn slightly left
			case TurnType.TSHL: return LaneDirection.SHAPE_SHARP_LEFT; // turn sharply left
			case TurnType.TR: return LaneDirection.SHAPE_NORMAL_RIGHT; // turn right
			case TurnType.TSLR: return LaneDirection.SHAPE_SLIGHT_RIGHT; // turn slightly right
			case TurnType.TSHR: return LaneDirection.SHAPE_SHARP_RIGHT; // turn sharply right
			case TurnType.KL: return LaneDirection.SHAPE_SLIGHT_LEFT; // keep left
			case TurnType.KR: return LaneDirection.SHAPE_SLIGHT_RIGHT; // keep right
			case TurnType.TU: return LaneDirection.SHAPE_U_TURN_LEFT; // U-turn
			case TurnType.TRU: return LaneDirection.SHAPE_U_TURN_RIGHT; // Right U-turn
			case TurnType.OFFR: return LaneDirection.SHAPE_UNKNOWN; // Off route
			case TurnType.RNDB: return LaneDirection.SHAPE_UNKNOWN; // Roundabout
			case TurnType.RNLB: return LaneDirection.SHAPE_UNKNOWN; // Roundabout left
			default: return LaneDirection.SHAPE_UNKNOWN;
		}
	}
}

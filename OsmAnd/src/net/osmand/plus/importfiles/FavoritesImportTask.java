package net.osmand.plus.importfiles;

import static net.osmand.plus.myplaces.ui.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.TAB_ID;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class FavoritesImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private final GPXFile gpxFile;
	private final String fileName;
	private final boolean forceImport;

	public FavoritesImportTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile,
	                           @NonNull String fileName, boolean forceImport) {
		super(activity);
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.forceImport = forceImport;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		mergeFavorites();
		return null;
	}

	private void mergeFavorites() {
		String defCategory = forceImport ? fileName : "";
		List<FavouritePoint> favourites = wptAsFavourites(app, gpxFile.getPoints(), defCategory);
		checkDuplicateNames(favourites);

		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		ParkingPositionPlugin plugin = OsmandPlugin.getPlugin(ParkingPositionPlugin.class);

		for (FavouritePoint favourite : favourites) {
			favoritesHelper.deleteFavourite(favourite, false);
			favoritesHelper.addFavourite(favourite, false);

			if (plugin != null && favourite.getSpecialPointType() == SpecialPointType.PARKING) {
				plugin.updateParkingPoint(favourite);
			}
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile();
	}

	private void checkDuplicateNames(@NonNull List<FavouritePoint> favourites) {
		for (FavouritePoint point : favourites) {
			int number = 1;
			String index;
			String name = point.getName();
			boolean duplicatesFound = false;
			for (FavouritePoint favouritePoint : favourites) {
				if (name.equals(favouritePoint.getName())
						&& point.getCategory().equals(favouritePoint.getCategory())
						&& !point.equals(favouritePoint)) {
					if (!duplicatesFound) {
						index = " (" + number + ")";
						point.setName(name + index);
					}
					duplicatesFound = true;
					number++;
					index = " (" + number + ")";
					favouritePoint.setName(favouritePoint.getName() + index);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(GPXFile result) {
		hideProgress();
		FragmentActivity activity = activityRef.get();
		if (activity != null) {
			app.showToastMessage(R.string.fav_imported_sucessfully);
			Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(TAB_ID, FAV_TAB);
			activity.startActivity(newIntent);
		}
	}

	public static List<FavouritePoint> wptAsFavourites(@NonNull OsmandApplication app,
	                                                   @NonNull List<WptPt> points,
	                                                   @NonNull String defaultCategory) {
		List<FavouritePoint> favourites = new ArrayList<>();
		for (WptPt point : points) {
			if (Algorithms.isEmpty(point.name)) {
				point.name = app.getString(R.string.shared_string_waypoint);
			}
			String category = point.category != null ? point.category : defaultCategory;
			favourites.add(FavouritePoint.fromWpt(point, category));
		}
		return favourites;
	}
}
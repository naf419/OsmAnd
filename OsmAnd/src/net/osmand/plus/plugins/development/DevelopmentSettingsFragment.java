package net.osmand.plus.plugins.development;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Debug;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.SunriseSunset;

import java.text.SimpleDateFormat;

public class DevelopmentSettingsFragment extends BaseSettingsFragment {

	private static final String SIMULATE_INITIAL_STARTUP = "simulate_initial_startup";
	private static final String SIMULATE_YOUR_LOCATION = "simulate_your_location";
	private static final String AGPS_DATA_DOWNLOADED = "agps_data_downloaded";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd  HH:mm");

	private Runnable updateSimulationTitle;

	@Override
	protected void setupPreferences() {
		Preference developmentInfo = findPreference("development_info");
		developmentInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupOpenglRenderPref();

		Preference safeCategory = findPreference("safe");
		safeCategory.setIconSpaceReserved(false);

		setupSafeModePref();
		setupShowHeightmapspref();
		setupApproximationSafeModePref();

		Preference routingCategory = findPreference("routing");
		routingCategory.setIconSpaceReserved(false);

		setupSimulateYourLocationPref();

		Preference debuggingAndDevelopment = findPreference("debugging_and_development");
		debuggingAndDevelopment.setIconSpaceReserved(false);

		setupDebugRenderingInfoPref();
		setupSimulateInitialStartupPref();
		setupFullscreenMapDrawingModePref();
		setupShouldShowFreeVersionBannerPref();
		setupTestVoiceCommandsPref();
		setupLogcatBufferPref();

		Preference info = findPreference("info");
		info.setIconSpaceReserved(false);

		setupMemoryAllocatedForRoutingPref();
		setupGlobalAppAllocatedMemoryPref();
		setupNativeAppAllocatedMemoryPref();
		setupAgpsDataDownloadedPref();
		setupDayNightInfoPref();
	}

	private void setupOpenglRenderPref() {
		SwitchPreferenceEx useOpenglRender = findPreference(settings.USE_OPENGL_RENDER.getId());
		if (Version.isOpenGlAvailable(app)) {
			useOpenglRender.setDescription(getString(R.string.use_opengl_render_descr));
			useOpenglRender.setIconSpaceReserved(false);
		} else {
			useOpenglRender.setVisible(false);
		}
	}

	private void setupSafeModePref() {
		SwitchPreferenceEx safeMode = findPreference(settings.SAFE_MODE.getId());
		safeMode.setDescription(getString(R.string.safe_mode_description));
		safeMode.setIconSpaceReserved(false);
		// disable the switch if the library cannot be used
		if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
			safeMode.setEnabled(false);
			safeMode.setChecked(true);
		}
	}

	private void setupApproximationSafeModePref() {
		SwitchPreferenceEx safeMode = findPreference(settings.APPROX_SAFE_MODE.getId());
		safeMode.setDescription(getString(R.string.approx_safe_mode_description));
		safeMode.setIconSpaceReserved(false);
	}

	private void setupShowHeightmapspref() {
		SwitchPreferenceEx showHeightmaps = findPreference(settings.SHOW_HEIGHTMAPS.getId());
		showHeightmaps.setIconSpaceReserved(false);
	}

	private void setupSimulateYourLocationPref() {
		Preference simulateYourLocation = findPreference(SIMULATE_YOUR_LOCATION);
		simulateYourLocation.setIconSpaceReserved(false);
		OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
		updateSimulationTitle = new Runnable() {
			@Override
			public void run() {
				simulateYourLocation.setSummary(sim.isRouteAnimating() ?
						R.string.simulate_your_location_stop_descr : R.string.simulate_your_location_gpx_descr);
			}
		};
		updateSimulationTitle.run();
	}

	private void setupDebugRenderingInfoPref() {
		SwitchPreferenceEx debugRenderingInfo = findPreference(settings.DEBUG_RENDERING_INFO.getId());
		debugRenderingInfo.setDescription(getString(R.string.trace_rendering_descr));
		debugRenderingInfo.setIconSpaceReserved(false);
	}

	private void setupSimulateInitialStartupPref() {
		Preference simulateInitialStartup = findPreference(SIMULATE_INITIAL_STARTUP);
		simulateInitialStartup.setIconSpaceReserved(false);
	}

	private void setupShouldShowFreeVersionBannerPref() {
		SwitchPreferenceEx shouldShowFreeVersionBanner = findPreference(settings.SHOULD_SHOW_FREE_VERSION_BANNER.getId());
		shouldShowFreeVersionBanner.setDescription(getString(R.string.show_free_version_banner_description));
		shouldShowFreeVersionBanner.setIconSpaceReserved(false);
	}

	private void setupFullscreenMapDrawingModePref() {
		SwitchPreferenceEx fullscreenMapDrawingMode = findPreference(settings.TRANSPARENT_STATUS_BAR.getId());
		fullscreenMapDrawingMode.setDescription(getString(R.string.transparent_status_bar_descr));
		fullscreenMapDrawingMode.setIconSpaceReserved(false);
	}

	private void setupTestVoiceCommandsPref() {
		Preference testVoiceCommands = findPreference("test_voice_commands");
		testVoiceCommands.setIntent(new Intent(getActivity(), TestVoiceActivity.class));
		testVoiceCommands.setIconSpaceReserved(false);
	}

	private void setupLogcatBufferPref() {
		Preference logcatBuffer = findPreference("logcat_buffer");
		logcatBuffer.setIntent(new Intent(getActivity(), LogcatActivity.class));
		logcatBuffer.setIconSpaceReserved(false);
	}

	private void setupMemoryAllocatedForRoutingPref() {
		Preference preference = findPreference(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId());
		int value = settings.MEMORY_ALLOCATED_FOR_ROUTING.get();
		String description = getString(
				R.string.ltr_or_rtl_combine_via_space,
				String.valueOf(value),
				"MB");
		preference.setSummary(description);
		preference.setIconSpaceReserved(false);
	}

	private void setupGlobalAppAllocatedMemoryPref() {
		long javaAvailMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024L);
		long javaTotal = Runtime.getRuntime().totalMemory() / (1024 * 1024L);
		long dalvikSize = android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024L);

		Preference globalAppAllocatedMemory = findPreference("global_app_allocated_memory");
		globalAppAllocatedMemory.setSummary(getString(R.string.global_app_allocated_memory_descr, javaAvailMem, javaTotal, dalvikSize));
		globalAppAllocatedMemory.setIconSpaceReserved(false);
	}

	private void setupNativeAppAllocatedMemoryPref() {
//		ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
//		ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//		activityManager.getMemoryInfo(memoryInfo);
//		long totalSize = memoryInfo.availMem / (1024*1024l);
		Debug.MemoryInfo mem = new Debug.MemoryInfo();
		Debug.getMemoryInfo(mem);
		//setEnabled(false) creates bad readability on some devices
		//pref.setEnabled(false);
		Preference nativeAppAllocatedMemory = findPreference("native_app_allocated_memory");
		nativeAppAllocatedMemory.setIconSpaceReserved(false);
		nativeAppAllocatedMemory.setSummary(getString(R.string.native_app_allocated_memory_descr
				, mem.nativePrivateDirty / 1024, mem.dalvikPrivateDirty / 1024, mem.otherPrivateDirty / 1024
				, mem.nativePss / 1024, mem.dalvikPss / 1024, mem.otherPss / 1024));
	}

	private void setupAgpsDataDownloadedPref() {
		Preference agpsDataDownloaded = findPreference("agps_data_downloaded");
		agpsDataDownloaded.setSummary(getAgpsDataDownloadedSummary());
		agpsDataDownloaded.setIconSpaceReserved(false);
	}

	private String getAgpsDataDownloadedSummary() {
		if (settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get() != 0L) {
			return getString(R.string.agps_data_last_downloaded, DATE_FORMAT.format(settings.AGPS_DATA_LAST_TIME_DOWNLOADED.get()));
		} else {
			return getString(R.string.agps_data_last_downloaded, "--");
		}
	}

	private void setupDayNightInfoPref() {
		Preference dayNightInfo = findPreference("day_night_info");
		SunriseSunset sunriseSunset = app.getDaynightHelper().getSunriseSunset();
		String sunrise = sunriseSunset != null ? DATE_FORMAT.format(sunriseSunset.getSunrise()) : "null";
		String sunset = sunriseSunset != null ? DATE_FORMAT.format(sunriseSunset.getSunset()) : "null";
		dayNightInfo.setSummary(getString(R.string.day_night_info_description, sunrise, sunset));
		dayNightInfo.setIconSpaceReserved(false);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (SIMULATE_YOUR_LOCATION.equals(prefId)) {
			updateSimulationTitle.run();
			OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
			sim.startStopRouteAnimation(getActivity(), true, updateSimulationTitle);
			return true;
		} else if (SIMULATE_INITIAL_STARTUP.equals(prefId)) {
			app.getAppInitializer().resetFirstTimeRun();
			settings.FIRST_MAP_IS_DOWNLOADED.resetToDefault();
			settings.WEBGL_SUPPORTED.resetToDefault();
			settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.resetToDefault();

			MapillaryPlugin mapillaryPlugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
			if (mapillaryPlugin != null) {
				mapillaryPlugin.MAPILLARY_FIRST_DIALOG_SHOWN.resetToDefault();
			}

			app.showToastMessage(R.string.shared_string_ok);
			return true;
		} else if (AGPS_DATA_DOWNLOADED.equals(prefId)) {
			if (settings.isInternetConnectionAvailable(true)) {
				app.getLocationProvider().redownloadAGPS();
				preference.setSummary(getAgpsDataDownloadedSummary());
			}
			return true;
		} else if (settings.MEMORY_ALLOCATED_FOR_ROUTING.getId().equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				AllocatedRoutingMemoryBottomSheet.showInstance(fragmentManager, preference.getKey(), this, getSelectedAppMode());
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (prefId.equals(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId())) {
			applyPreference(settings.MEMORY_ALLOCATED_FOR_ROUTING.getId(), applyToAllProfiles, newValue);
			setupMemoryAllocatedForRoutingPref();
		} else {
			super.onApplyPreferenceChange(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (settings.SAFE_MODE.getId().equals(prefId) && newValue instanceof Boolean) {
			loadNativeLibrary();
			return true;
		} else if (settings.TRANSPARENT_STATUS_BAR.getId().equals(prefId) && newValue instanceof Boolean) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.restart();
			}
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	public void loadNativeLibrary() {
		FragmentActivity activity = getActivity();
		if (!NativeOsmandLibrary.isLoaded() && activity != null) {
			RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
			NativeLibraryLoadTask nativeLibraryLoadTask = new NativeLibraryLoadTask(activity, storage);
			nativeLibraryLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
}
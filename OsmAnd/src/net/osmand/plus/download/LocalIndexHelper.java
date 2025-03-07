package net.osmand.plus.download;


import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


public class LocalIndexHelper {

	private final OsmandApplication app;

	public LocalIndexHelper(OsmandApplication app) {
		this.app = app;
	}


	public String getInstalledDate(File f) {
		return android.text.format.DateFormat.getMediumDateFormat(app).format(getInstalationDate(f));
	}

	public Date getInstalationDate(File f) {
		long t = f.lastModified();
		return new Date(t);
	}

	public String getInstalledDate(long t, TimeZone timeZone) {
		return android.text.format.DateFormat.getMediumDateFormat(app).format(new Date(t));
	}

	public void updateDescription(@NonNull LocalIndexInfo info) {
		File f = new File(info.getPathToData());
		if (info.getType() == LocalIndexType.MAP_DATA) {
			Map<String, String> ifns = app.getResourceManager().getIndexFileNames();
			if (ifns.containsKey(info.getFileName())) {
				try {
					Date dt = app.getResourceManager().getDateFormat().parse(ifns.get(info.getFileName()));
					info.setDescription(getInstalledDate(dt.getTime(), null));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else {
				info.setDescription(getInstalledDate(f));
			}
		} else if (info.getType() == LocalIndexType.TILES_DATA) {
			ITileSource template;
			if (f.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(f)) {
				template = TileSourceManager.createTileSourceTemplate(new File(info.getPathToData()));
			} else if (f.isFile() && f.getName().endsWith(SQLiteTileSource.EXT)) {
				template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
			} else {
				return;
			}
			String descr = "";
			if (template.getExpirationTimeMinutes() >= 0) {
				descr += app.getString(R.string.local_index_tile_data_expire, String.valueOf(template.getExpirationTimeMinutes()));
			}
			info.setAttachedObject(template);
			info.setDescription(descr);
		} else if (info.getType() == LocalIndexType.SRTM_DATA) {
			info.setDescription(app.getString(R.string.download_srtm_maps));
		} else if (info.getType() == LocalIndexType.WIKI_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.TRAVEL_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.TTS_VOICE_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.DEACTIVATED) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.VOICE_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.FONT_DATA) {
			info.setDescription(getInstalledDate(f));
		}
	}

	private LocalIndexInfo getLocalIndexInfo(LocalIndexType type, String downloadName, boolean roadMap, boolean backuped) {

		File fileDir = null;
		String fileName = null;

		if (type == LocalIndexType.MAP_DATA) {
			if (!roadMap) {
				fileDir = app.getAppPath(IndexConstants.MAPS_PATH);
				fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
						+ IndexConstants.BINARY_MAP_INDEX_EXT;
			} else {
				fileDir = app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
				fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
						+ IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
			}
		} else if (type == LocalIndexType.SRTM_DATA) {
			fileDir = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
		} else if (type == LocalIndexType.WIKI_DATA) {
			fileDir = app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
		} else if (type == LocalIndexType.TRAVEL_DATA) {
			fileDir = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
		}

		if (backuped) {
			fileDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		}

		if (fileDir != null && fileName != null) {
			File f = new File(fileDir, fileName);
			if (f.exists()) {
				LocalIndexInfo info = new LocalIndexInfo(type, f, backuped, app);
				updateDescription(info);
				return info;
			}
		}

		return null;
	}

	public List<LocalIndexInfo> getLocalIndexInfos(String downloadName) {
		List<LocalIndexInfo> list = new ArrayList<>();
		LocalIndexInfo info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, true, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.SRTM_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.WIKI_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, true, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.SRTM_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.WIKI_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}

		return list;
	}

	public List<LocalIndexInfo> getLocalIndexData(boolean readFiles, boolean needDescription,
												  @Nullable AbstractLoadLocalIndexTask loadTask,
												  LocalIndexType... indexTypes) {
		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		Map<String, File> indexFiles = app.getResourceManager().getIndexFiles();
		List<LocalIndexInfo> result = new ArrayList<>();

		LocalIndexType[] types = indexTypes;
		if (types == null || types.length == 0) {
			types = LocalIndexType.values();
		}
		boolean voicesCollected = false;
		for (LocalIndexType type : types) {
			switch (type) {
				case SRTM_DATA:
					loadSrtmData(app.getAppPath(IndexConstants.SRTM_INDEX_DIR), result, false, readFiles,
							needDescription, indexFiles, loadTask);
					break;
				case WIKI_DATA:
					loadWikiData(app.getAppPath(IndexConstants.WIKI_INDEX_DIR), result, false, readFiles,
							needDescription, indexFiles, loadTask);
					break;
				case MAP_DATA:
					loadObfData(app.getAppPath(IndexConstants.MAPS_PATH), result, false, readFiles,
							needDescription, indexFileNames, indexFiles, loadTask);
					loadObfData(app.getAppPath(IndexConstants.ROADS_INDEX_DIR), result, false, readFiles,
							needDescription, indexFileNames, indexFiles, loadTask);
					break;
				case TILES_DATA:
					loadTilesData(app.getAppPath(IndexConstants.TILES_INDEX_DIR), result, false, needDescription, loadTask);
					loadTilesData(app.getAppPath(IndexConstants.HEIGHTMAP_INDEX_DIR), result, false, needDescription, loadTask);
					break;
				case TRAVEL_DATA:
					loadTravelData(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), result, false, readFiles,
							needDescription, indexFiles, loadTask);
					break;
				case TTS_VOICE_DATA:
				case VOICE_DATA:
					if (!voicesCollected) {
						loadVoiceData(app.getAppPath(IndexConstants.VOICE_INDEX_DIR), result, false, readFiles,
								needDescription, indexFiles, loadTask);
						voicesCollected = true;
					}
					break;
				case FONT_DATA:
					loadFontData(app.getAppPath(IndexConstants.FONT_INDEX_DIR), result, false, readFiles,
							needDescription, indexFiles, loadTask);
					break;
				case DEACTIVATED:
					loadObfData(app.getAppPath(IndexConstants.BACKUP_INDEX_DIR), result, true, readFiles,
							needDescription, indexFileNames, indexFiles, loadTask);
					break;
			}
		}
		return result;
	}

	public List<LocalIndexInfo> getLocalTravelFiles(AbstractLoadLocalIndexTask loadTask) {
		List<LocalIndexInfo> result = new ArrayList<>();
		loadTravelData(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), result, false, true, true,
				app.getResourceManager().getIndexFiles(), loadTask);
		return result;
	}

	public List<LocalIndexInfo> getLocalFullMaps(AbstractLoadLocalIndexTask loadTask) {
		List<LocalIndexInfo> result = new ArrayList<>();
		loadObfData(app.getAppPath(IndexConstants.MAPS_PATH), result, false, true, true,
				app.getResourceManager().getIndexFileNames(), app.getResourceManager().getIndexFiles(), loadTask);
		return result;
	}

	public void loadVoiceData(@NonNull File voiceDir, @NonNull List<LocalIndexInfo> result, boolean backup,
							  boolean readFiles, boolean needDescription, @NonNull Map<String, File> indexFiles,
							  @Nullable AbstractLoadLocalIndexTask loadTask) {
		if ((readFiles || backup) && voiceDir.canRead()) {
			File[] files = listFilesSorted(voiceDir);
			if (files.length > 0) {
				loadVoiceDataImpl(files, result, backup, needDescription, loadTask);
			}
		} else {
			List<File> voiceFiles = new ArrayList<>();
			for (File file : indexFiles.values()) {
				if (voiceDir.getPath().equals(file.getParent())) {
					voiceFiles.add(file);
				}
			}
			if (voiceFiles.size() > 0) {
				Collections.sort(voiceFiles);
				loadVoiceDataImpl(voiceFiles.toArray(new File[0]), result, backup, needDescription, loadTask);
			}
		}
	}

	private void loadVoiceDataImpl(@NonNull File[] voiceFiles, @NonNull List<LocalIndexInfo> result,
								   boolean backup, boolean needDescription, @Nullable AbstractLoadLocalIndexTask loadTask) {
		List<File> voiceFilesList = new ArrayList<>(Arrays.asList(voiceFiles));
		//First list TTS files, they are preferred
		Iterator<File> it = voiceFilesList.iterator();
		while (it.hasNext()) {
			File voiceFile = it.next();
			if (voiceFile.isDirectory() && (JsTtsCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, LocalIndexType.TTS_VOICE_DATA, result, backup, needDescription, loadTask);
				it.remove();
			}
		}
		//Now list recorded voices
		for (File voiceFile : voiceFilesList) {
			if (voiceFile.isDirectory() && (JsMediaCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, LocalIndexType.VOICE_DATA, result, backup, needDescription, loadTask);
			}
		}
	}

	private void loadFontData(@NonNull File fontPath, @NonNull List<LocalIndexInfo> result, boolean backup,
							  boolean readFiles, boolean needDescription, @NonNull Map<String, File> indexFiles,
							  @Nullable AbstractLoadLocalIndexTask loadTask) {
		loadDataImpl(fontPath, LocalIndexType.FONT_DATA, IndexConstants.FONT_INDEX_EXT,
				backup, readFiles, needDescription, result, indexFiles, loadTask);
	}

	private void loadTilesData(@NonNull File tilesPath, @NonNull List<LocalIndexInfo> result, boolean backup,
							   boolean needDescription, @Nullable AbstractLoadLocalIndexTask loadTask) {
		if (tilesPath.canRead()) {
			for (File tileFile : listFilesSorted(tilesPath)) {
				if (tileFile.isFile()) {
					String fileName = tileFile.getName();
					if (fileName.endsWith(SQLiteTileSource.EXT) || fileName.endsWith(IndexConstants.HEIGHTMAP_SQLITE_EXT)) {
						loadLocalData(tileFile, LocalIndexType.TILES_DATA, result, backup, needDescription, loadTask);
					}
				} else if (tileFile.isDirectory()) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup, app);

					if (!TileSourceManager.isTileSourceMetaInfoExist(tileFile)) {
						info.setCorrupted(true);
					}
					updateDescription(info);
					result.add(info);
					if (loadTask != null) {
						loadTask.loadFile(info);
					}
				}
			}
		}
	}

	private File[] listFilesSorted(File dir) {
		File[] listFiles = dir.listFiles();
		if (listFiles == null) {
			return new File[0];
		}
		Arrays.sort(listFiles);
		return listFiles;
	}

	private void loadSrtmData(@NonNull File dataPath, @NonNull List<LocalIndexInfo> result, boolean backup,
							  boolean readFiles, boolean needDescription, @NonNull Map<String, File> indexFiles,
							  @Nullable AbstractLoadLocalIndexTask loadTask) {
		loadDataImpl(dataPath, LocalIndexType.SRTM_DATA, IndexConstants.BINARY_MAP_INDEX_EXT,
				backup, readFiles, needDescription, result, indexFiles, loadTask);
	}

	private void loadWikiData(@NonNull File dataPath, @NonNull List<LocalIndexInfo> result, boolean backup,
							  boolean readFiles, boolean needDescription, @NonNull Map<String, File> indexFiles,
							  @Nullable AbstractLoadLocalIndexTask loadTask) {
		loadDataImpl(dataPath, LocalIndexType.WIKI_DATA, IndexConstants.BINARY_MAP_INDEX_EXT,
				backup, readFiles, needDescription, result, indexFiles, loadTask);
	}

	private void loadTravelData(@NonNull File dataPath, @NonNull List<LocalIndexInfo> result, boolean backup,
								boolean readFiles, boolean needDescription, @NonNull Map<String, File> indexFiles,
								@Nullable AbstractLoadLocalIndexTask loadTask) {
		loadDataImpl(dataPath, LocalIndexType.TRAVEL_DATA, IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT,
				backup, readFiles, needDescription, result, indexFiles, loadTask);
	}

	private void loadObfData(@NonNull File dataPath, @NonNull List<LocalIndexInfo> result, boolean backup,
							 boolean readFiles, boolean needDescription, @NonNull Map<String, String> indexFileNames,
							 @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask loadTask) {
		if ((readFiles || backup) && dataPath.canRead()) {
			for (File mapFile : listFilesSorted(dataPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					loadObfDataImpl(mapFile, result, backup, needDescription, indexFileNames, loadTask);
				}
			}
		} else {
			for (File file : indexFiles.values()) {
				if (file.isFile() && dataPath.getPath().equals(file.getParent())
						&& file.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					loadObfDataImpl(file, result, backup, needDescription, indexFileNames, loadTask);
				}
			}
		}
	}

	private void loadObfDataImpl(@NonNull File dataFile, @NonNull List<LocalIndexInfo> result, boolean backup,
								 boolean needDescription, @NonNull Map<String, String> indexFileNames,
								 @Nullable AbstractLoadLocalIndexTask loadTask) {
		String fileName = dataFile.getName();
		LocalIndexType lt = LocalIndexType.MAP_DATA;
		if (SrtmDownloadItem.isSrtmFile(fileName)) {
			lt = LocalIndexType.SRTM_DATA;
		} else if (fileName.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
			lt = LocalIndexType.WIKI_DATA;
		}
		LocalIndexInfo info = new LocalIndexInfo(lt, dataFile, backup, app);
		if (indexFileNames.containsKey(fileName) && !backup) {
			info.setLoaded(true);
		}
		if (needDescription) {
			updateDescription(info);
		}
		result.add(info);
		if (loadTask != null) {
			loadTask.loadFile(info);
		}
	}

	private void loadDataImpl(@NonNull File dataPath, @NonNull LocalIndexType indexType, @NonNull String fileExt,
							  boolean backup, boolean readFiles, boolean needDescription, @NonNull List<LocalIndexInfo> result,
							  @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask loadTask) {
		if ((readFiles || backup) && dataPath.canRead()) {
			for (File file : listFilesSorted(dataPath)) {
				if (file.isFile() && file.getName().endsWith(fileExt)) {
					loadLocalData(file, indexType, result, backup, needDescription, loadTask);
				}
			}
		} else {
			for (File file : indexFiles.values()) {
				if (file.isFile() && file.getPath().startsWith(dataPath.getPath())
						&& file.getName().endsWith(fileExt)) {
					loadLocalData(file, indexType, result, backup, needDescription, loadTask);
				}
			}
		}
	}

	private void loadLocalData(@NonNull File file, @NonNull LocalIndexType indexType,
							   @NonNull List<LocalIndexInfo> result, boolean backup, boolean needDescription,
							   @Nullable AbstractLoadLocalIndexTask loadTask) {
		LocalIndexInfo info = new LocalIndexInfo(indexType, file, backup, app);
		if (needDescription) {
			updateDescription(info);
		}
		result.add(info);
		if (loadTask != null) {
			loadTask.loadFile(info);
		}
	}

	public enum LocalIndexType {
		MAP_DATA(R.string.local_indexes_cat_map, R.drawable.ic_map, 10),
		TILES_DATA(R.string.local_indexes_cat_tile, R.drawable.ic_map, 60),
		SRTM_DATA(R.string.local_indexes_cat_srtm, R.drawable.ic_plugin_srtm, 40),
		WIKI_DATA(R.string.local_indexes_cat_wiki, R.drawable.ic_plugin_wikipedia, 50),
		TRAVEL_DATA(R.string.download_maps_travel, R.drawable.ic_plugin_wikipedia, 60),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up, 20),
		VOICE_DATA(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up, 30),
		FONT_DATA(R.string.fonts_header, R.drawable.ic_action_map_language, 35),
		DEACTIVATED(R.string.local_indexes_cat_backup, R.drawable.ic_type_archive, 1000);
//		AV_DATA(R.string.local_indexes_cat_av);

		@StringRes
		private final int resId;
		@DrawableRes
		private final int iconResource;
		private final int orderIndex;

		LocalIndexType(@StringRes int resId, @DrawableRes int iconResource, int orderIndex) {
			this.resId = resId;
			this.iconResource = iconResource;
			this.orderIndex = orderIndex;
		}

		public String getHumanString(Context ctx) {
			return ctx.getString(resId);
		}

		public int getIconResource() {
			return iconResource;
		}

		public int getOrderIndex(LocalIndexInfo info) {
			String fileName = info.getFileName();
			int index = info.getOriginalType().orderIndex;
			if (info.getType() == DEACTIVATED) {
				index += DEACTIVATED.orderIndex;
			}
			if (fileName.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				index++;
			}
			return index;
		}

		public String getBasename(LocalIndexInfo localIndexInfo) {
			String fileName = localIndexInfo.getFileName();
			if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
			}
			if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length());
			}
			if (localIndexInfo.getType() == TRAVEL_DATA &&
					fileName.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT.length());
			}
			if (this == VOICE_DATA) {
				int l = fileName.lastIndexOf('_');
				if (l == -1) {
					l = fileName.length();
				}
				return fileName.substring(0, l);
			}
			if (this == FONT_DATA) {
				int l = fileName.indexOf('.');
				if (l == -1) {
					l = fileName.length();
				}
				return fileName.substring(0, l).replace('_', ' ').replace('-', ' ');
			}
			int ls = fileName.lastIndexOf('_');
			if (ls >= 0) {
				return fileName.substring(0, ls);
			} else if (fileName.indexOf('.') > 0) {
				return fileName.substring(0, fileName.indexOf('.'));
			}
			return fileName;
		}
	}
}

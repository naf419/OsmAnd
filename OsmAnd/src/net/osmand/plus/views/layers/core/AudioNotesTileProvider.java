package net.osmand.plus.views.layers.core;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapTiledCollectionProvider;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapTiledCollectionPoint;
import net.osmand.core.jni.QListPointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_MapTiledCollectionProvider;
import net.osmand.plus.R;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioNotesTileProvider extends interface_MapTiledCollectionProvider {
    private final List<MapLayerData> mapLayerDataList = new ArrayList<>();
    private final Map<TypeNotes, Bitmap> bigBitmapCache = new ConcurrentHashMap<>();
    private Bitmap smallBitmap;
    private MapTiledCollectionProvider providerInstance;
    private final int baseOrder;
    private final Context ctx;
    private final TextRasterizer.Style textStyle;
    private final float density;

    public AudioNotesTileProvider(@NonNull Context context, int baseOrder, float density) {
        this.baseOrder = baseOrder;
        this.ctx = context;
        textStyle = new TextRasterizer.Style();
        this.density = density;
    }

    public void drawSymbols(@NonNull MapRendererView mapRenderer) {
        if (providerInstance == null) {
            providerInstance = instantiateProxy();
        }
        mapRenderer.addSymbolsProvider(providerInstance);
    }

    public void clearSymbols(@NonNull MapRendererView mapRenderer) {
        if (providerInstance != null) {
            mapRenderer.removeSymbolsProvider(providerInstance);
            providerInstance = null;
        }
    }

    @Override
    public int getBaseOrder() {
        return baseOrder;
    }

    @Override
    public QListPointI getHiddenPoints() {
        return new QListPointI();
    }

    @Override
    public boolean shouldShowCaptions() {
        return false;
    }

    @Override
    public TextRasterizer.Style getCaptionStyle() {
        return textStyle;
    }

    @Override
    public double getCaptionTopSpace() {
        return -4.0 * density;
    }

    @Override
    public float getReferenceTileSizeOnScreenInPixels() {
        return 256;
    }

    @Override
    public double getScale() {
        return 1.0d;
    }

    @Override
    public PointI getPoint31(int index) {
        MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
        return data != null ? data.point : new PointI(0, 0);
    }

    @Override
    public int getPointsCount() {
        return mapLayerDataList.size();
    }

    @Override
    public QListMapTiledCollectionPoint getTilePoints(TileId tileId, ZoomLevel zoom) {
        return new QListMapTiledCollectionPoint();
    }

    @Override
    public SWIGTYPE_p_sk_spT_SkImage_const_t getImageBitmap(int index, boolean isFullSize) {
        MapLayerData data = index < mapLayerDataList.size() ? mapLayerDataList.get(index) : null;
        if (data == null) {
            return SwigUtilities.nullSkImage();
        }
        Bitmap bitmap;
        if (isFullSize) {
            bitmap = bigBitmapCache.get(data.type);
            if (bitmap == null) {
                int iconId;
                if (data.type == TypeNotes.PHOTO) {
                    iconId = R.drawable.mx_special_photo_camera;
                } else if (data.type == TypeNotes.AUDIO) {
                    iconId = R.drawable.mx_special_microphone;
                } else {
                    iconId = R.drawable.mx_special_video_camera;
                }
                PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx,
                        ContextCompat.getColor(ctx, R.color.audio_video_icon_color), true, iconId);
                pointImageDrawable.setAlpha(0.8f);
                bitmap = pointImageDrawable.getBigMergedBitmap(data.textScale, false);
                bigBitmapCache.put(data.type, bitmap);
            }
        } else {
            if (smallBitmap == null) {
                PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx,
                        ContextCompat.getColor(ctx, R.color.audio_video_icon_color), true);
                pointImageDrawable.setAlpha(0.8f);
                smallBitmap = pointImageDrawable.getSmallMergedBitmap(data.textScale);
            }
            bitmap = smallBitmap;
        }
        return bitmap != null ? NativeUtilities.createSkImageFromBitmap(bitmap) : SwigUtilities.nullSkImage();
    }

    @Override
    public ZoomLevel getMinZoom() {
        return ZoomLevel.ZoomLevel10;
    }

    @Override
    public ZoomLevel getMaxZoom() {
        return ZoomLevel.MaxZoomLevel;
    }

    public void addToData(@NonNull AudioVideoNotesPlugin.Recording recording, float textScale) {
        if (providerInstance != null) {
            throw new IllegalStateException("Provider already instantiated. Data cannot be modified at this stage.");
        }
        mapLayerDataList.add(new MapLayerData(recording, textScale));
    }

    private static class MapLayerData {
        TypeNotes type;
        PointI point;
        float textScale;
        MapLayerData(@NonNull AudioVideoNotesPlugin.Recording recording, float textScale) {
            int x = MapUtils.get31TileNumberX(recording.getLongitude());
            int y = MapUtils.get31TileNumberY(recording.getLatitude());
            point = new PointI(x, y);
            if (recording.isPhoto()) {
                type = TypeNotes.PHOTO;
            } else if (recording.isAudio()) {
                type = TypeNotes.AUDIO;
            } else {
                type = TypeNotes.VIDEO;
            }
            this.textScale = textScale;
        }
    }

    private enum TypeNotes {
        PHOTO,
        AUDIO,
        VIDEO
    }

    @Override
    public MapMarker.PinIconVerticalAlignment getPinIconVerticalAlignment() {
        return MapMarker.PinIconVerticalAlignment.CenterVertical;
    }

    @Override
    public MapMarker.PinIconHorisontalAlignment getPinIconHorisontalAlignment() {
        return MapMarker.PinIconHorisontalAlignment.CenterHorizontal;
    }
}

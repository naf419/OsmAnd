package net.osmand.plus.plugins.audionotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class TakeAudioNoteAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(8,
			"audio.note", TakeAudioNoteAction.class).
			nameRes(R.string.quick_action_take_audio_note).iconRes(R.drawable.ic_action_micro_dark).nonEditable().
			category(QuickActionType.CREATE_CATEGORY);

	public TakeAudioNoteAction() {
		super(TYPE);
	}

	public TakeAudioNoteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null)
			plugin.recordAudio(latLon.getLatitude(), latLon.getLongitude(), mapActivity);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_take_audio_note_descr);
		parent.addView(view);
	}
}

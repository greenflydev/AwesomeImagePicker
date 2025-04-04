package in.myinnos.awesomeimagepicker.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import in.myinnos.awesomeimagepicker.models.Media;
import in.myinnos.awesomeimagepicker.models.MediaType;

/**
 * Created by MyInnos on 03-11-2016.
 */
public class ConstantsCustomGallery {
    public static final int PERMISSION_REQUEST_CODE = 1000;

    /*
     * Fake albums for showing all photos or all videos.
     */
    public static final long ALL_MEDIA_ALBUM_ID = -1001;

    /*
     * When looking at mixed media, at the top will be tabs to select just
     * photos or videos. These are the tab positions.
     */
    public static final int TAB_ALL_MEDIA_POSITION = 0;
    public static final int TAB_PHOTOS_POSITION = 1;
    public static final int TAB_VIDEOS_POSITION = 2;

    /*
     * The parent application may need to know when certain events happen. There will be a local
     * broadcast with action BROADCAST_EVENT and BROADCAST_EVENT_TYPE. This will let the parent
     * application know what event has triggered.
     *
     * Event type: BROADCAST_EVENT_ALBUM_SELECTED = true
     *  - Property: INTENT_EXTRA_ALBUM_ID = The album id selected
     *
     * Event type: BROADCAST_EVENT_FILTER_BY_TYPE = true
     *  - Property: INTENT_EXTRA_FILTER_BY_TYPE = the tab position for all media, photos, or videos
     *
     * Event type: BROADCAST_EVENT_LONG_PRESS_FTUE
     *  - Property: None
     *
     * Event type: BROADCAST_EVENT_MEDIA_LOADED
     * - Property: INTENT_EXTRA_MEDIA_COUNT = How many media items were loaded
     * - Property: INTENT_EXTRA_LOADING_TIME = Time it took to load
     */
    public static final String BROADCAST_EVENT = "BROADCAST_EVENT";
    public static final String BROADCAST_EVENT_ALBUM_SELECTED = "BROADCAST_EVENT_ALBUM_SELECTED";
    public static final String BROADCAST_EVENT_FILTER_BY_TYPE = "BROADCAST_EVENT_FILTER_BY_TYPE";
    public static final String BROADCAST_EVENT_LONG_PRESS = "BROADCAST_EVENT_LONG_PRESS";
    public static final String BROADCAST_EVENT_LONG_PRESS_FTUE = "BROADCAST_EVENT_LONG_PRESS_FTUE";
    public static final String BROADCAST_EVENT_MEDIA_LOADED = "BROADCAST_EVENT_MEDIA_LOADED";
    public static final String BROADCAST_EVENT_MANAGE_STORAGE = "BROADCAST_EVENT_MANAGE_STORAGE";
    public static final String BROADCAST_EVENT_OPEN_SETTINGS = "BROADCAST_EVENT_OPEN_SETTINGS";

    public static final String INTENT_EXTRA_ALBUM_ID = "albumId";
    public static final String INTENT_EXTRA_MEDIA = "media";
    public static final String INTENT_EXTRA_LIMIT = "limit";
    public static final String INTENT_EXTRA_MEDIATYPE = "mediaType";
    public static final String INTENT_EXTRA_FILTER_BY_TYPE = "filterByType";
    public static final String INTENT_EXTRA_MEDIA_COUNT = "mediaCount";
    public static final String INTENT_EXTRA_LOADING_TIME = "loadingTime";
    public static final int DEFAULT_LIMIT = 10;

    public static final String SP_NAME_MAIN = "SP_NAME_MAIN";
    public static final String SP_PREVIOUSLY_SELECTED_IDS = "SP_PREVIOUSLY_SELECTED_IDS";
    public static final String SP_LONG_PRESS_FTUE_VIEWED = "SP_LONG_PRESS_FTUE_VIEWED";

    /*
     * Holds a list of ids that the user has already selected to upload. So if the user comes back
     * while sending several batches they will know where they left off.
     */
    private static Set<String> previouslySelectedIds = null;

    public static final Map<String, Media> currentlySelectedMap = new HashMap<>();

    /*
     * Maximum number of media items that can be selected at a time
     */
    public static int limit = DEFAULT_LIMIT;

    //Type of media
    public static MediaType mediaType;

    /*
     * This will load the saved previously selected ids from shared preferences.
     * If they are already loaded then it will return it.
     */
    public static Set<String> getPreviouslySelectedIds(Context context) {
        if (previouslySelectedIds != null) {
            return previouslySelectedIds;
        }

        previouslySelectedIds = getStringSetFromMainSP(context, SP_PREVIOUSLY_SELECTED_IDS);
        if (previouslySelectedIds == null) {
            previouslySelectedIds = new HashSet<>();
        }
        return previouslySelectedIds;
    }

    /*
     * This will save the set of previously selected ids
     */
    public static void savePreviouslySelectedIds(Context context) {
        saveStringSetToMainSP(context, SP_PREVIOUSLY_SELECTED_IDS, previouslySelectedIds);
    }

    private static void saveStringSetToMainSP(Context context, String key, Set<String> value) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            sp.edit().putStringSet(key, value).commit();
        }
    }

    private static Set<String> getStringSetFromMainSP(Context context, String key) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            return sp.getStringSet(key, new HashSet<>());
        }
        return new HashSet<>();
    }

    public static void saveBooleanToMainSP(Context context, String key, boolean value) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            sp.edit().putBoolean(key, value).commit();
        }
    }

    public static boolean getBooleanFromMainSP(Context context, String key) {
        if (context != null) {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME_MAIN, 0);
            return sp.getBoolean(key, false);
        }
        return false;
    }
}

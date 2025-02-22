package org.videolan.tools

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.tools.Settings.init

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
object Settings : SingletonHolder<SharedPreferences, Context>({ init(it.applicationContext) }) {

    var showVideoThumbs = true
    var tvUI = false
    var listTitleEllipsize = 0
    var overrideTvUI = false
    var videoHudDelay = 2
    var includeMissing = true
    var showHeaders = true
    var showAudioTrackInfo = false
    lateinit var device : DeviceInfo
        private set

    fun init(context: Context) : SharedPreferences{
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        showVideoThumbs = prefs.getBoolean(SHOW_VIDEO_THUMBNAILS, false)
        tvUI = prefs.getBoolean(PREF_TV_UI, false)
        listTitleEllipsize = prefs.getString(LIST_TITLE_ELLIPSIZE, "0")?.toInt() ?: 0
        videoHudDelay = prefs.getString(VIDEO_HUD_TIMEOUT, "2")?.toInt() ?: 2
        device = DeviceInfo(context)
        includeMissing = prefs.getBoolean(KEY_INCLUDE_MISSING, true)
        showHeaders = prefs.getBoolean(KEY_SHOW_HEADERS, true)
        showAudioTrackInfo = prefs.getBoolean(KEY_SHOW_TRACK_INFO, false)
        return prefs
    }

    val showTvUi : Boolean
        get() = !overrideTvUI && device.isTv
}

const val KEY_CURRENT_SETTINGS_VERSION = "current_settings_version"

// Keys
const val KEY_ARTISTS_SHOW_ALL = "artists_show_all"
const val KEY_SHOW_HEADERS = "show_headers"
const val KEY_APP_THEME = "app_theme"
const val KEY_BLACK_THEME = "enable_black_theme"
const val KEY_DAYNIGHT = "daynight"
const val SHOW_VIDEO_THUMBNAILS = "show_video_thumbnails"
const val KEY_VIDEO_CONFIRM_RESUME = "video_confirm_resume"
const val KEY_MEDIALIBRARY_AUTO_RESCAN = "auto_rescan"
const val KEY_TV_ONBOARDING_DONE = "key_tv_onboarding_done"
const val KEY_INCLUDE_MISSING = "include_missing"

//UI
const val LIST_TITLE_ELLIPSIZE = "list_title_ellipsize"


// AudioPlayer
const val AUDIO_SHUFFLING = "audio_shuffling"
const val MEDIA_SHUFFLING = "media_shuffling"
const val POSITION_IN_SONG = "position_in_song"
const val POSITION_IN_MEDIA = "position_in_media"
const val POSITION_IN_AUDIO_LIST = "position_in_audio_list"
const val POSITION_IN_MEDIA_LIST = "position_in_media_list"
const val SHOW_REMAINING_TIME = "show_remaining_time"
const val PREF_PLAYLIST_TIPS_SHOWN = "playlist_tips_shown"
const val PREF_AUDIOPLAYER_TIPS_SHOWN = "audioplayer_tips_shown"
const val KEY_MEDIALIBRARY_SCAN = "ml_scan"
const val KEY_SHOW_TRACK_INFO = "show_track_info"
const val ML_SCAN_ON = 0
const val ML_SCAN_OFF = 1

//Tips

const val PREF_TIPS_SHOWN = "video_player_tips_shown"

const val PREF_TV_UI = "tv_ui"
const val FORCE_PLAY_ALL = "force_play_all"

const val SCREEN_ORIENTATION = "screen_orientation"
const val VIDEO_RESUME_TIME = "VideoResumeTime"
const val VIDEO_RESUME_URI = "VideoResumeUri"
const val ENABLE_SEEK_BUTTONS = "enable_seek_buttons"
const val ENABLE_DOUBLE_TAP_SEEK = "enable_double_tap_seek"
const val ENABLE_VOLUME_GESTURE = "enable_volume_gesture"
const val ENABLE_BRIGHTNESS_GESTURE = "enable_brightness_gesture"
const val SAVE_BRIGHTNESS = "save_brightness"
const val BRIGHTNESS_VALUE = "brightness_value"
const val POPUP_KEEPSCREEN = "popup_keepscreen"
const val POPUP_FORCE_LEGACY = "popup_force_legacy"
const val LOCK_USE_SENSOR = "lock_use_sensor"

const val VIDEO_PAUSED = "VideoPaused"
const val VIDEO_SPEED = "VideoSpeed"
const val VIDEO_RATIO = "video_ratio"
const val LOGIN_STORE = "store_login"
const val LOGIN_USER_NAME = "login_user_name"
const val LOGIN_PASSWORD = "login_password"
const val KEY_PLAYBACK_RATE = "playback_rate"
const val KEY_PLAYBACK_RATE_VIDEO = "playback_rate_video"
const val KEY_PLAYBACK_SPEED_PERSIST = "playback_speed"
const val KEY_PLAYBACK_SPEED_PERSIST_VIDEO = "playback_speed_video"
const val KEY_VIDEO_APP_SWITCH = "video_action_switch"
const val VIDEO_TRANSITION_SHOW = "video_transition_show"
const val VIDEO_HUD_TIMEOUT = "video_hud_timeout"
const val RESULT_RESCAN = Activity.RESULT_FIRST_USER + 1
const val RESULT_RESTART = Activity.RESULT_FIRST_USER + 2
const val RESULT_RESTART_APP = Activity.RESULT_FIRST_USER + 3
const val RESULT_UPDATE_SEEN_MEDIA = Activity.RESULT_FIRST_USER + 4
const val RESULT_UPDATE_ARTISTS = Activity.RESULT_FIRST_USER + 5

const val BETA_WELCOME = "beta_welcome"
const val CRASH_DONT_ASK_AGAIN = "crash_dont_ask_again"

const val PLAYBACK_HISTORY = "playback_history"
const val RESUME_PLAYBACK = "resume_playback"
const val AUDIO_DUCKING = "audio_ducking"

const val AUDIO_DELAY_GLOBAL = "audio_delay_global"
const val AUDIO_PLAY_PROGRESS_MODE = "audio_play_progress_mode"
const val AUDIO_STOP_AFTER = "audio_stop_after"

const val LAST_LOCK_ORIENTATION = "last_lock_orientation"
const val INITIAL_PERMISSION_ASKED = "initial_permission_asked"

class DeviceInfo(context: Context) {
    val pm = context.packageManager
    val tm = context.getSystemService<TelephonyManager>()!!
    val isPhone = tm.phoneType != TelephonyManager.PHONE_TYPE_NONE
    val hasTsp = pm.hasSystemFeature("android.hardware.touchscreen")
    val isAndroidTv = false
    val watchDevices = isAndroidTv && Build.MODEL.startsWith("Bouygtel")
    val isChromeBook = pm.hasSystemFeature("org.chromium.arc.device_management")
    val isTv = isAndroidTv || !isChromeBook && !hasTsp
    val isAmazon = "Amazon" == Build.MANUFACTURER
    val hasPiP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pm.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isAndroidTv
    val pipAllowed = hasPiP || hasTsp && Build.VERSION.SDK_INT < Build.VERSION_CODES.O
}

fun SharedPreferences.putSingle(key: String, value: Any) {
    when(value) {
        is Boolean -> edit { putBoolean(key, value) }
        is Int -> edit { putInt(key, value) }
        is Float -> edit { putFloat(key, value) }
        is Long -> edit { putLong(key, value) }
        is String -> edit { putString(key, value) }
        is List<*> -> edit { putStringSet(key, value.toSet() as Set<String>) }
        else -> throw IllegalArgumentException("value class is invalid!")
    }
}

#include <jni.h>
#include <fluidsynth.h>
#include <android/log.h>
#include <memory>
#include <unordered_map>

#define LOG_TAG "FluidSynthJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state management
static std::unordered_map<jlong, fluid_synth_t*> synth_instances;
static std::unordered_map<jlong, fluid_settings_t*> settings_instances;
static jlong next_synth_id = 1;

extern "C" {

// Create a new FluidSynth synthesizer
JNIEXPORT jlong JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_createSynth(JNIEnv* env, jclass clazz) {
    try {
        // Create settings
        fluid_settings_t* settings = new_fluid_settings();
        if (!settings) {
            LOGE("Failed to create FluidSynth settings");
            return -1;
        }

        // Configure settings
        fluid_settings_setstr(settings, "synth.audio.driver", "oboe");
        fluid_settings_setint(settings, "synth.polyphony", 256);
        fluid_settings_setint(settings, "synth.midi-channels", 16);

        // Create synthesizer
        fluid_synth_t* synth = new_fluid_synth(settings);
        if (!synth) {
            LOGE("Failed to create FluidSynth synthesizer");
            delete_fluid_settings(settings);
            return -1;
        }

        // Store instances and return handle
        jlong synth_id = next_synth_id++;
        synth_instances[synth_id] = synth;
        settings_instances[synth_id] = settings;

        LOGI("Created synthesizer with ID: %lld", synth_id);
        return synth_id;
    } catch (const std::exception& e) {
        LOGE("Exception in createSynth: %s", e.what());
        return -1;
    }
}

// Destroy a FluidSynth synthesizer
JNIEXPORT void JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_destroySynth(JNIEnv* env, jclass clazz, jlong synth_handle) {
    try {
        auto synth_it = synth_instances.find(synth_handle);
        auto settings_it = settings_instances.find(synth_handle);

        if (synth_it != synth_instances.end()) {
            delete_fluid_synth(synth_it->second);
            synth_instances.erase(synth_it);
        }

        if (settings_it != settings_instances.end()) {
            delete_fluid_settings(settings_it->second);
            settings_instances.erase(settings_it);
        }

        LOGI("Destroyed synthesizer with ID: %lld", synth_handle);
    } catch (const std::exception& e) {
        LOGE("Exception in destroySynth: %s", e.what());
    }
}

// Load a SoundFont file
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_loadSoundFont(JNIEnv* env, jclass clazz, jlong synth_handle, jstring file_path) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return -1;
        }

        const char* path = env->GetStringUTFChars(file_path, nullptr);
        int sfont_id = fluid_synth_sfload(it->second, path, 1);
        env->ReleaseStringUTFChars(file_path, path);

        if (sfont_id == FLUID_FAILED) {
            LOGE("Failed to load SoundFont: %s", path);
            return -1;
        }

        LOGI("Loaded SoundFont with ID: %d", sfont_id);
        return sfont_id;
    } catch (const std::exception& e) {
        LOGE("Exception in loadSoundFont: %s", e.what());
        return -1;
    }
}

// Play a note
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_noteOn(JNIEnv* env, jclass clazz, jlong synth_handle, 
                                                  jint channel, jint note, jint velocity) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return FLUID_FAILED;
        }

        int result = fluid_synth_noteon(it->second, channel, note, velocity);
        if (result != FLUID_OK) {
            LOGE("Failed to play note: channel=%d, note=%d, velocity=%d", channel, note, velocity);
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in noteOn: %s", e.what());
        return FLUID_FAILED;
    }
}

// Stop a note
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_noteOff(JNIEnv* env, jclass clazz, jlong synth_handle, 
                                                   jint channel, jint note) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return FLUID_FAILED;
        }

        int result = fluid_synth_noteoff(it->second, channel, note);
        if (result != FLUID_OK) {
            LOGE("Failed to stop note: channel=%d, note=%d", channel, note);
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in noteOff: %s", e.what());
        return FLUID_FAILED;
    }
}

// Set program (instrument)
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_programChange(JNIEnv* env, jclass clazz, jlong synth_handle, 
                                                        jint channel, jint program) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return FLUID_FAILED;
        }

        int result = fluid_synth_program_change(it->second, channel, program);
        if (result != FLUID_OK) {
            LOGE("Failed to change program: channel=%d, program=%d", channel, program);
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in programChange: %s", e.what());
        return FLUID_FAILED;
    }
}

// Set channel volume (CC 7)
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_setChannelVolume(JNIEnv* env, jclass clazz, jlong synth_handle, 
                                                           jint channel, jint volume) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return FLUID_FAILED;
        }

        int result = fluid_synth_cc(it->second, channel, 7, volume);
        if (result != FLUID_OK) {
            LOGE("Failed to set channel volume: channel=%d, volume=%d", channel, volume);
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in setChannelVolume: %s", e.what());
        return FLUID_FAILED;
    }
}

// Generic MIDI CC control
JNIEXPORT jint JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_controlChange(JNIEnv* env, jclass clazz, jlong synth_handle, 
                                                        jint channel, jint controller, jint value) {
    try {
        auto it = synth_instances.find(synth_handle);
        if (it == synth_instances.end()) {
            LOGE("Synthesizer with ID %lld not found", synth_handle);
            return FLUID_FAILED;
        }

        int result = fluid_synth_cc(it->second, channel, controller, value);
        if (result != FLUID_OK) {
            LOGE("Failed to send CC: channel=%d, controller=%d, value=%d", channel, controller, value);
        }
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in controlChange: %s", e.what());
        return FLUID_FAILED;
    }
}

// Get synthesizer version
JNIEXPORT jstring JNICALL
Java_org_tetawex_cmpsftdemo_FluidSynthJNI_getVersion(JNIEnv* env, jclass clazz) {
    char version[256];
    snprintf(version, sizeof(version), "FluidSynth %s", FLUIDSYNTH_VERSION);
    return env->NewStringUTF(version);
}

} // extern "C"

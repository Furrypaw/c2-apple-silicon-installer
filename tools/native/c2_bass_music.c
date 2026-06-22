#include <dlfcn.h>
#include <jni.h>
#include <stdint.h>

typedef uint32_t DWORD;
typedef uint64_t QWORD;
typedef int BOOL;
typedef DWORD HMUSIC;

#define BASS_SAMPLE_LOOP 4
#define BASS_MUSIC_PRESCAN 0x20000
#define BASS_MUSIC_RAMP 0x200
#define BASS_ATTRIB_VOL 2
#define BASS_ACTIVE_PLAYING 1
#define BASS_ERROR_ALREADY 14

static void *bass_lib;
static BOOL (*p_BASS_Init)(int, DWORD, DWORD, void *, const void *);
static BOOL (*p_BASS_Free)(void);
static int (*p_BASS_ErrorGetCode)(void);
static HMUSIC (*p_BASS_MusicLoad)(DWORD, const void *, QWORD, DWORD, DWORD, DWORD);
static BOOL (*p_BASS_MusicFree)(HMUSIC);
static BOOL (*p_BASS_ChannelPlay)(DWORD, BOOL);
static BOOL (*p_BASS_ChannelStop)(DWORD);
static BOOL (*p_BASS_ChannelSetAttribute)(DWORD, DWORD, float);
static DWORD (*p_BASS_ChannelIsActive)(DWORD);

static int load_symbol(void **target, const char *name) {
    *target = dlsym(bass_lib, name);
    return *target != 0;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativeInit(JNIEnv *env, jclass cls, jstring bassPath) {
    (void)cls;
    const char *path = (*env)->GetStringUTFChars(env, bassPath, 0);
    if (!path) {
        return JNI_FALSE;
    }
    bass_lib = dlopen(path, RTLD_NOW | RTLD_LOCAL);
    (*env)->ReleaseStringUTFChars(env, bassPath, path);
    if (!bass_lib) {
        return JNI_FALSE;
    }
    if (!load_symbol((void **)&p_BASS_Init, "BASS_Init")
            || !load_symbol((void **)&p_BASS_Free, "BASS_Free")
            || !load_symbol((void **)&p_BASS_ErrorGetCode, "BASS_ErrorGetCode")
            || !load_symbol((void **)&p_BASS_MusicLoad, "BASS_MusicLoad")
            || !load_symbol((void **)&p_BASS_MusicFree, "BASS_MusicFree")
            || !load_symbol((void **)&p_BASS_ChannelPlay, "BASS_ChannelPlay")
            || !load_symbol((void **)&p_BASS_ChannelStop, "BASS_ChannelStop")
            || !load_symbol((void **)&p_BASS_ChannelSetAttribute, "BASS_ChannelSetAttribute")
            || !load_symbol((void **)&p_BASS_ChannelIsActive, "BASS_ChannelIsActive")) {
        dlclose(bass_lib);
        bass_lib = 0;
        return JNI_FALSE;
    }
    if (p_BASS_Init(-1, 44100, 0, 0, 0)) {
        return JNI_TRUE;
    }
    return p_BASS_ErrorGetCode() == BASS_ERROR_ALREADY ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_C2BassMusic_nativeLoadMusic(JNIEnv *env, jclass cls, jstring path, jboolean loop) {
    (void)cls;
    if (!p_BASS_MusicLoad) {
        return 0;
    }
    const char *file = (*env)->GetStringUTFChars(env, path, 0);
    if (!file) {
        return 0;
    }
    DWORD flags = BASS_MUSIC_RAMP | BASS_MUSIC_PRESCAN;
    if (loop) {
        flags |= BASS_SAMPLE_LOOP;
    }
    HMUSIC handle = p_BASS_MusicLoad(0, file, 0, 0, flags, 0);
    (*env)->ReleaseStringUTFChars(env, path, file);
    return (jint)handle;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativePlay(JNIEnv *env, jclass cls, jint handle, jboolean restart) {
    (void)env;
    (void)cls;
    return p_BASS_ChannelPlay && p_BASS_ChannelPlay((DWORD)handle, restart) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativeStop(JNIEnv *env, jclass cls, jint handle) {
    (void)env;
    (void)cls;
    return p_BASS_ChannelStop && p_BASS_ChannelStop((DWORD)handle) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativeFreeMusic(JNIEnv *env, jclass cls, jint handle) {
    (void)env;
    (void)cls;
    return p_BASS_MusicFree && p_BASS_MusicFree((HMUSIC)handle) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativeSetVolume(JNIEnv *env, jclass cls, jint handle, jfloat volume) {
    (void)env;
    (void)cls;
    return p_BASS_ChannelSetAttribute && p_BASS_ChannelSetAttribute((DWORD)handle, BASS_ATTRIB_VOL, volume) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_C2BassMusic_nativeIsActive(JNIEnv *env, jclass cls, jint handle) {
    (void)env;
    (void)cls;
    return p_BASS_ChannelIsActive && p_BASS_ChannelIsActive((DWORD)handle) == BASS_ACTIVE_PLAYING ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_C2BassMusic_nativeShutdown(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    if (p_BASS_Free) {
        p_BASS_Free();
    }
    if (bass_lib) {
        dlclose(bass_lib);
    }
    bass_lib = 0;
    p_BASS_Init = 0;
    p_BASS_Free = 0;
    p_BASS_ErrorGetCode = 0;
    p_BASS_MusicLoad = 0;
    p_BASS_MusicFree = 0;
    p_BASS_ChannelPlay = 0;
    p_BASS_ChannelStop = 0;
    p_BASS_ChannelSetAttribute = 0;
    p_BASS_ChannelIsActive = 0;
}

JNIEXPORT jint JNICALL Java_C2BassMusic_nativeError(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    return p_BASS_ErrorGetCode ? p_BASS_ErrorGetCode() : -1;
}

#include "rime_jni.h"
#include <android/log.h>
#include <string>
#include <vector>
#include <map>

#define LOG_TAG "AegisInput-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// TODO: Replace with actual librime headers when available
// #include <rime_api.h>

namespace {
    // Stub session state for development without librime
    struct StubSession {
        std::string composing;
        std::vector<std::string> candidates;
    };

    std::map<jlong, StubSession> sessions;
    jlong nextSessionId = 1;
    bool initialized = false;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeInitialize(JNIEnv *env, jobject thiz,
                                                       jstring data_dir, jstring shared_dir) {
    const char *dataPath = env->GetStringUTFChars(data_dir, nullptr);
    const char *sharedPath = env->GetStringUTFChars(shared_dir, nullptr);

    LOGI("Initializing RIME engine: data=%s, shared=%s", dataPath, sharedPath);

    // When librime is integrated:
    // RIME_STRUCT(RimeTraits, traits);
    // traits.shared_data_dir = sharedPath;
    // traits.user_data_dir = dataPath;
    // traits.app_name = "aegisinput";
    // RimeSetup(&traits);
    // RimeInitialize(&traits);

    initialized = true;

    env->ReleaseStringUTFChars(data_dir, dataPath);
    env->ReleaseStringUTFChars(shared_dir, sharedPath);
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeShutdown(JNIEnv *env, jobject thiz) {
    LOGI("Shutting down RIME engine");
    sessions.clear();
    initialized = false;
    // RimeFinalize();
}

JNIEXPORT jlong JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCreateSession(JNIEnv *env, jobject thiz) {
    jlong id = nextSessionId++;
    sessions[id] = StubSession{};
    LOGI("Created session %lld", (long long)id);
    return id;
    // return (jlong) RimeCreateSession();
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeDestroySession(JNIEnv *env, jobject thiz,
                                                           jlong session_id) {
    sessions.erase(session_id);
    LOGI("Destroyed session %lld", (long long)session_id);
    // RimeDestroySession((RimeSessionId) session_id);
}

JNIEXPORT jboolean JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeProcessKey(JNIEnv *env, jobject thiz,
                                                       jlong session_id, jstring key) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return JNI_FALSE;

    const char *keyStr = env->GetStringUTFChars(key, nullptr);
    std::string k(keyStr);
    env->ReleaseStringUTFChars(key, keyStr);

    // Stub: append key to composing text
    if (k == "BackSpace") {
        if (!it->second.composing.empty()) {
            it->second.composing.pop_back();
        }
    } else {
        it->second.composing += k;
    }

    // Stub candidates
    it->second.candidates.clear();
    if (!it->second.composing.empty()) {
        it->second.candidates.push_back(it->second.composing);
    }

    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetComposingText(JNIEnv *env, jobject thiz,
                                                              jlong session_id) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return env->NewStringUTF("");
    return env->NewStringUTF(it->second.composing.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeGetCandidates(JNIEnv *env, jobject thiz,
                                                          jlong session_id) {
    auto it = sessions.find(session_id);
    jclass stringClass = env->FindClass("java/lang/String");

    if (it == sessions.end() || it->second.candidates.empty()) {
        return env->NewObjectArray(0, stringClass, nullptr);
    }

    auto &candidates = it->second.candidates;
    jobjectArray result = env->NewObjectArray(
        static_cast<jsize>(candidates.size()), stringClass, nullptr);

    for (size_t i = 0; i < candidates.size(); i++) {
        env->SetObjectArrayElement(result, static_cast<jsize>(i),
                                   env->NewStringUTF(candidates[i].c_str()));
    }

    return result;
}

JNIEXPORT jstring JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeCommitComposition(JNIEnv *env, jobject thiz,
                                                              jlong session_id) {
    auto it = sessions.find(session_id);
    if (it == sessions.end()) return env->NewStringUTF("");

    std::string committed = it->second.composing;
    it->second.composing.clear();
    it->second.candidates.clear();
    return env->NewStringUTF(committed.c_str());
}

JNIEXPORT void JNICALL
Java_com_aegisinput_engine_RimeBridge_nativeClearComposition(JNIEnv *env, jobject thiz,
                                                             jlong session_id) {
    auto it = sessions.find(session_id);
    if (it != sessions.end()) {
        it->second.composing.clear();
        it->second.candidates.clear();
    }
}

} // extern "C"
